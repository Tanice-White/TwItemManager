package io.github.tanice.twItemManager.event;

import io.github.tanice.twItemManager.manager.pdc.impl.ItemPDC;
import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

@Getter
public class TwItemUpdateEvent extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();
    private boolean cancelled;

    private final JavaPlugin plugin;
    private final Player player;
    private final ItemStack pre;

    public TwItemUpdateEvent(@NotNull JavaPlugin plugin, @NotNull Player player, @NotNull ItemStack pre) {
        this.plugin = plugin;
        this.player = player;
        this.pre = pre;
    }

    public static HandlerList getHandlerList() {return handlers;}

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean b) {
        cancelled = b;
    }
}
