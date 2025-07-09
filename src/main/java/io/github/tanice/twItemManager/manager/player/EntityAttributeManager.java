package io.github.tanice.twItemManager.manager.player;

import io.github.tanice.twItemManager.config.Config;
import io.github.tanice.twItemManager.manager.calculator.Calculator;
import io.github.tanice.twItemManager.manager.calculator.LivingEntityCombatPowerCalculator;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static io.github.tanice.twItemManager.util.Logger.logInfo;

public class EntityAttributeManager {
    /** 计算结果-实体属性 */
    private final ConcurrentMap<String, Calculator> calculatorMap;
    /** 记录待更新实体（最新请求） */
    private final ConcurrentMap<String, AtomicReference<LivingEntity>> pendingUpdates;
    /** 标记实体是否正在计算中 */
    private final ConcurrentMap<String, AtomicBoolean> computingFlags;
    /** 脏标记 */
    private final ConcurrentMap<String, AtomicBoolean> dirtyFlags;
    /** 线程池 */
    private final ExecutorService executor;

    public EntityAttributeManager() {
        calculatorMap = new ConcurrentHashMap<>();
        pendingUpdates = new ConcurrentHashMap<>();
        computingFlags = new ConcurrentHashMap<>();
        dirtyFlags = new ConcurrentHashMap<>();

        int coreThreads = Math.max(2, Runtime.getRuntime().availableProcessors());
        int maxThreads = Math.max(2, Runtime.getRuntime().availableProcessors() + 1);
        executor = new ThreadPoolExecutor(
                coreThreads,
                maxThreads,
                60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(20000),
                new ThreadPoolExecutor.DiscardOldestPolicy() // 丢弃旧任务保留新任务
        );
        logInfo("EntityAttributeManager loaded, Scheduler started with " + coreThreads + " core threads");
    }

    public void onReload() {
        /* 收集所有需要更新的实体(已计算但不在队列中) */
        Set<String> entitiesToUpdate = new HashSet<>(calculatorMap.keySet());
        entitiesToUpdate.removeAll(pendingUpdates.keySet());

        calculatorMap.clear();
        Server server = Bukkit.getServer();

        /* 为符合条件的实体重新提交请求 */
        int updatedCount = 0;
        for (String uuid : entitiesToUpdate) {
            try {
                UUID entityUUID = UUID.fromString(uuid);
                // 优先检查玩家
                LivingEntity entity = server.getPlayer(entityUUID);
                if (entity == null) {
                    // 尝试从世界获取实体
                    entity = Bukkit.getWorlds().stream()
                            .flatMap(world -> world.getLivingEntities().stream())
                            .filter(e -> e.getUniqueId().equals(entityUUID))
                            .findFirst()
                            .orElse(null);
                }
                
                if (entity != null && entity.isValid() && !entity.isDead()) {
                    submitAsyncCalculation(entity);
                    updatedCount++;
                }
            } catch (IllegalArgumentException e) {
                logInfo("[EntityAttribute] 跳过无效UUID: " + uuid);
            }
        }
        /* 重置计算标志 */
        for (AtomicBoolean flag : computingFlags.values()) flag.set(false);
        logInfo("[EntityAttribute] Reloaded: " + updatedCount + " entities queued for recalculation (" + entitiesToUpdate.size() + " eligible)");
    }

    public void onDisable() {
        executor.shutdownNow();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                System.err.println("[EntityAttribute] Thread pool did not terminate in time");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        calculatorMap.clear();
        pendingUpdates.clear();
        computingFlags.clear();
        dirtyFlags.clear();
    }

    /**
     * 提交实体属性异步计算任务（带去重）
     */
    public void submitAsyncCalculation(@NotNull LivingEntity entity) {
        String uuid = entity.getUniqueId().toString();

        // 更新最新实体引用
        pendingUpdates.compute(uuid, (k, v) -> {
            if (v == null) v = new AtomicReference<>(entity);
            else v.set(entity);
            return v;
        });

        dirtyFlags.computeIfAbsent(uuid, k -> new AtomicBoolean(false));

        AtomicBoolean computing = computingFlags.computeIfAbsent(uuid, k -> new AtomicBoolean(false));
        if (computing.compareAndSet(false, true)) {
            executor.submit(() -> processEntityUpdate(uuid));
            if (Config.debug) logInfo("[Calculator] 实体" + entity.getName() + " 属性更新");
        } else {
            // 计算中，标记为脏
            dirtyFlags.get(uuid).set(true);
        }
    }

    /**
     * 处理实体更新（核心去重逻辑）
     */
    private void processEntityUpdate(String uuid) {
        try {
            // 计算前清除脏标记
            dirtyFlags.get(uuid).set(false);

            AtomicReference<LivingEntity> entityRef = pendingUpdates.get(uuid);
            if (entityRef == null) return;

            LivingEntity latestEntity = entityRef.get();
            if (latestEntity == null || !latestEntity.isValid() || latestEntity.isDead()) return;

            Calculator calculator = new LivingEntityCombatPowerCalculator(latestEntity);
            calculatorMap.put(uuid, calculator);

        } finally {
            computingFlags.get(uuid).set(false);
            // 如果期间有新请求，继续处理
            if (dirtyFlags.get(uuid).getAndSet(false)) {
                if (computingFlags.get(uuid).compareAndSet(false, true)) {
                    executor.submit(() -> processEntityUpdate(uuid));
                }
            }
        }
    }

    /**
     * 获取实体计算器（高频访问优化）
     */
    public Calculator getCalculator(@NotNull LivingEntity livingEntity) {
        Calculator calculator = calculatorMap.get(livingEntity.getUniqueId().toString());
        /* 没有则现场计算 */
        if (calculator == null) {
            LivingEntityCombatPowerCalculator nc = new LivingEntityCombatPowerCalculator(livingEntity);
            calculatorMap.put(livingEntity.getUniqueId().toString(), nc);
            return nc;
        }
        return calculator;
    }

    /**
     * 移除实体数据
     */
    public void removeEntity(@NotNull UUID uuid) {
        String id = uuid.toString();
        calculatorMap.remove(id);
        pendingUpdates.remove(id);
        computingFlags.remove(id);
        dirtyFlags.remove(id);
    }

    /**
     * 获取当前管理的实体数量
     */
    public int getManagedEntityCount() {
        return calculatorMap.size();
    }

    /**
     * 获取线程池状态信息
     */
    public String getThreadPoolStatus() {
        ThreadPoolExecutor tpe = (ThreadPoolExecutor) executor;
        return String.format("活跃线程: %d, 核心线程: %d, 最大线程: %d, 队列大小: %d", 
                tpe.getActiveCount(), tpe.getCorePoolSize(), tpe.getMaximumPoolSize(), tpe.getQueue().size());
    }
}
