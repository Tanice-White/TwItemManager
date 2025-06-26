package io.github.tanice.twItemManager.listener;

import io.github.tanice.twItemManager.TwItemManager;
import io.github.tanice.twItemManager.event.PlayerDataChangeEvent;
import io.github.tanice.twItemManager.manager.player.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import static io.github.tanice.twItemManager.util.Logger.logInfo;

public class PlayerDataListener implements Listener{
    private final JavaPlugin plugin;

    public PlayerDataListener(@NotNull JavaPlugin plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
        logInfo("PlayerDataListener loaded");
    }

    public void onReload() {
        plugin.getLogger().info("BuffListener reloaded");
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(@NotNull PlayerJoinEvent event) {
        Player p =event.getPlayer();

        PlayerData playerData = TwItemManager.getDatabaseManager().loadPlayerData(p.getUniqueId().toString());
        if(playerData == null) playerData = PlayerData.initPlayerData(p);
        playerData.selfActivate();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerQuit(@NotNull PlayerQuitEvent event) {
        Player p =event.getPlayer();
        PlayerData playerData = PlayerData.readPlayerData(p);
        TwItemManager.getDatabaseManager().savePlayerData(playerData);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDataChange(@NotNull PlayerDataChangeEvent event) {
        Player p =event.getPlayer();
        PlayerData playerData = PlayerData.readPlayerData(p);
        TwItemManager.getDatabaseManager().savePlayerData(playerData);
    }
}
