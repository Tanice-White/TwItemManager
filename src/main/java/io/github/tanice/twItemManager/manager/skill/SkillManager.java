package io.github.tanice.twItemManager.manager.skill;

import io.github.tanice.twItemManager.calculator.Calculator;
import io.github.tanice.twItemManager.calculator.LivingEntityCombatPowerCalculator;
import io.github.tanice.twItemManager.config.Config;
import io.github.tanice.twItemManager.manager.AsyncDirtyUpdateManager;
import io.github.tanice.twItemManager.manager.item.base.impl.Item;
import io.github.tanice.twItemManager.util.EquipmentUtil;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import static io.github.tanice.twItemManager.constance.key.ConfigKey.TRIGGER;
import static io.github.tanice.twItemManager.util.Logger.logInfo;
import static io.github.tanice.twItemManager.util.Logger.logWarning;

/**
 * 管理技能和技能冷却
 * 监听式释放
 */
public class SkillManager extends AsyncDirtyUpdateManager {
    private final JavaPlugin plugin;
    /** 清理间隔时间(分钟) */
    private static final long CLEAN_UP_CD = 1;
    /** 未使用记录保留时间(分钟) */
    private static final long REMAIN_TIME = 5;

    /** 技能和对应的触发器 */
    private final ConcurrentMap<String, SkillMeta> skillMap;
    /** 玩家可释放的技能 */
    private final Map<UUID, EnumMap<Trigger, Set<SkillRowData>>> playerSkillMap;
    /** 玩家ID -> 技能对象 -> 下一次可释放的时间戳(毫秒) */
    private final ConcurrentMap<UUID, ConcurrentMap<String, Long>> playerSkillCooldowns;
    /** 定期清理线程 */
    private final ScheduledExecutorService asyncCleanupSchedulerExecutor;
    /** 线程运行标志 */
    private final AtomicBoolean isSchedulerRunning = new AtomicBoolean(false);

    public SkillManager(@NotNull JavaPlugin plugin) {
        super();
        this.plugin = plugin;
        skillMap = new ConcurrentHashMap<>();
        playerSkillMap = new ConcurrentHashMap<>();
        playerSkillCooldowns = new ConcurrentHashMap<>();

        asyncCleanupSchedulerExecutor = Executors.newSingleThreadScheduledExecutor();
        asyncCleanupSchedulerExecutor.scheduleAtFixedRate(
                this::asyncCleanupExpiredRecords,
                CLEAN_UP_CD,
                CLEAN_UP_CD,
                TimeUnit.MINUTES
        );
        this.loadResourceFiles();
    }

    public void onReload() {
        super.onReload();
        skillMap.clear();
        playerSkillMap.clear();
        playerSkillCooldowns.clear();
        this.loadResourceFiles();
        logInfo("SkillManager reloaded");
    }

    public void OnDisable() {
        super.onDisable();
        skillMap.clear();
        playerSkillMap.clear();
        playerSkillCooldowns.clear();
        asyncCleanupSchedulerExecutor.shutdownNow();
    }

    /**
     * 释放技能
     * @return 是否释放成功
     */
    public boolean casteSkill(@NotNull Player player, @NotNull String skillName) {
        return true;
    }

    /**
     * 手动设置技能冷却
     */
    public void setSkillCooldown(@NotNull Player player, @NotNull SkillRowData skill, int cooldownTicks) {
        playerSkillCooldowns.compute(player.getUniqueId(), (uid, skillMap) -> {
            if (skillMap == null) skillMap = new ConcurrentHashMap<>();
            long nextAvailableTime = System.currentTimeMillis() + cooldownTicks * 50L;
            skillMap.put(skill.getSkillName(), nextAvailableTime);
            return skillMap;
        });
    }

    /**
     * 检查技能是否就绪 (只读)
     */
    public boolean isSkillCooldownReady(@NotNull Player player, @NotNull String skillName, long currentTime) {
        ConcurrentMap<String, Long> skillMap = playerSkillCooldowns.get(player.getUniqueId());
        if (skillMap == null) return true;

        Long nextAvailableTime = skillMap.get(skillName);
        if (nextAvailableTime == null) return true;

        return currentTime > nextAvailableTime;
    }

    /**
     * 获取技能剩余冷却时间 (只读)
     */
    public long getSkillRemainingCooldown(@NotNull Player player, @NotNull String skillName, long currentTime) {
        ConcurrentMap<String, Long> skillMap = playerSkillCooldowns.get(player.getUniqueId());
        if (skillMap == null) return 0;

        Long nextAvailableTime = skillMap.get(skillName);
        if (nextAvailableTime == null) return 0;

        if (currentTime >= nextAvailableTime) return 0;
        return nextAvailableTime - currentTime;
    }

    /**
     * 清除玩家所有技能数据（玩家下线时调用）
     */
    public void clearPlayerData(@NotNull UUID uuid) {
        playerSkillCooldowns.remove(uuid);
    }

    public void addPlayerData(@NotNull UUID uuid) {
        playerSkillCooldowns.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>());
    }

    /**
     * 提交玩家技能更新
     */
    public void submitAsyncTask(@NotNull Player player) {
        UUID uuid = player.getUniqueId();
        /* 更新最新实体引用 */
        pendingUpdates.compute(uuid, (k, v) -> {
            if (v == null) v = new AtomicReference<>(player);
            else v.set(player);
            return v;
        });
        dirtyFlags.computeIfAbsent(uuid, k -> new AtomicBoolean(false));

        AtomicBoolean computing = computingFlags.computeIfAbsent(uuid, k -> new AtomicBoolean(false));
        if (computing.compareAndSet(false, true)) {
            asyncExecutor.submit(() -> asyncRun(player));
            if (Config.debug) logInfo("[SkillManager] 实体" + player.getName() + " 可释放技能更新");

            /* 计算中，标记为脏 */
        } else dirtyFlags.get(uuid).set(true);
    }

    /**
     * 更新玩家可用技能  与玩家属性同步更新(相对滞后)
     */
    private void asyncRun(@NotNull Player player) {
        UUID uuid = player.getUniqueId();
        try {
            /* 计算前清除脏标记 */
            dirtyFlags.get(uuid).set(false);

            AtomicReference<LivingEntity> entityRef = pendingUpdates.get(uuid);
            if (entityRef == null) {
                playerSkillMap.remove(uuid);
                return;
            }

            LivingEntity latestEntity = entityRef.get();
            if (latestEntity == null || !latestEntity.isValid() || latestEntity.isDead()) {
                playerSkillMap.remove(uuid);
                return;
            }

            EnumMap<Trigger, Set<SkillRowData>> tsm = playerSkillMap.computeIfAbsent(
                    uuid, k -> new EnumMap<>(Trigger.class)
            );
            /* 防止旧数据残留 */
            tsm.clear();

            SkillRowData skillRowData;
            Trigger trigger;
            for (Item item : EquipmentUtil.getActiveEquipmentItem(player)) {
                for (String skillName : item.getSkills()) {
                    SkillMeta skillMeta = skillMap.get(skillName);
                    if (skillMeta == null) continue;

                    skillRowData = skillMeta.skillRowData;
                    trigger = skillMeta.trigger;
                    tsm.computeIfAbsent(trigger, t -> ConcurrentHashMap.newKeySet()).add(skillRowData);
                }
            }
            if (Config.debug) {
                Trigger triggerType;
                Set<SkillRowData> skillSet;
                for (Map.Entry<Trigger, Set<SkillRowData>> entry : tsm.entrySet()) {
                    triggerType = entry.getKey();
                    skillSet = entry.getValue();
                    logInfo("  触发类型: " + triggerType.name());
                    for (SkillRowData skillData : skillSet) {
                        logInfo("\t" + skillData.getSkillName() + "(" + skillData.getMythicSkillName() + ") ");
                    }
                }
            }

        } finally {
            computingFlags.get(uuid).set(false);
            /* 如果期间有新请求，继续处理 */
            if (dirtyFlags.get(uuid).getAndSet(false)) {
                if (computingFlags.get(uuid).compareAndSet(false, true)) {
                    asyncExecutor.submit(() -> asyncRun(player));
                }
            }
        }
    }

    /**
     * 定期清理过期记录
     */
    private void asyncCleanupExpiredRecords() {
        if (!isSchedulerRunning.get()) {
            logWarning("SKillManager 中技能回收线程运行时间过长");
            return;
        }
        isSchedulerRunning.set(true);
        long expiredThreshold = System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(REMAIN_TIME);

        // 遍历所有玩家
        playerSkillCooldowns.forEach((uuid, skillMap) -> {
            skillMap.entrySet().removeIf(entry -> {
                long nextAvailableTime = entry.getValue();
                return nextAvailableTime <= expiredThreshold;
            });
            /* 玩家没有技能条目则删除 */
            if (skillMap.isEmpty()) playerSkillCooldowns.remove(uuid, skillMap);
        });
        isSchedulerRunning.set(false);
    }

    private void loadResourceFiles(){
        AtomicInteger total = new AtomicInteger();
        Path buffDir = plugin.getDataFolder().toPath().resolve("skills");
        if (!Files.exists(buffDir) || !Files.isDirectory(buffDir)) return;
        try (Stream<Path> files = Files.list(buffDir)) {
            files.forEach(file -> {
                String fileName = file.getFileName().toString();
                ConfigurationSection subsection;
                if (fileName.endsWith(".yml")) {
                    ConfigurationSection section = YamlConfiguration.loadConfiguration(file.toFile());
                    for (String k : section.getKeys(false)) {
                        subsection = section.getConfigurationSection(k);
                        if (subsection == null) continue;
                        final ConfigurationSection cfg = subsection;
                        skillMap.computeIfAbsent(k,
                                s -> new SkillMeta(new SkillRowData(k, cfg), Trigger.valueOf(cfg.getString(TRIGGER))));
                        total.getAndIncrement();
                    }
                }
                else logWarning("未知的文件格式: " + fileName);
            });
            logInfo("[loadSkills]: 共加载 技能" + total.get() + "个");
        } catch (IOException e) {
            logWarning("加载Skills文件错误: " + e);
        }
    }

    private static class SkillMeta {
        SkillRowData skillRowData;
        Trigger trigger;

        SkillMeta(@NotNull SkillRowData skillRowData, @NotNull Trigger trigger) {
            this.skillRowData = skillRowData;
            this.trigger = trigger;
        }
    }
}
