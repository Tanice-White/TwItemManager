package io.github.tanice.twItemManager.listener;

import io.github.tanice.twItemManager.TwItemManager;
import io.github.tanice.twItemManager.infrastructure.PDCAPI;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class TwItemUpdateListener implements Listener {
    private final JavaPlugin plugin;
    private final List<Player> checkPlayers = new ArrayList<>();


    public TwItemUpdateListener(JavaPlugin plugin) {
        this.plugin = plugin;

        Bukkit.getPluginManager().registerEvents(this, plugin);

        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (checkPlayers.isEmpty()) return;
            List<Player> checkPlayers = new ArrayList<>(this.checkPlayers);
            this.checkPlayers.clear();
            for (Player player : checkPlayers) {
                if (player != null) checkAndUpdateItem(player, player.getInventory().getContents());
            }
        }, 20, 20);
    }

    public void onReload() {
        plugin.getLogger().info("TwItemUpdateListener reloaded");
    }

    @EventHandler
    void onPlayerHeld(@NotNull PlayerItemHeldEvent event) {
        Inventory inv = event.getPlayer().getInventory();
        checkAndUpdateItem(event.getPlayer(), inv.getItem(event.getPreviousSlot()), inv.getItem(event.getNewSlot()));
    }

    @EventHandler
    void onInventoryOpen(@NotNull InventoryOpenEvent event) {
        if (event.getPlayer() instanceof Player player) {
            if (player.equals(event.getInventory().getHolder())) checkPlayers.add(player);
        }
    }

    @EventHandler
    void onPlayerJoin(@NotNull PlayerJoinEvent event) {
        checkPlayers.add(event.getPlayer());
    }

    /**
     * 检查更新物品
     */
    private void checkAndUpdateItem(Player player, ItemStack @NotNull ... itemStacks) {
        for (ItemStack item : itemStacks) {
            if (item == null || item.getType().equals(Material.AIR)) continue;
            Long c = PDCAPI.getUpdateCode(item);
            if (c == null) continue;
            if (c == TwItemManager.getUpdateCode()) continue;
            updateItem(player, item);
            player.updateInventory();
        }
    }

    /**
     * 强制更新物品
     */
    public void updateItem(@NotNull Player player, @NotNull ItemStack item) {
        List<String> externalGems = TwItemManager.getItemManager().updateItem(player, item);
        StringBuilder s = new StringBuilder("§a装备更新");
        ItemStack g;
        for (String gn : externalGems) {
            s.append("，返还多余宝石: ");
            g = TwItemManager.getItemManager().generateItem(gn);
            player.getInventory().addItem(g);
            s.append(g.displayName()).append(" ");
        }
        player.sendMessage(s.toString());
        PDCAPI.updateUpdateCode(item);
    }
}
