package io.github.tanice.twItemManager.event.entity;

import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * 玩家可用技能改变
 */
@Getter
public class PlayerSkillChangeEvent extends Event {
    private static final HandlerList handlers = new HandlerList();

    private final Player player;

    public PlayerSkillChangeEvent(@NotNull Player player) {
        this.player = player;
    }

    public static HandlerList getHandlerList() {return handlers;}

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }
}
