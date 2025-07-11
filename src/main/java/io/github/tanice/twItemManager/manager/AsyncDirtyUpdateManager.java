package io.github.tanice.twItemManager.manager;

import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public abstract class AsyncDirtyUpdateManager extends AsyncManager{
    /** 记录待更新实体（最新请求） */
    protected final ConcurrentMap<UUID, AtomicReference<LivingEntity>> pendingUpdates;
    /** 标记实体是否正在计算中 */
    protected final ConcurrentMap<UUID, AtomicBoolean> computingFlags;
    /** 脏标记 */
    protected final ConcurrentMap<UUID, AtomicBoolean> dirtyFlags;

    public AsyncDirtyUpdateManager() {
        super();
        pendingUpdates = new ConcurrentHashMap<>();
        computingFlags = new ConcurrentHashMap<>();
        dirtyFlags = new ConcurrentHashMap<>();
    }

    public void onReload() {
        super.onReload();
        pendingUpdates.clear();
        computingFlags.clear();
        dirtyFlags.clear();
    }

    public void onDisable() {
        super.onDisable();
        pendingUpdates.clear();
        computingFlags.clear();
        dirtyFlags.clear();
    }
}
