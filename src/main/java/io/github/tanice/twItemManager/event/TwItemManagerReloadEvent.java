package io.github.tanice.twItemManager.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class TwItemManagerReloadEvent extends Event {
    private static final HandlerList handlers = new HandlerList();

    public static HandlerList getHandlerList() {return handlers;}

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }
}
