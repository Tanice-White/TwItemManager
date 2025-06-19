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
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spigotmc.event.player.PlayerSpawnLocationEvent;

import static io.github.tanice.twItemManager.util.Logger.logInfo;
import static io.github.tanice.twItemManager.util.Logger.logWarning;

public class BuffListener implements Listener {
    private final JavaPlugin plugin;

    public BuffListener(@NotNull JavaPlugin plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
        logInfo("BuffListener loaded");
    }

    public void onReload() {
        plugin.getLogger().info("BuffListener reloaded");
    }

    @EventHandler
    public void onPlayerDie (@NotNull PlayerDeathEvent event) {
        Player p =event.getPlayer();
        EntityPDC ePDC = PDCAPI.getEntityCalculablePDC(p);
        if (ePDC != null) {
            /* 若直接 new 可能会频繁扩容，增加 GC 的压力 */
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                ePDC.removeAllBuffs();
                PDCAPI.setEntityCalculablePDC(p, ePDC);
                TwItemManager.getBuffManager().deactivatePlayerTimerBuff(p.getUniqueId());
            }, 1L);
        }
    }

    @EventHandler
    public void onPlayerRespawn(@NotNull PlayerRespawnEvent event) {
        Player p =event.getPlayer();
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> TwItemManager.getBuffManager().updateHoldBuffs(p, null), 1L);
    }

    @EventHandler
    public void onPlayerQuit(@NotNull PlayerQuitEvent event) {
        Player p =event.getPlayer();
        TwItemManager.getBuffManager().onPlayerQuit(p.getUniqueId());
        /* 生成一个新的 EntityPDC，防止代码变动，序列化出问题 */
        PDCAPI.setEntityCalculablePDC(p, new EntityPDC());
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> TwItemManager.getBuffManager().deactivatePlayerTimerBuff(p.getUniqueId()), 1L);
    }

    @EventHandler
    public void OnPlayerJoin(@NotNull PlayerJoinEvent event) {
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> TwItemManager.getBuffManager().updateHoldBuffs(event.getPlayer(), null), 1L);
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
        changeBuff(player, previousItem);
    }

    /**
     * 物品栏点击事件
     * @param event 事件
     */
    @EventHandler
    public void onInventoryClick(@NotNull InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player p)) return;
        /* 不特殊判断了, 全部更新 */
        if (event.getClickedInventory() instanceof PlayerInventory) {
            if (TwItemManager.getItemManager().isNotTwItem(event.getCursor()) && TwItemManager.getItemManager().isNotTwItem(event.getCurrentItem())) return;
            /* current是被替换的*/
            ItemStack pre = event.getCurrentItem();
            /* 解决引用的问题 */
            if (pre == null) changeBuff(p, null);
            else changeBuff(p, pre.clone());
        }
    }

    /**
     * 玩家捡起物品事件
     * @param event 事件
     */
    @EventHandler
    public void onItemPickup(@NotNull EntityPickupItemEvent event) {
        ItemStack item = event.getItem().getItemStack();
        // 更新buff
        if (!TwItemManager.getItemManager().isNotTwItem(item) && isValidItem(item)) changeBuff(event.getEntity(), null);
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
        if (!TwItemManager.getItemManager().isNotTwItem(item) && isValidItem(item)) changeBuff(p, item);
    }

    /**
     * 玩家物品的 HoldBuff 生效
     */
    private void changeBuff(@NotNull LivingEntity e, ItemStack pre) {
        if (Config.debug) logInfo(e.getName() + ": buff updated");
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> TwItemManager.getBuffManager().updateHoldBuffs(e, pre), 1L);
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
