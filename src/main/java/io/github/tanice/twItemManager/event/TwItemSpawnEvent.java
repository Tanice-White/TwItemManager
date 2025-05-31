package io.github.tanice.twItemManager.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;


@AllArgsConstructor
public class TwItemSpawnEvent extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();
    private final Player player;
//    @Getter
//    private final AbstractItemGenerator aig;
    @Getter
    private ItemStack item;
    private boolean cancelled;

    public static HandlerList getHandlerList() {return handlers;}
    @Override
    public @NotNull HandlerList getHandlers() {return handlers;}

    @Override
    public boolean isCancelled() {return cancelled;}
    @Override
    public void setCancelled(boolean b) {this.cancelled = b;}

}
