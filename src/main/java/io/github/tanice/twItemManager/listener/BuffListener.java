package io.github.tanice.twItemManager.listener;

import io.github.tanice.twItemManager.TwItemManager;
import io.github.tanice.twItemManager.config.Config;
import io.github.tanice.twItemManager.infrastructure.PDCAPI;
import io.github.tanice.twItemManager.manager.pdc.impl.EntityPDC;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static io.github.tanice.twItemManager.util.Logger.logInfo;

public class BuffListener implements Listener {
    private final JavaPlugin plugin;
    private final List<Player> checkPlayers;

    public BuffListener(@NotNull JavaPlugin plugin) {
        this.plugin = plugin;
        checkPlayers = new ArrayList<>();
        Bukkit.getPluginManager().registerEvents(this, plugin);
        logInfo("BuffListener loaded");
    }

    public void onReload() {
        plugin.getLogger().info("BuffListener reloaded");
    }

    @EventHandler
    public void onDie (@NotNull PlayerDeathEvent e) {
        EntityPDC ePDC = PDCAPI.getEntityCalculablePDC(e.getEntity());
        if (ePDC != null) {
            /* 若直接 new 可能会频繁扩容，增加 GC 的压力 */
            ePDC.removeAllBuffs();
            PDCAPI.setEntityCalculablePDC(e.getEntity(), ePDC);
        }
    }

    @EventHandler
    void onPlayerJoin(@NotNull PlayerJoinEvent event) {
        checkPlayers.add(event.getPlayer());
    }

    @EventHandler
    void onPlayerQuit(@NotNull PlayerQuitEvent event) {
        checkPlayers.remove(event.getPlayer());
        TwItemManager.getBuffManager().onPlayerQuit(event.getPlayer().getUniqueId());
    }

    /**
     * 手持 buff 生效
     */
    @EventHandler
    public void onItemHeld(@NotNull PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        ItemStack previousItem = player.getInventory().getItem(event.getPreviousSlot());
        ItemStack newItem = player.getInventory().getItem(event.getNewSlot());

        /* 让buff生效 */
        if (TwItemManager.getItemManager().isNotTwItem(previousItem) && TwItemManager.getItemManager().isNotTwItem(newItem)) return;
        changeBuff(player);
    }

    /**
     * 物品栏点击事件
     * @param event 事件
     */
    @EventHandler
    public void onInventoryClick(@NotNull InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player p)) return;
        /* 点击装备栏 */
        if (event.getClickedInventory() instanceof PlayerInventory) {
            int slot = event.getSlot();
            if (isArmorSlot(slot) || isHandSlot(slot)) changeBuff(p);
        }
    }

    /**
     * 玩家捡起物品事件
     * @param event 事件
     */
    @EventHandler
    public void onItemPickup(@NotNull EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player p)) return;
        ItemStack item = event.getItem().getItemStack();
        // 更新buff
        if (!TwItemManager.getItemManager().isNotTwItem(item) && isValidItem(item)) changeBuff(p);
    }

    /**
     * 玩家丢弃物品事件
     * @param event 事件
     */
    @EventHandler
    public void onItemDrop(@NotNull PlayerDropItemEvent event) {
        Player p = event.getPlayer();
        ItemStack item = event.getItemDrop().getItemStack();
        // 更新buff
        if (!TwItemManager.getItemManager().isNotTwItem(item) && isValidItem(item)) changeBuff(p);
    }

    /**
     * 玩家物品的 HoldBuff 生效
     */
    private void changeBuff(@NotNull LivingEntity e) {
        if (Config.debug) logInfo(e.getName() + ": buff updated");
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> TwItemManager.getBuffManager().updateHoldBuffs(e), 1L);
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
