package io.github.tanice.twItemManager.listener;

import io.github.tanice.twItemManager.TwItemManager;
import io.github.tanice.twItemManager.event.TwItemUpdateEvent;
import io.github.tanice.twItemManager.infrastructure.PDCAPI;
import io.github.tanice.twItemManager.manager.item.base.impl.Item;
import io.github.tanice.twItemManager.manager.pdc.CalculablePDC;
import io.github.tanice.twItemManager.manager.pdc.impl.AttributePDC;
import io.github.tanice.twItemManager.manager.pdc.impl.ItemPDC;
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

import static io.github.tanice.twItemManager.util.Logger.logWarning;

public class TwItemUpdateListener implements Listener {
    private final JavaPlugin plugin;
    private final List<Player> checkPlayers = new ArrayList<>();


    public TwItemUpdateListener(JavaPlugin plugin) {
        this.plugin = plugin;

        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (checkPlayers.isEmpty()) return;
            List<Player> checkPlayers = new ArrayList<>(this.checkPlayers);
            this.checkPlayers.clear();
            for (Player player : checkPlayers) {
                if (player != null) checkAndUpdateItem(player, player.getInventory().getContents());
            }
        }, 20, 20);
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
            updateItem(player, item);
            player.updateInventory();
        }
    }

    /**
     * 强制更新物品
     */
    public void updateItem(@NotNull Player player, @NotNull ItemStack item) {
        List<String> externalGems = TwItemManager.getItemManager().updateItem(player, item);
        StringBuilder s = new StringBuilder("§a装备更新, 返还多余宝石(");
        ItemStack g;
        for (String gn : externalGems) {
            g = TwItemManager.getItemManager().generateGemItem(gn);
            player.getInventory().addItem(g);
            s.append(((AttributePDC) PDCAPI.getItemCalculablePDC(g)).getDisplayName()).append(" ");
        }
        s.append(")");
        player.sendMessage(s.toString());
    }
}
