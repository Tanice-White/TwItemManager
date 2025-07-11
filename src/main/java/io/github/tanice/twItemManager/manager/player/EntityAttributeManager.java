package io.github.tanice.twItemManager.manager.player;

import io.github.tanice.twItemManager.config.Config;
import io.github.tanice.twItemManager.calculator.Calculator;
import io.github.tanice.twItemManager.calculator.LivingEntityCombatPowerCalculator;
import io.github.tanice.twItemManager.event.entity.PlayerSkillChangeEvent;
import io.github.tanice.twItemManager.manager.AsyncDirtyUpdateManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static io.github.tanice.twItemManager.util.Logger.logInfo;

/**
 * 实体属性异步计算
 */
public class EntityAttributeManager extends AsyncDirtyUpdateManager {
    /** 计算结果-实体属性 */
    private final ConcurrentMap<UUID, Calculator> calculatorMap;


    public EntityAttributeManager() {
        super();
        calculatorMap = new ConcurrentHashMap<>();
        logInfo("EntityAttributeManager loaded, Scheduler started");
    }

    public void onReload() {
        super.onReload();
        calculatorMap.clear();
    }

    public void onDisable() {
        super.onDisable();
        calculatorMap.clear();
    }

    /**
     * 提交实体属性异步计算任务
     */
    public void submitAsyncTask(@NotNull LivingEntity entity) {
        UUID uuid = entity.getUniqueId();

        // 更新最新实体引用
        pendingUpdates.compute(uuid, (k, v) -> {
            if (v == null) v = new AtomicReference<>(entity);
            else v.set(entity);
            return v;
        });
        dirtyFlags.computeIfAbsent(uuid, k -> new AtomicBoolean(false));

        AtomicBoolean computing = computingFlags.computeIfAbsent(uuid, k -> new AtomicBoolean(false));
        if (computing.compareAndSet(false, true)) {
            asyncExecutor.submit(() -> asyncRun(entity));
            if (Config.debug) logInfo("[Calculator] 实体" + entity.getName() + " 属性更新");

            /* 计算中，标记为脏 */
        } else dirtyFlags.get(uuid).set(true);

        /* 为了尽量保持装备和技能的一致，在这里让玩家技能做出变化 */
        if (entity instanceof Player player) Bukkit.getPluginManager().callEvent(new PlayerSkillChangeEvent(player));
    }

    /**
     * 处理实体更新 (已经死亡的实体会自动删除, 且只有计算依赖游戏实体, 故不需要缓存实体)
     */
    private void asyncRun(@NotNull LivingEntity entity) {
        UUID uuid = entity.getUniqueId();
        try {
            /* 计算前清除脏标记 */
            dirtyFlags.get(uuid).set(false);

            AtomicReference<LivingEntity> entityRef = pendingUpdates.get(uuid);
            if (entityRef == null) {
                calculatorMap.remove(uuid);
                return;
            }

            LivingEntity latestEntity = entityRef.get();
            if (latestEntity == null || !latestEntity.isValid() || latestEntity.isDead()) {
                calculatorMap.remove(uuid);
                return;
            }

            Calculator calculator = new LivingEntityCombatPowerCalculator(latestEntity);
            calculatorMap.put(uuid, calculator);

        } finally {
            computingFlags.get(uuid).set(false);
            /* 如果期间有新请求，继续处理 */
            if (dirtyFlags.get(uuid).getAndSet(false)) {
                if (computingFlags.get(uuid).compareAndSet(false, true)) {
                    asyncExecutor.submit(() -> asyncRun(entity));
                }
            }
        }
    }

    /**
     * 获取实体计算器（高频访问优化）
     */
    public Calculator getCalculator(@NotNull LivingEntity entity) {
        UUID uuid = entity.getUniqueId();
        Calculator calculator = calculatorMap.get(uuid);
        /* 没有则现场计算 */
        if (calculator == null) {
            LivingEntityCombatPowerCalculator nc = new LivingEntityCombatPowerCalculator(entity);
            calculatorMap.put(uuid, nc);
            return nc;
        }
        return calculator;
    }

    /**
     * 移除实体数据
     */
    public void removeEntityData(@NotNull LivingEntity entity) {
        UUID uuid = entity.getUniqueId();
        calculatorMap.remove(uuid);
        pendingUpdates.remove(uuid);
        computingFlags.remove(uuid);
        dirtyFlags.remove(uuid);
    }

    /**
     * 获取当前管理的实体数量
     */
    public int getManagedEntityCount() {
        return calculatorMap.size();
    }
}
