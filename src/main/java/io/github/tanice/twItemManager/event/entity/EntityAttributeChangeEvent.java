package io.github.tanice.twItemManager.event.entity;

import lombok.Getter;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * 实体属性更新
 */
@Getter
public class EntityAttributeChangeEvent extends Event {
    private static final HandlerList handlers = new HandlerList();

    private final LivingEntity entity;

    public EntityAttributeChangeEvent(@NotNull LivingEntity entity) {
        this.entity = entity;
    }

    public static HandlerList getHandlerList() {return handlers;}

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }
}
