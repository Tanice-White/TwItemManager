package io.github.tanice.twItemManager.event;

import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * 玩家数据中 最值 更改事件
 */
public class PlayerDataLimitChangeEvent extends Event implements Cancellable {
    private boolean cancelled;
    private static final HandlerList handlers = new HandlerList();

    @Getter
    private final Player player;

    public static HandlerList getHandlerList() {return handlers;}

    public PlayerDataLimitChangeEvent(@NotNull final Player player) {
        this.player = player;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }
}
