package io.github.tanice.twItemManager.listener;

import io.github.tanice.twItemManager.TwItemManager;
import io.github.tanice.twItemManager.config.Config;
import io.github.tanice.twItemManager.event.PlayerDataLimitChangeEvent;
import io.github.tanice.twItemManager.manager.player.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicReference;

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
        if (!Config.use_mysql) return;
        Player p =event.getPlayer();
        TwItemManager.getDatabaseManager().loadPlayerData(p.getUniqueId().toString())
                .thenAccept(playerData -> {
                    PlayerData finalPlayerData = playerData == null ? PlayerData.initPlayerData(p) : playerData;
                    // 切回主线程
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        /* 数据库读取的类中player是空的, 在 selfActivate 会作检测并初始化 */
                        plugin.getServer().getScheduler().runTaskLater(plugin, finalPlayerData::selfActivate, 1L);
                    });
                });
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerQuit(@NotNull PlayerQuitEvent event) {
        if (!Config.use_mysql) return;
        Player p =event.getPlayer();
        PlayerData playerData = PlayerData.readPlayerData(p);
        TwItemManager.getDatabaseManager().savePlayerData(playerData);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDataChange(@NotNull PlayerDataLimitChangeEvent event) {
        if (!Config.use_mysql) return;
        Player p = event.getPlayer();
        PlayerData playerData = PlayerData.readPlayerData(p);
        TwItemManager.getDatabaseManager().savePlayerData(playerData);
    }
}
