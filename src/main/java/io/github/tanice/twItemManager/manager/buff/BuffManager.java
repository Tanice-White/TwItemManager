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
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static io.github.tanice.twItemManager.infrastructure.PDCAPI.*;
import static io.github.tanice.twItemManager.util.Logger.logInfo;
import static io.github.tanice.twItemManager.util.Logger.logWarning;

public class BuffManager {
    private final JavaPlugin plugin;
    /** 全局可用Buff*/
    private final Map<String, BuffPDC> buffMap;

    /** 全局Buff线程池 */
    private final ScheduledExecutorService timerBuffExecutor;
    /** 跟踪正在运行的Timer Buff任务 */
    private final ConcurrentHashMap<UUID, Map<String, ScheduledFuture<?>>> activeTimerBuffs;


    public BuffManager(@NotNull JavaPlugin plugin) {
        this.plugin = plugin;
        buffMap = new HashMap<>();
        activeTimerBuffs = new ConcurrentHashMap<>();
        this.loadBuffFilesAndBuffMap();

        // 创建线程池 - 根据服务器情况调整核心线程数
        int corePoolSize = Math.max(2, Runtime.getRuntime().availableProcessors() / 2);
        timerBuffExecutor = new ScheduledThreadPoolExecutor(
                corePoolSize,
                new ThreadFactory() {
                    private final AtomicInteger threadNumber = new AtomicInteger(1);
                    @Override
                    public Thread newThread(@NotNull Runnable r) {
                        Thread t = new Thread(r, "BuffTimerThread-" + threadNumber.getAndIncrement());
                        t.setDaemon(true);
                        t.setPriority(Thread.NORM_PRIORITY);
                        return t;
                    }
                },
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
        ((ScheduledThreadPoolExecutor) timerBuffExecutor).setRemoveOnCancelPolicy(true);
        Bukkit.getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void onPluginDisable(PluginDisableEvent event) {
                if (event.getPlugin() == plugin) {
                    shutdownExecutor();
                }
            }
        }, plugin);
    }

    public void onReload() {
        buffMap.clear();
        this.loadBuffFilesAndBuffMap();
    }

    public @Nullable BuffPDC getBuffPDC(@Nullable String bufInnerName) {
        return buffMap.get(bufInnerName);
    }

    /**
     * 遍历所有 hold_buff 并更新
     */
    public void updateHoldBuffs(@NotNull LivingEntity e) {
        /* 清空buff */
        /* TODO 不能全部清空！ */
        EntityPDC ePDC = getEntityCalculablePDC(e);
        if (ePDC == null) ePDC = new EntityPDC();
        ePDC.removeAllBuffs();
        setEntityCalculablePDC(e, ePDC);

        EntityEquipment equip = e.getEquipment();
        if (equip == null) return;

        ItemStack it;
        Item i;
        it = equip.getItemInMainHand();
        if (SlotUtil.mainHandJudge(getSlot(it))) {
            i = TwItemManager.getItemManager().getItemByItemStack(it);
            if (i != null) activeBuff(e, i.getHoldBuffs());
        }

        it = equip.getItemInOffHand();
        if (SlotUtil.offHandJudge(getSlot(it))) {
            i = TwItemManager.getItemManager().getItemByItemStack(it);
            if (i != null) activeBuff(e, i.getHoldBuffs());
        }

        it = equip.getHelmet();
        if (it != null) {
            i = TwItemManager.getItemManager().getItemByItemStack(it);
            if (i != null) activeBuff(e, i.getHoldBuffs());
        }

        it = equip.getChestplate();
        if (it != null) {
            i = TwItemManager.getItemManager().getItemByItemStack(it);
            if (i != null) activeBuff(e, i.getHoldBuffs());
        }

        it = equip.getLeggings();
        if (it != null) {
            i = TwItemManager.getItemManager().getItemByItemStack(it);
            if (i != null) activeBuff(e, i.getHoldBuffs());
        }

        it = equip.getBoots();
        if (it != null) {
            i = TwItemManager.getItemManager().getItemByItemStack(it);
            if (i != null) activeBuff(e, i.getHoldBuffs());
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
            if (i != null) activeBuff(b, i.getAttackBuffs());
        }

        it = equip.getItemInOffHand();
        if (SlotUtil.offHandJudge(getSlot(it))) {
            i = TwItemManager.getItemManager().getItemByItemStack(it);
            if (i != null) activeBuff(b, i.getAttackBuffs());
        }

        it = equip.getHelmet();
        if (it != null) {
            i = TwItemManager.getItemManager().getItemByItemStack(it);
            if (i != null) activeBuff(b, i.getAttackBuffs());
        }

        it = equip.getChestplate();
        if (it != null) {
            i = TwItemManager.getItemManager().getItemByItemStack(it);
            if (i != null) activeBuff(b, i.getAttackBuffs());
        }

        it = equip.getLeggings();
        if (it != null) {
            i = TwItemManager.getItemManager().getItemByItemStack(it);
            if (i != null) activeBuff(b, i.getAttackBuffs());
        }

        it = equip.getBoots();
        if (it != null) {
            i = TwItemManager.getItemManager().getItemByItemStack(it);
            if (i != null) activeBuff(b, i.getAttackBuffs());
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
            if (i != null) activeBuff(a, i.getDefenseBuffs());
        }

        it = equip.getItemInOffHand();
        if (SlotUtil.offHandJudge(getSlot(it))) {
            i = TwItemManager.getItemManager().getItemByItemStack(it);
            if (i != null) activeBuff(a, i.getDefenseBuffs());
        }

        it = equip.getHelmet();
        if (it != null) {
            i = TwItemManager.getItemManager().getItemByItemStack(it);
            if (i != null) activeBuff(a, i.getDefenseBuffs());
        }

        it = equip.getChestplate();
        if (it != null) {
            i = TwItemManager.getItemManager().getItemByItemStack(it);
            if (i != null) activeBuff(a, i.getDefenseBuffs());
        }

        it = equip.getLeggings();
        if (it != null) {
            i = TwItemManager.getItemManager().getItemByItemStack(it);
            if (i != null) activeBuff(a, i.getDefenseBuffs());
        }

        it = equip.getBoots();
        if (it != null) {
            i = TwItemManager.getItemManager().getItemByItemStack(it);
            if (i != null) activeBuff(a, i.getDefenseBuffs());
        }
    }

    /**
     * 让 buff 生效
     */
    public void activeBuff(@NotNull LivingEntity e, @NotNull List<String> buffNames) {
        BuffPDC bPDC;
        EntityPDC ePDC = getEntityCalculablePDC(e);
        if (ePDC == null) ePDC = new EntityPDC();

        for (String bn : buffNames) {
            bPDC = getBuffPDC(bn);
            if (bPDC == null) continue;
            /* 全局计算类属性 */
            if (bPDC.getAttributeCalculateSection() == AttributeCalculateSection.TIMER) {
                startTimerBuffTask(e, bPDC);
                logWarning("执行js任务");
            /* 非Timer类的都需要计算生效时间 */
            } else {
                /* 持续时间为负数则永续 */
                if (bPDC.getDuration() > 0) bPDC.setEndTimeStamp(System.currentTimeMillis() + (long) 50 * bPDC.getDuration());
                ePDC.addBuff(bPDC);
            }
        }
        setEntityCalculablePDC(e, ePDC);

        if (Config.debug) {
            logInfo("[activeBuff]: " + ePDC);
        }
    }

    /**
     * buff失效
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
     * 玩家退出时清理所有Timer Buff任务
     */
    public void onPlayerQuit(UUID playerId) {
        Map<String, ScheduledFuture<?>> entityTasks = activeTimerBuffs.remove(playerId);
        if (entityTasks != null) {
            for (ScheduledFuture<?> future : entityTasks.values()) {
                future.cancel(false);
            }
            entityTasks.clear();
        }
    }

    /**
     * 关闭线程池
     */
    public void shutdownExecutor() {
        timerBuffExecutor.shutdown();
        try {
            if (!timerBuffExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                timerBuffExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            timerBuffExecutor.shutdownNow();
        }
        activeTimerBuffs.clear();
    }

    /**
     * 启动Timer类型的Buff任务
     */
    private void startTimerBuffTask(@NotNull LivingEntity entity, @NotNull BuffPDC buff) {
        UUID entityId = entity.getUniqueId();
        String buffName = buff.getInnerName();

        // 检查是否已有相同的Timer Buff正在运行
        Map<String, ScheduledFuture<?>> entityTasks = activeTimerBuffs.computeIfAbsent(
                entityId,
                k -> new ConcurrentHashMap<>()
        );

        if (entityTasks.containsKey(buffName)) {
            return; // 已有任务在运行，跳过
        }

        long period = buff.getDuration() > 0 ? buff.getDuration() : 20; // 默认20tick(1秒)
        ScheduledFuture<?> future = timerBuffExecutor.scheduleAtFixedRate(() -> {
            try {
                if (entity.isValid() && !entity.isDead()) {
                    // 执行Buff效果
                    buff.run(entity);
                    if (buff.getDuration() > 0 && System.currentTimeMillis() >= buff.getEndTimeStamp())
                        cancelTimerBuffTask(entity, buffName);
                } else cancelTimerBuffTask(entity, buffName);

            } catch (Exception ex) {
                logWarning("执行Timer Buff任务时出错: " + ex.getMessage());
            }
        }, 0, period, TimeUnit.MILLISECONDS);

        entityTasks.put(buffName, future);
    }

    /**
     * 取消Timer类型的Buff任务
     */
    private void cancelTimerBuffTask(@NotNull LivingEntity entity, String buffName) {
        UUID entityId = entity.getUniqueId();
        Map<String, ScheduledFuture<?>> entityTasks = activeTimerBuffs.get(entityId);

        if (entityTasks != null) {
            ScheduledFuture<?> future = entityTasks.remove(buffName);
            if (future != null) {
                future.cancel(false);
            }

            // 如果没有任务了，移除实体的任务映射
            if (entityTasks.isEmpty()) {
                activeTimerBuffs.remove(entityId);
            }
        }
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
}
