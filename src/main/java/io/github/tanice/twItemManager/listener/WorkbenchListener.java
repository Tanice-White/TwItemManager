package io.github.tanice.twItemManager.listener;

import io.github.tanice.twItemManager.TwItemManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

/**
 * 处理强化和宝石镶嵌
 */
public class WorkbenchListener implements Listener {
    private final JavaPlugin plugin;

    public WorkbenchListener(JavaPlugin plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
        plugin.getLogger().info("WorkbenchListener Registered");
    }

    public void onReload() {
        plugin.getLogger().info("WorkbenchListener reloaded");
    }

    /**
     * 处理武器升级和宝石镶嵌
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryClickForLevelUp(@NotNull InventoryClickEvent event) {
        // 检查是否是工作台界面 左键点击
        if (event.getInventory().getType() != InventoryType.WORKBENCH || event.getClick() != ClickType.LEFT) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;
        /* 获取光标物品和被点击物品 */
        ItemStack cursorItem = event.getCursor();
        ItemStack clickedItem = event.getCurrentItem();
        if (TwItemManager.getItemManager().isNotItemClassInTwItem(cursorItem) || TwItemManager.getItemManager().isNotItemClassInTwItem(clickedItem)) return;

        /* 排除非工作台的9个槽位 */
        InventoryView view = event.getView();
        Inventory clickedInventory = event.getClickedInventory();
        if (clickedInventory != view.getTopInventory() || event.getSlot() < 1 || event.getSlot() > 9) return;

        /* 等级升级 */
        if (TwItemManager.getItemManager().levelUp(player, clickedItem, cursorItem, true)) {
            /* 更新 */
            event.setCurrentItem(clickedItem);
            cursorItem.setAmount(cursorItem.getAmount() - 1);
            /* 取消后续默认操作 */
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onInventoryClickForInsertGem(@NotNull InventoryClickEvent event) {
        // 检查是否是工作台界面 左键点击
        if (event.getInventory().getType() != InventoryType.WORKBENCH || event.getClick() != ClickType.LEFT) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;
        /* 获取光标物品和被点击物品 */
        ItemStack cursorItem = event.getCursor();
        ItemStack clickedItem = event.getCurrentItem();
        if (TwItemManager.getItemManager().isNotItemClassInTwItem(clickedItem) || TwItemManager.getItemManager().isNotItemClassInTwItem(cursorItem)) return;

        /* 排除非工作台的9个槽位 */
        InventoryView view = event.getView();
        Inventory clickedInventory = event.getClickedInventory();
        if (clickedInventory != view.getTopInventory() || event.getSlot() < 1 || event.getSlot() > 9) return;

        /* 宝石镶嵌 */
        if (TwItemManager.getItemManager().insertGem(player, clickedItem, cursorItem)){
            event.setCurrentItem(clickedItem);
            cursorItem.setAmount(cursorItem.getAmount() - 1);
            /* 取消后续默认操作 */
            event.setCancelled(true);
        }
    }
}
