package io.github.tanice.twItemManager.manager.buff;

import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

// 实体状态缓存
// 避免通过uuid获取实体这种复杂操作
public class CachedEntity {
    final UUID uuid;
    public LivingEntity entity;

    public CachedEntity(@NotNull LivingEntity entity) {
        this.uuid = entity.getUniqueId();
        this.entity = entity;
    }
}
