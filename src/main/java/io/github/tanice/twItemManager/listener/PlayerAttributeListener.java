package io.github.tanice.twItemManager.listener;

import io.github.tanice.twItemManager.TwItemManager;
import io.github.tanice.twItemManager.event.PlayerAttributeChangeEvent;
import io.papermc.paper.event.entity.EntityEquipmentChangedEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import static io.github.tanice.twItemManager.util.Logger.logInfo;
import static io.github.tanice.twItemManager.util.Logger.logWarning;

public class PlayerAttributeListener implements Listener {
    private final JavaPlugin plugin;

    // TODO buff 监听合并
    // Bukkit.getPluginManager().callEvent(new PlayerAttributeChangeEvent(p));

    public PlayerAttributeListener(@NotNull JavaPlugin plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
        logInfo("PlayerAttributeListener loaded");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onAttributeChange(PlayerAttributeChangeEvent event) {
        TwItemManager.getPlayerAttributeManager().submitAsyncCalculation(event.getPlayer());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        TwItemManager.getPlayerAttributeManager().removePlayer(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerRespawn(@NotNull PlayerRespawnEvent event) {
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> Bukkit.getPluginManager().callEvent(new PlayerAttributeChangeEvent(event.getPlayer())), 1L);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(@NotNull PlayerJoinEvent event) {
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> Bukkit.getPluginManager().callEvent(new PlayerAttributeChangeEvent(event.getPlayer())), 1L);
    }

    @EventHandler
    public void onEntityEquipmentChange(@NotNull EntityEquipmentChangedEvent event) {
        // TODO 这里甚至能检测所有 LivingEntity 的
        // TODO 让生物的属性也生效！
        LivingEntity livingEntity = event.getEntity();
        logWarning("EntityEquipmentChangedEvent " + livingEntity.getName());

        // TODO 判断更新的东西是TwItem
        if (livingEntity instanceof Player p){
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> Bukkit.getPluginManager().callEvent(new PlayerAttributeChangeEvent(p)), 1L);
        }
    }
}
