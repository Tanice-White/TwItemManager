package io.github.tanice.twItemManager.manager.buff;

import io.github.tanice.twItemManager.TwItemManager;
import io.github.tanice.twItemManager.config.Config;
import io.github.tanice.twItemManager.event.EntityAttributeChangeEvent;
import io.github.tanice.twItemManager.pdc.CalculablePDC;
import io.github.tanice.twItemManager.pdc.impl.BuffPDC;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static io.github.tanice.twItemManager.util.Logger.logInfo;
import static io.github.tanice.twItemManager.util.Logger.logWarning;

public class BuffManager {
    private static final int BUFF_RUN_CD = 2;
    private final JavaPlugin plugin;
    private final Random random;

    /** 全局可用Buff */
    private final Map<String, BuffPDC> buffMap;

    /** 实体状态缓存 */
    private final ConcurrentMap<UUID, CachedEntity> entityCache;
    /** 实体的 Buff 列表(实体id, (buff内部名, buff记录)) */
    private final ConcurrentMap<UUID, ConcurrentMap<String, BuffRecord>> entityBuffMap;
    /** 线程池 - 处理buff */
    private ExecutorService executor;
    /** 主线程任务调度器 */
    private BukkitTask syncBuffExecutor;
    /** 需要主线程执行的 buff 队列 */
    private final LinkedBlockingQueue<BuffRecord> buffTaskQueue;
    private final LinkedBlockingQueue<LivingEntity> eventQueue;

    public BuffManager(@NotNull JavaPlugin plugin) {
        this.plugin = plugin;
        this.buffMap = new ConcurrentHashMap<>();
        this.entityCache = new ConcurrentHashMap<>();
        this.entityBuffMap = new ConcurrentHashMap<>();
        this.buffTaskQueue = new LinkedBlockingQueue<>();
        this.eventQueue = new LinkedBlockingQueue<>();
        this.random = new Random();
        this.initAsyncExecutor();
        this.initSyncBuffExecutor();
        this.loadBuffMap();
        logInfo("BuffManager loaded, Scheduler started");
    }

    public void onReload() {
        this.onDisable();
        this.initAsyncExecutor();
        this.initSyncBuffExecutor();
        this.loadBuffMap();
        logInfo("BuffManager reload");
    }

    public void onDisable() {
        this.saveAllPlayerRecords();

        if (syncBuffExecutor != null && !syncBuffExecutor.isCancelled()) syncBuffExecutor.cancel();
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
            }
        }

        buffMap.clear();
        entityCache.clear();
        entityBuffMap.clear();
        buffTaskQueue.clear();
        eventQueue.clear();
    }

    /**
     * 获取 buffPDC 的可变副本
     */
    public @Nullable BuffPDC getBuffPDC(@Nullable String bufInnerName) {
        BuffPDC buffPDC = buffMap.get(bufInnerName);
        if (buffPDC == null) return null;
        return buffPDC.clone();
    }

    /**
     * 需要延迟添加
     * 玩家登录后从数据库初始化对应的信息
     */
    public void loadPlayerBuffs(@NotNull Player player) {
        if (!Config.use_mysql) return;
        TwItemManager.getDatabaseManager().loadPlayerBuffRecords(player.getUniqueId().toString())
                .thenAccept(res -> {
                    if (res.isEmpty()) return;
                    ConcurrentMap<String, BuffRecord> innerMap;
                    for (BuffRecord r : res) {
                        innerMap = entityBuffMap.computeIfAbsent(r.getUuid(), k -> new ConcurrentHashMap<>());
                        innerMap.put(r.getBuffInnerName(), r);
                    }
                    if (Config.debug) {
                        StringBuilder s = new StringBuilder("玩家 " + player.getName() + "加入，同步buff: ");
                        for (BuffRecord r : res) s.append(r.toString()).append(" ");
                        logInfo(s.toString());
                    }
                });
        this.syncCallAttributeChangeEvent(player);
    }

    /**
     * 获取实体的有效 buff
     */
    public @NotNull List<CalculablePDC> getEntityActiveBuffs(@NotNull LivingEntity entity) {
        UUID uuid = entity.getUniqueId();
        if (!entityCache.containsKey(uuid)) return List.of();

        ConcurrentMap<String, BuffRecord> records = entityBuffMap.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>());
        if (records.isEmpty()) return List.of();

        List<CalculablePDC> res = new ArrayList<>(records.size() * 2);
        for (BuffRecord r : records.values()) {
            if (r.isTimer()) continue;
            res.add(r.getBuff());
        }
        return res;
    }

    /**
     * 玩家退出时清理所有Timer Buff任务
     */
    public void SaveAndClearPlayerBuffs(@NotNull Player player) {
        if (Config.use_mysql) {
            ConcurrentMap<String, BuffRecord> res = entityBuffMap.get(player.getUniqueId());
            if (res == null) return;
            TwItemManager.getDatabaseManager().saveBuffRecords(res.values());

            if (Config.debug) {
                StringBuilder s = new StringBuilder("玩家 " + player.getName() + " 退出，储存buff: ");
                for (BuffRecord r : res.values()) s.append(r.toString()).append(" ");
                logInfo(s.toString());
            }
        }
        entityBuffMap.remove(player.getUniqueId());
        entityCache.remove(player.getUniqueId());
    }

    /**
     * 储存所有的玩家记录
     * 非玩家的记录不保存
     */
    public void saveAllPlayerRecords() {
        for (Map.Entry<UUID, ConcurrentMap<String, BuffRecord>> entry : entityBuffMap.entrySet()) {
            if (entityCache.get(entry.getKey()) instanceof Player)
                TwItemManager.getDatabaseManager().saveBuffRecords(entry.getValue().values());
        }
    }

    /**
     * 为实体增加 buff
     * 完成后请触发 EntityAttributeChangeEvent 事件
     * @return  buff 是否被触发
     */
    public boolean activateBuff(@NotNull LivingEntity entity, @NotNull BuffPDC buff) {
        return activateBuff(entity,buff, false);
    }

    /**
     * 为实体增加 buff
     * 完成后请触发 EntityAttributeChangeEvent 事件
     */
    public boolean activateBuff(@NotNull LivingEntity entity, @NotNull BuffPDC buff, boolean isPermanent) {
        if (random.nextDouble() > buff.getChance()) return false;
        UUID playerId = entity.getUniqueId();
        entityCache.computeIfAbsent(playerId, id -> new CachedEntity(entity));
        ConcurrentMap<String, BuffRecord> innerMap = entityBuffMap.computeIfAbsent(playerId, id -> new ConcurrentHashMap<>());

        /* 创建或更新 Buff 记录 */
        innerMap.compute(buff.getInnerName(), (name, existingRecord) -> {
            if (existingRecord != null) {
                existingRecord.setPermanent(isPermanent);
                existingRecord.merge(buff);
                return existingRecord;

            } else return new BuffRecord(entity, buff, isPermanent);
        });
        return true;
    }

    /**
     * 为实体激活一组非永久的 buff
     * 完成后请触发 EntityAttributeChangeEvent 事件
     * @return 是否有 buff 被触发
     */
    public boolean activateBuffs(@NotNull LivingEntity entity, @NotNull Collection<BuffPDC> buffs) {
        return activateBuffs(entity, buffs, false);
    }

    /**
     * 为实体激活一组 buff
     * 完成后请触发 EntityAttributeChangeEvent 事件
     */
    public boolean activateBuffs(@NotNull LivingEntity entity, @NotNull Collection<BuffPDC> buffs, boolean isPermanent) {
        UUID playerId;
        ConcurrentMap<String, BuffRecord> innerMap;

        boolean f = isPermanent;

        for (BuffPDC bPDC : buffs) {
            if (!isPermanent && random.nextDouble() > bPDC.getChance()) continue;
            playerId = entity.getUniqueId();
            entityCache.computeIfAbsent(playerId, id -> new CachedEntity(entity));
            innerMap = entityBuffMap.computeIfAbsent(playerId, id -> new ConcurrentHashMap<>());

            /* 创建或更新 Buff 记录 */
            innerMap.compute(bPDC.getInnerName(), (name, existingRecord) -> {
                if (existingRecord != null) {
                    existingRecord.setPermanent(isPermanent);
                    existingRecord.merge(bPDC);
                    return existingRecord;

                } else return new BuffRecord(entity, bPDC, isPermanent);
            });
            f = true;
        }
        return f;
    }

    /**
     * 批量清除实体的 buff 效果
     * 完成后请触发 EntityAttributeChangeEvent 事件
     */
    public void deactivateBuffs(@NotNull LivingEntity entity, @NotNull Collection<BuffPDC> buffs) {
        UUID uuid = entity.getUniqueId();

        CachedEntity e = entityCache.get(entity.getUniqueId());
        if (e == null) return;
        ConcurrentMap<String, BuffRecord> innerMap = entityBuffMap.get(uuid);
        if (innerMap == null || innerMap.isEmpty()) return;

        for (BuffPDC bPDC : buffs) innerMap.remove(bPDC.getInnerName());

        if (innerMap.isEmpty()) {
            entityBuffMap.remove(uuid);
            entityCache.remove(uuid);
        }
    }

    /**
     * 去除实体的所有 buff
     * 完成后请触发 EntityAttributeChangeEvent 事件
     */
    public void deactivateEntityBuffs(@NotNull LivingEntity entity) {
        UUID uuid = entity.getUniqueId();

        CachedEntity cashed = entityCache.get(entity.getUniqueId());
        if (cashed == null) return;

        entityBuffMap.remove(uuid);
        entityCache.remove(uuid);
    }

    /**
     * 获取线程池状态信息
     */
    public String getThreadPoolStatus() {
        ThreadPoolExecutor tpe = (ThreadPoolExecutor) executor;
        return String.format("活跃线程: %d, 核心线程: %d, 最大线程: %d, 队列大小: %d",
                tpe.getActiveCount(), tpe.getCorePoolSize(), tpe.getMaximumPoolSize(), tpe.getQueue().size());
    }

    /**
     * 唤起实体属性改变事件
     */
    private void syncCallAttributeChangeEvent(@NotNull LivingEntity entity) {
        Bukkit.getPluginManager().callEvent(new EntityAttributeChangeEvent(entity));
    }

    /**
     * 处理 buff
     */
    private void asyncProcessEntityBuffs() {
        Map.Entry<UUID, ConcurrentMap<String, BuffRecord>> buffMapEntry;
        UUID uid;
        ConcurrentMap<String, BuffRecord> innerMap;
        BuffRecord record;
        boolean changed;
        for (Iterator<Map.Entry<UUID, ConcurrentMap<String, BuffRecord>>> it = entityBuffMap.entrySet().iterator(); it.hasNext(); ) {
            /* 初始化 */
            buffMapEntry = it.next();
            uid = buffMapEntry.getKey();
            innerMap = buffMapEntry.getValue();
            changed = false;

            /* 检查实体有效性 */
            CachedEntity cached = entityCache.get(uid);
            if (cached == null || cached.entity == null || !cached.entity.isValid() || cached.entity.isDead()) {
                entityCache.remove(uid);
                it.remove();
                continue;
            }

            if (innerMap == null || innerMap.isEmpty()) continue;
            for (Iterator<BuffRecord> innerIt = innerMap.values().iterator(); innerIt.hasNext();) {
                record = innerIt.next();

                record.cooldownCounter -= BUFF_RUN_CD;
                if (record.cooldownCounter <= 0 && record.isTimer()) {
                    if (buffTaskQueue.offer(record)) record.cooldownCounter = Math.max(1, record.getBuff().getCd());
                    else logWarning("buff异步队列已满，无法添加");
                }
                /* 检查持续时间 - 永续类不减少持续时间 */
                if (!record.isPermanent()) record.durationCounter -= BUFF_RUN_CD;
                if (record.durationCounter <= 0) {
                    changed = true;
                    innerIt.remove();
                }
            }
            if (changed) eventQueue.offer(cached.entity);
        }
    }

    private void initAsyncExecutor() {
        int coreThreads = Math.max(2, Runtime.getRuntime().availableProcessors());
        int maxThreads = Math.max(2, Runtime.getRuntime().availableProcessors() + 1);
        executor = new ThreadPoolExecutor(
                coreThreads,
                maxThreads,
                60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(20000),
                new ThreadPoolExecutor.DiscardOldestPolicy() // 丢弃旧任务保留新任务
        );
    }

    private void initSyncBuffExecutor() {
        syncBuffExecutor = new BukkitRunnable() {
            @Override
            public void run() {
                List<BuffRecord> buffsToExecute = new ArrayList<>();
                List<LivingEntity> attributeToChange = new ArrayList<>();

                /* 让异步线程调用 buff处理 函数 */
                executor.submit(BuffManager.this::asyncProcessEntityBuffs);
                /* 主线程执行buff效果 */
                buffTaskQueue.drainTo(buffsToExecute);
                // 可以设置负载，过高则分给下 RUN_CD - 1 个tick
                for (BuffRecord record : buffsToExecute) {
                    /* 获取缓存的实体并执行buff */
                    LivingEntity entity = entityCache.get(record.getUuid()).entity;
                    if (entity != null && entity.isValid() && !entity.isDead()) record.getBuff().execute(entity);
                    else entityCache.remove(record.getUuid());
                }
                /* 唤起事件 - 改变玩家属性 */
                eventQueue.drainTo(attributeToChange);
                for (LivingEntity entity : attributeToChange) syncCallAttributeChangeEvent(entity);
            }
        }.runTaskTimer(plugin, 1L, BUFF_RUN_CD);
    }

    private void loadBuffMap(){
        AtomicInteger total = new AtomicInteger();
        Path buffDir = plugin.getDataFolder().toPath().resolve("buffs");
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

                } else if (fileName.endsWith(".js")) {
                    bPDC = new BuffPDC(name, file);
                    buffMap.put(bPDC.getInnerName(), bPDC);
                    total.getAndIncrement();

                } else logWarning("未知的文件格式: " + fileName);
            });
            logInfo("[loadBuffs]: 共加载BUFF" + total.get() + "个");
        } catch (IOException e) {
            logWarning("加载buffs文件错误: " + e);
        }
    }

    // 实体状态缓存
    // 避免通过uuid获取实体这种复杂操作
    private static class CachedEntity {
        final UUID uuid;
        LivingEntity entity;

        CachedEntity(@NotNull LivingEntity entity) {
            this.uuid = entity.getUniqueId();
            this.entity = entity;
        }
    }
}
