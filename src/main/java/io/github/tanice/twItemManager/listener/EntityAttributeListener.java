package io.github.tanice.twItemManager.listener;

import io.github.tanice.twItemManager.TwItemManager;
import io.github.tanice.twItemManager.event.EntityAttributeChangeEvent;
import io.papermc.paper.event.entity.EntityEquipmentChangedEvent;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import static io.github.tanice.twItemManager.util.Logger.logInfo;

/**
 * 属性跟新应该在buff之后，所以优先级最高
 */
public class EntityAttributeListener implements Listener {
    private final JavaPlugin plugin;

    public EntityAttributeListener(@NotNull JavaPlugin plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
        logInfo("EntityAttributeListener loaded");
    }

    /**
     * 监听 实体属性更新 事件
     * 多在 buff 改变触发(两者的触发完全一致，但是它必须在 buff 改变之后触发，所以融入 buff 监听中)
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onAttributeChange(@NotNull EntityAttributeChangeEvent event) {
        TwItemManager.getEntityAttributeManager().submitAsyncCalculation(event.getEntity());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerQuit(@NotNull PlayerQuitEvent event) {
        TwItemManager.getEntityAttributeManager().removeEntity(event.getPlayer().getUniqueId());
    }
}
