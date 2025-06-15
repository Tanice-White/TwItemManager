package io.github.tanice.twItemManager.listener;

import io.github.tanice.twItemManager.TwItemManager;
import io.github.tanice.twItemManager.manager.item.base.impl.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class BuffListener implements Listener {
    private final JavaPlugin plugin;

    private final List<Player> checkPlayers = new ArrayList<>();

    /**
     * 创建holdBuff检测，防止刷出无限buff
     */
    public BuffListener(@NotNull JavaPlugin plugin) {
        this.plugin = plugin;

        new BukkitRunnable() {
            @Override
            public void run() {}
        };
    }

    @EventHandler
    void onPlayerJoin(@NotNull PlayerJoinEvent event) {
        checkPlayers.add(event.getPlayer());
    }

    /**
     * 手持 buff 生效
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onItemHeld(@NotNull PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        ItemStack previousItem = player.getInventory().getItem(event.getPreviousSlot());
        ItemStack newItem = player.getInventory().getItem(event.getNewSlot());
        /* 让buff生效 */
        changeBuff(player, previousItem, newItem);
    }

    /**
     * 物品栏点击事件
     * @param event 事件
     */
    @EventHandler
    public void onInventoryClick(@NotNull InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        /* 点击装备栏 */
        if (event.getClickedInventory() instanceof PlayerInventory) {
            int slot = event.getSlot();
            if (isArmorSlot(slot) || isHandSlot(slot)) {
                // 更新buff
            }
        }
    }

    /**
     * 玩家捡起物品事件
     * @param event 事件
     */
    @EventHandler
    public void onItemPickup(@NotNull EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        ItemStack item = event.getItem().getItemStack();
        // 更新buff
    }

    /**
     * 玩家丢弃物品事件
     * @param event 事件
     */
    @EventHandler
    public void onItemDrop(@NotNull PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItemDrop().getItemStack();

        // 更新buff
    }

    /**
     * 玩家物品的 HoldBuff 生效
     */
    private void changeBuff(@NotNull Player p, @Nullable ItemStack pre, @Nullable ItemStack current) {
        Item i = TwItemManager.getItemManager().getItemByItemStack(pre);
        if (i != null && isValidItem(pre)) TwItemManager.getBuffManager().deactivateBuff(p, i.getHoldBuffs());

        i = TwItemManager.getItemManager().getItemByItemStack(current);
        if (i != null && isValidItem(current)) TwItemManager.getBuffManager().activeBuff(p, i.getHoldBuffs());
    }

    /**
     * 检查物品耐久等方面确保可使用
     */
    public boolean isValidItem(@Nullable ItemStack itemStack) {
        if (itemStack == null) return false;
        return true;
    }

    /**
     * 检查是否是装备栏
     * @param slot 槽位
     * @return 是否是装备栏
     */
    private boolean isArmorSlot(int slot) {
        return slot >= 36 && slot <= 39; // 头盔、胸甲、护腿、靴子
    }

    /**
     * 检查是否是手部栏
     * @param slot 槽位
     * @return 是否是手部栏
     */
    private boolean isHandSlot(int slot) {
        return slot == 40 || slot == 41; // 主手、副手
    }
}
