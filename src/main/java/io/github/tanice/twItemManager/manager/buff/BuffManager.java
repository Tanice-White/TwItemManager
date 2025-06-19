package io.github.tanice.twItemManager.manager.buff;

import io.github.tanice.twItemManager.TwItemManager;
import io.github.tanice.twItemManager.config.Config;
import io.github.tanice.twItemManager.manager.item.base.impl.Item;
import io.github.tanice.twItemManager.manager.pdc.impl.BuffPDC;
import io.github.tanice.twItemManager.manager.pdc.impl.EntityPDC;
import io.github.tanice.twItemManager.manager.pdc.type.AttributeCalculateSection;
import io.github.tanice.twItemManager.util.SlotUtil;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static io.github.tanice.twItemManager.infrastructure.PDCAPI.*;
import static io.github.tanice.twItemManager.util.Logger.logInfo;
import static io.github.tanice.twItemManager.util.Logger.logWarning;

public class BuffManager {
    private static final int RUN_CD = 2;
    private final JavaPlugin plugin;
    /** 全局可用Buff*/
    private final Map<String, BuffPDC> buffMap;

    /** buff记录 */
    private final BuffRecords buffRecords;
    /** 任务处理 */
    private BukkitTask buffTask;
    /** 实体状态缓存 */
    private final Map<UUID, CachedEntity> entityCache;

    private static BuffManager instance;

    public BuffManager(@NotNull JavaPlugin plugin) {
        instance = this;
        this.plugin = plugin;
        buffMap = new HashMap<>();
        this.loadBuffFilesAndBuffMap();

        buffRecords = new BuffRecords();
        entityCache = new ConcurrentHashMap<>();
        // 启动时间轮处理任务 (每5tick执行一次)
        buffTask = Bukkit.getScheduler().runTaskTimer(plugin, this::processBuffs, 10L, RUN_CD);
        logInfo("BuffManager loaded, Scheduler started");
    }

    public void onReload() {
        buffMap.clear();
        this.loadBuffFilesAndBuffMap();

        // 重新启动时间轮任务
        if (buffTask != null) buffTask.cancel();
        buffTask = Bukkit.getScheduler().runTaskTimer(plugin, this::processBuffs, 10L, RUN_CD);
    }

    public void onDisable() {
        buffMap.clear();

        // 停止时间轮任务
        if (buffTask != null) {
            buffTask.cancel();
            buffTask = null;
        }
        // 清空时间轮和缓存
        buffRecords.records.clear();
        entityCache.clear();
    }

    public @Nullable BuffPDC getBuffPDC(@Nullable String bufInnerName) {
        return buffMap.get(bufInnerName);
    }

    /**
     * 玩家退出时清理所有Timer Buff任务
     */
    public void onPlayerQuit(@NotNull UUID playerId) {
        buffRecords.removeAllRecordsForEntity(playerId);
        entityCache.remove(playerId);
    }

    /**
     * 遍历所有 hold_buff 并更新
     */
    public void updateHoldBuffs(@NotNull LivingEntity e, @Nullable ItemStack pre) {
        /* 清空 buff */
        EntityPDC ePDC = getEntityCalculablePDC(e);
        if (ePDC == null) ePDC = new EntityPDC();
        ePDC.removeHoldBuffs();  // else ePDC.removeBuff(bn); 一样
        /* TIMER 类永续buff移除 */
        Item preI = TwItemManager.getItemManager().getItemByItemStack(pre);
        if (preI != null) {
            BuffPDC bPDC;
            for (String bn : preI.getHoldBuffs()) {
                bPDC = getBuffPDC(bn);
                if (bPDC == null) continue;
                if (bPDC.getAttributeCalculateSection() == AttributeCalculateSection.TIMER) {
                    cancelTimerBuffTask(e, bn);
                }
                // else ePDC.removeBuff(bn);
            }
        }

        setEntityCalculablePDC(e, ePDC);

        EntityEquipment equip = e.getEquipment();
        if (equip == null) return;

        ItemStack it;
        Item i;
        it = equip.getItemInMainHand();
        if (SlotUtil.mainHandJudge(getSlot(it))) {
            i = TwItemManager.getItemManager().getItemByItemStack(it);
            if (i != null) activeBuff(e, i.getHoldBuffs(), true);
        }

        it = equip.getItemInOffHand();
        if (SlotUtil.offHandJudge(getSlot(it))) {
            i = TwItemManager.getItemManager().getItemByItemStack(it);
            if (i != null) activeBuff(e, i.getHoldBuffs(), true);
        }

        it = equip.getHelmet();
        if (it != null) {
            i = TwItemManager.getItemManager().getItemByItemStack(it);
            if (i != null) activeBuff(e, i.getHoldBuffs(), true);
        }

        it = equip.getChestplate();
        if (it != null) {
            i = TwItemManager.getItemManager().getItemByItemStack(it);
            if (i != null) activeBuff(e, i.getHoldBuffs(), true);
        }

        it = equip.getLeggings();
        if (it != null) {
            i = TwItemManager.getItemManager().getItemByItemStack(it);
            if (i != null) activeBuff(e, i.getHoldBuffs(), true);
        }

        it = equip.getBoots();
        if (it != null) {
            i = TwItemManager.getItemManager().getItemByItemStack(it);
            if (i != null) activeBuff(e, i.getHoldBuffs(), true);
        }
    }

    /**
     * 攻击时上的 buff
     * @param a 攻击方
     * @param b 被攻击方
     */
    public void doAttackBuffs(@NotNull LivingEntity a, @NotNull LivingEntity b) {
        EntityEquipment equip = a.getEquipment();
        if (equip == null) return;

        ItemStack it;
        Item i;
        it = equip.getItemInMainHand();
        if (SlotUtil.mainHandJudge(getSlot(it))) {
            i = TwItemManager.getItemManager().getItemByItemStack(it);
            if (i != null) activeBuff(b, i.getAttackBuffs(), false);
        }

        it = equip.getItemInOffHand();
        if (SlotUtil.offHandJudge(getSlot(it))) {
            i = TwItemManager.getItemManager().getItemByItemStack(it);
            if (i != null) activeBuff(b, i.getAttackBuffs(), false);
        }

        it = equip.getHelmet();
        if (it != null) {
            i = TwItemManager.getItemManager().getItemByItemStack(it);
            if (i != null) activeBuff(b, i.getAttackBuffs(), false);
        }

        it = equip.getChestplate();
        if (it != null) {
            i = TwItemManager.getItemManager().getItemByItemStack(it);
            if (i != null) activeBuff(b, i.getAttackBuffs(), false);
        }

        it = equip.getLeggings();
        if (it != null) {
            i = TwItemManager.getItemManager().getItemByItemStack(it);
            if (i != null) activeBuff(b, i.getAttackBuffs(), false);
        }

        it = equip.getBoots();
        if (it != null) {
            i = TwItemManager.getItemManager().getItemByItemStack(it);
            if (i != null) activeBuff(b, i.getAttackBuffs(), false);
        }
    }

    /**
     * 被攻击时上的 buff
     * @param a 攻击方
     * @param b 被攻击方
     */
    public void doDefenceBuffs(@NotNull LivingEntity a, @NotNull LivingEntity b) {
        EntityEquipment equip = b.getEquipment();
        if (equip == null) return;

        ItemStack it;
        Item i;
        it = equip.getItemInMainHand();
        if (SlotUtil.mainHandJudge(getSlot(it))) {
            i = TwItemManager.getItemManager().getItemByItemStack(it);
            if (i != null) activeBuff(a, i.getDefenseBuffs(), false);
        }

        it = equip.getItemInOffHand();
        if (SlotUtil.offHandJudge(getSlot(it))) {
            i = TwItemManager.getItemManager().getItemByItemStack(it);
            if (i != null) activeBuff(a, i.getDefenseBuffs(), false);
        }

        it = equip.getHelmet();
        if (it != null) {
            i = TwItemManager.getItemManager().getItemByItemStack(it);
            if (i != null) activeBuff(a, i.getDefenseBuffs(), false);
        }

        it = equip.getChestplate();
        if (it != null) {
            i = TwItemManager.getItemManager().getItemByItemStack(it);
            if (i != null) activeBuff(a, i.getDefenseBuffs(), false);
        }

        it = equip.getLeggings();
        if (it != null) {
            i = TwItemManager.getItemManager().getItemByItemStack(it);
            if (i != null) activeBuff(a, i.getDefenseBuffs(), false);
        }

        it = equip.getBoots();
        if (it != null) {
            i = TwItemManager.getItemManager().getItemByItemStack(it);
            if (i != null) activeBuff(a, i.getDefenseBuffs(), false);
        }
    }

    /**
     * 让 buff 生效
     */
    public void activeBuff(@NotNull LivingEntity e, @NotNull List<String> buffNames, boolean isHoldBuff) {
        BuffPDC bPDC;
        EntityPDC ePDC = getEntityCalculablePDC(e);
        if (ePDC == null) ePDC = new EntityPDC();

        for (String bn : buffNames) {
            bPDC = getBuffPDC(bn);
            if (bPDC == null) {
                logWarning("buff 名: " + bn + "不存在");
                continue;
            }
            /* 全局计算类属性 */
            if (bPDC.getAttributeCalculateSection() == AttributeCalculateSection.TIMER) {
                /* 永续 */
                if (isHoldBuff) bPDC.setDuration(-1);
                startTimerBuffTask(e, bPDC);
            /* 非Timer类的都需要计算生效时间 */
            } else {
                /* 持续时间为负数则永续 */
                if (bPDC.getDuration() > 0) bPDC.setEndTimeStamp(System.currentTimeMillis() + (50L * bPDC.getDuration()));
                ePDC.addBuff(bPDC, isHoldBuff);
            }
        }
        setEntityCalculablePDC(e, ePDC);

        if (Config.debug) {
            logInfo("[activeBuff]: " + ePDC);
        }
    }

    /**
     * 属性 buff 失效
     */
    public void deactivateBuff(@NotNull LivingEntity e, @NotNull List<String> buffNames) {
        BuffPDC bPDC;
        EntityPDC ePDC = getEntityCalculablePDC(e);
        if (ePDC == null) ePDC = new EntityPDC();

        for (String bn : buffNames) {
            bPDC = getBuffPDC(bn);
            if (bPDC == null) continue;
            /* TIMER */
            if (bPDC.getAttributeCalculateSection() == AttributeCalculateSection.TIMER) {
                cancelTimerBuffTask(e, bPDC.getInnerName());
            }
            ePDC.removeBuff(bPDC);
        }
        setEntityCalculablePDC(e, ePDC);
    }

    /**
     * TIMER buff 失效
     */
    public void deactivatePlayerTimerBuff(@NotNull UUID playerId) {
        buffRecords.removeAllRecordsForEntity(playerId);
        if (!buffRecords.hasAnyBuff(playerId)) entityCache.remove(playerId);
    }

    private void processBuffs() {
        buffRecords.process();
    }

    /**
     * 启动Timer类型的Buff任务
     */
    private void startTimerBuffTask(@NotNull LivingEntity entity, @NotNull BuffPDC buff) {
        // 获取或创建缓存
        entityCache.computeIfAbsent(
                entity.getUniqueId(),
                k -> new CachedEntity(entity)
        );
        // 创建Buff记录
        buffRecords.addBuff(entity, buff);
    }

    /**
     * 取消Timer类型的Buff任务
     */
    private void cancelTimerBuffTask(@NotNull LivingEntity entity, String buffName) {
        UUID entityId = entity.getUniqueId();
        buffRecords.removeRecord(entityId, buffName);
        boolean hasOtherBuffs = buffRecords.hasAnyBuff(entityId);
        if (!hasOtherBuffs) entityCache.remove(entityId);
    }

    private void loadBuffFilesAndBuffMap(){
        AtomicInteger total = new AtomicInteger();
        Path buffDir = plugin.getDataFolder().toPath().resolve("buff"); ;
        if (!Files.exists(buffDir) || !Files.isDirectory(buffDir)) return;
        try (Stream<Path> files = Files.list(buffDir)) {
            files.forEach(file -> {
                String fileName = file.getFileName().toString();
                String name = fileName.substring(0, fileName.lastIndexOf('.'));
                BuffPDC bPDC;
                ConfigurationSection subsection;
                if (fileName.endsWith(".yml")) {
                    ConfigurationSection section = YamlConfiguration.loadConfiguration(file.toFile());
                    for (String k : section.getKeys(false)) {
                        subsection = section.getConfigurationSection(k);
                        if (subsection == null) continue;
                        bPDC = new BuffPDC(k, subsection);
                        buffMap.put(k, bPDC);
                        total.getAndIncrement();
                    }
                }
                else if (fileName.endsWith(".js")) {
                    bPDC = new BuffPDC(name, file);
                    buffMap.put(bPDC.getInnerName(), bPDC);
                    total.getAndIncrement();
                }
                else logWarning("未知的文件格式: " + fileName);
            });
            logInfo("[loadBuffs]: 共加载BUFF" + total.get() + "个");
        } catch (IOException e) {
            logWarning("加载BUFF文件错误: " + e);
        }
    }

    // 时间轮记录
    private static class BuffRecord {
        final UUID entityId;
        final BuffPDC buff;
        int cooldownCounter;
        int durationCounter;

        BuffRecord(@NotNull LivingEntity entity, @NotNull BuffPDC buff) {
            this.entityId = entity.getUniqueId();
            this.buff = buff;
            this.cooldownCounter = buff.getCd();
            this.durationCounter = buff.getDuration();
        }
    }

    // 实体状态缓存
    private static class CachedEntity {
        final UUID uuid;
        LivingEntity entity;

        CachedEntity(@NotNull LivingEntity entity) {
            this.uuid = entity.getUniqueId();
            this.entity = entity;
        }
    }

    // 时间轮实现
    private static class BuffRecords {
        final Map<String, BuffRecord> records = new ConcurrentHashMap<>(100);

        public void process() {
            Iterator<Map.Entry<String, BuffRecord>> it = records.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, BuffRecord> entry = it.next();
                BuffRecord record = entry.getValue();
                // 检查CD
                record.cooldownCounter -= RUN_CD;
                if (record.cooldownCounter <= 0) {
                    CachedEntity cached = BuffManager.instance.entityCache.get(record.entityId);
                    if (cached == null || cached.entity == null || cached.entity.isDead()) {
                        it.remove();
                        continue;
                    }
                    record.buff.execute(cached.entity);
                    record.cooldownCounter = Math.max(1, record.buff.getCd());
                    // 检查持续时间
                    /* 最开始就小于等于0则永续 */
                    if (record.durationCounter > 0) {
                        record.durationCounter -= RUN_CD;
                        if (record.durationCounter <= 0) it.remove();
                    }
                }
            }
        }

        // 移除指定记录
        public void removeRecord(@NotNull UUID entityId, String buffName) {
            String key = createKey(entityId, buffName);
            records.remove(key);
        }

        public void removeAllRecordsForEntity(@NotNull UUID entityId) {
            String prefix = entityId + "-";
            records.keySet().removeIf(key -> key.startsWith(prefix));
        }

        public boolean hasAnyBuff(@NotNull UUID entityId) {
            String prefix = entityId + "-";
            for (String key : records.keySet()) {
                if (key.startsWith(prefix)) return true;
            }
            return false;
        }

        public void addBuff(@NotNull LivingEntity entity, @NotNull BuffPDC buff) {
            String key = createKey(entity.getUniqueId(), buff.getInnerName());
            BuffRecord nbr = new BuffRecord(entity, buff);
            BuffRecord br = records.get(key);

            if (br != null) {
                br.durationCounter = Math.max(br.durationCounter, nbr.durationCounter);
                records.put(key, br);
            } else records.put(key, nbr);
        }

        // 创建唯一键
        @Contract(pure = true)
        private @NotNull String createKey(@NotNull UUID entityId, String buffName) {
            return entityId + "-" + buffName;
        }
    }
}
