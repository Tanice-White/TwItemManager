package io.github.tanice.twItemManager.event;

import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * 玩家属性更新
 */
@Getter
public class PlayerAttributeChangeEvent extends Event {
    private static final HandlerList handlers = new HandlerList();

    private final Player player;

    public PlayerAttributeChangeEvent(@NotNull Player player) {
        this.player = player;
    }


    public static HandlerList getHandlerList() {return handlers;}

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }
}
