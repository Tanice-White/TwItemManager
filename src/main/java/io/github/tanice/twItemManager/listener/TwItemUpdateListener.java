package io.github.tanice.twItemManager.listener;

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

import java.util.ArrayList;
import java.util.List;

public class TwItemUpdateListener implements Listener {
    private JavaPlugin plugin;
    private final List<Player> checkPlayers = new ArrayList<>();


    public TwItemUpdateListener(JavaPlugin plugin) {
        this.plugin = plugin;

        // 注册

        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!checkPlayers.isEmpty()) {
                List<Player> checkPlayers = new ArrayList<>(this.checkPlayers);
                this.checkPlayers.clear();
                for (Player player : checkPlayers) {
                    if (player != null) {
                        checkAndUpdateItem(player, player.getInventory().getContents());
                    }
                }
            }
        }, 20, 20);
    }

    @EventHandler
    void onPlayerHeld(PlayerItemHeldEvent event) {
        Inventory inv = event.getPlayer().getInventory();
        checkAndUpdateItem(event.getPlayer(), inv.getItem(event.getPreviousSlot()), inv.getItem(event.getNewSlot()));
    }

    @EventHandler
    void onInventoryOpen(InventoryOpenEvent event) {
        if (event.getPlayer() instanceof Player) {
            Player player = (Player) event.getPlayer();
            if (player.equals(event.getInventory().getHolder())) {
                checkPlayers.add(player);
            }
        }
    }

    @EventHandler
    void onPlayerJoin(PlayerJoinEvent event) {
        checkPlayers.add(event.getPlayer());
    }

    /**
     * 检查更新物品
     */
    private void checkAndUpdateItem(Player player, ItemStack... itemStacks) {
        for (ItemStack item : itemStacks) {
            if (item == null || item.getType().equals(Material.AIR)) continue;



//            val oldWrapper = NbtUtil.getInst().getItemTagWrapper(item);
//            IGenerator generator = itemMap.get(oldWrapper.getString(itemKey));
//            if (generator instanceof IUpdate) {
//                IUpdate updateIg = (IUpdate) generator;
//                Integer hashCode = oldWrapper.getInt(hashCodeKey);
//                if (!updateIg.isUpdate() || (hashCode != null && updateIg.updateCode() == hashCode)) continue;
//                updateItem(player, item, updateIg, oldWrapper);
//            }
            player.updateInventory();
        }
    }

//    /**
//     * 强制更新物品
//     */
//    private void updateItem(Player player, ItemStack item) {
//        val oldWrapper = NbtUtil.getInst().getItemTagWrapper(item);
//        IGenerator generator = itemMap.get(oldWrapper.getString(itemKey));
//        if (!(generator instanceof IUpdate)) return;
//        updateItem(player, item, (IUpdate) generator, oldWrapper);
//    }
//
//    /**
//     * 强制更新物品
//     */
//    public void updateItem(Player player, ItemStack item, IUpdate updateIg, NbtUtil.Wrapper oldWrapper) {
//        ItemStack newItem = updateIg.update(item, oldWrapper, player);
//        val wrapper = NbtUtil.getInst().getItemTagWrapper(newItem);
//        wrapper.set(itemKey, updateIg.getKey());
//        wrapper.set(hashCodeKey, updateIg.updateCode());
//        updateIg.protectNBT(wrapper, oldWrapper, protectNbtList);
//
//        SXItemUpdateEvent event = new SXItemUpdateEvent(plugin, player, (IGenerator) updateIg, newItem, item);
//        Bukkit.getPluginManager().callEvent(event);
//        if (event.isCancelled()) return;
//        item.setType(event.getItem().getType());
//        item.setItemMeta(event.getItem().getItemMeta());
//    }
}
