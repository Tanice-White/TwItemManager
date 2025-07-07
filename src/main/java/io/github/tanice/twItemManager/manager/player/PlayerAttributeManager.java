package io.github.tanice.twItemManager.manager.player;

import io.github.tanice.twItemManager.config.Config;
import io.github.tanice.twItemManager.manager.calculator.Calculator;
import io.github.tanice.twItemManager.manager.calculator.LivingEntityCombatPowerCalculator;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static io.github.tanice.twItemManager.util.Logger.logInfo;

public class PlayerAttributeManager {
    /** 计算结果-玩家属性 */
    private final ConcurrentMap<String, Calculator> calculatorMap = new ConcurrentHashMap<>();
    /** 记录待更新玩家（最新请求） */
    private final ConcurrentMap<String, AtomicReference<Player>> pendingUpdates = new ConcurrentHashMap<>();
    /** 标记玩家是否正在计算中 */
    private final ConcurrentMap<String, AtomicBoolean> computingFlags = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, AtomicBoolean> dirtyFlags = new ConcurrentHashMap<>();

    // 线程池配置
    private final ExecutorService executor = new ThreadPoolExecutor(
            Runtime.getRuntime().availableProcessors(),
            Runtime.getRuntime().availableProcessors() * 2,
            60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(10000),
            new ThreadPoolExecutor.DiscardOldestPolicy() // 丢弃旧任务保留新任务
    );

    public void onReload() {
        /* 收集所有需要更新的玩家(已计算但不在队列中) */
        Set<String> playersToUpdate = new HashSet<>(calculatorMap.keySet());
        playersToUpdate.removeAll(pendingUpdates.keySet());

        calculatorMap.clear();
        Server server = Bukkit.getServer();

        /* 为符合条件的玩家重新提交请求 */
        int updatedCount = 0;
        for (String uuid : playersToUpdate) {
            Player player = server.getPlayer(UUID.fromString(uuid));
            if (player != null && player.isOnline()) {
                submitAsyncCalculation(player);
                updatedCount++;
            }
        }
        /* 重置计算标志 */
        for (AtomicBoolean flag : computingFlags.values()) flag.set(false);
        logInfo("[PlayerAttribute] Reloaded: " + updatedCount + " players queued for recalculation (" + playersToUpdate.size() + " eligible)");
    }

    public void onDisable() {
        executor.shutdownNow();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                System.err.println("[PlayerAttribute] Thread pool did not terminate in time");
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
     * 提交玩家属性异步计算任务（带去重）
     */
    public void submitAsyncCalculation(@NotNull Player player) {
        String uuid = player.getUniqueId().toString();

        // 更新最新玩家引用
        pendingUpdates.compute(uuid, (k, v) -> {
            if (v == null) v = new AtomicReference<>(player);
            else v.set(player);
            return v;
        });

        dirtyFlags.computeIfAbsent(uuid, k -> new AtomicBoolean(false));

        AtomicBoolean computing = computingFlags.computeIfAbsent(uuid, k -> new AtomicBoolean(false));
        if (computing.compareAndSet(false, true)) {
            executor.submit(() -> processPlayerUpdate(uuid));
        } else {
            // 计算中，标记为脏
            dirtyFlags.get(uuid).set(true);
        }

        if (Config.debug) logInfo("[Calculator] 玩家" + player.getName() + " 属性更新");
    }

    /**
     * 处理玩家更新（核心去重逻辑）
     */
    private void processPlayerUpdate(String uuid) {
        try {
            // 计算前清除脏标记
            dirtyFlags.get(uuid).set(false);

            AtomicReference<Player> playerRef = pendingUpdates.get(uuid);
            if (playerRef == null) return;

            Player latestPlayer = playerRef.get();
            if (latestPlayer == null || !latestPlayer.isOnline()) return;

            Calculator calculator = new LivingEntityCombatPowerCalculator(latestPlayer);
            calculatorMap.put(uuid, calculator);

        } finally {
            computingFlags.get(uuid).set(false);
            // 如果期间有新请求，继续处理
            if (dirtyFlags.get(uuid).getAndSet(false)) {
                if (computingFlags.get(uuid).compareAndSet(false, true)) {
                    executor.submit(() -> processPlayerUpdate(uuid));
                }
            }
        }
    }

    /**
     * 获取玩家计算器（高频访问优化）
     */
    public Calculator getCalculator(@NotNull Player player) {
        Calculator calculator = calculatorMap.get(player.getUniqueId().toString());
        /* 没有则现场计算 */
        if (calculator == null) return new LivingEntityCombatPowerCalculator(player);
        return calculator;
    }

    /**
     * 移除离线玩家数据
     */
    public void removePlayer(@NotNull UUID uuid) {
        String id = uuid.toString();
        calculatorMap.remove(id);
        pendingUpdates.remove(id);
        computingFlags.remove(id);
        dirtyFlags.remove(id);
    }
}
