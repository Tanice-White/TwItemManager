package io.github.tanice.twItemManager.listener;

import io.github.tanice.twItemManager.TwItemManager;
import io.github.tanice.twItemManager.config.Config;
import io.github.tanice.twItemManager.infrastructure.PDCAPI;
import io.github.tanice.twItemManager.manager.pdc.impl.BuffPDC;
import io.github.tanice.twItemManager.manager.pdc.EntityPDC;
import io.papermc.paper.event.player.PlayerInventorySlotChangeEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

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

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDie (@NotNull PlayerDeathEvent event) {
        Player p =event.getPlayer();
        EntityPDC ePDC = PDCAPI.getEntityPDC(p);
        if (ePDC != null) {
            /* 若直接 new 可能会频繁扩容，增加 GC 的压力 */
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                ePDC.removeBuffs();
                PDCAPI.setCalculablePDC(p, ePDC);
                TwItemManager.getBuffManager().deactivatePlayerTimerBuff(p.getUniqueId());
            }, 1L);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerRespawn(@NotNull PlayerRespawnEvent event) {
        Player p =event.getPlayer();
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> TwItemManager.getBuffManager().updateHoldBuffs(p), 1L);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerQuit(@NotNull PlayerQuitEvent event) {
        if (!Config.use_mysql) return;
        Player p =event.getPlayer();
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            TwItemManager.getBuffManager().onPlayerQuit(p);
            TwItemManager.getDatabaseManager().saveEntityPDC(p.getUniqueId().toString(), PDCAPI.getEntityPDC(p));
        }, 1L);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(@NotNull PlayerJoinEvent event) {
        if (!Config.use_mysql) return;
        Player p = event.getPlayer();
        TwItemManager.getDatabaseManager().loadEntityPDC(p.getUniqueId().toString())
            .thenAccept(ePDC -> {
                EntityPDC usePDC = (ePDC == null) ? new EntityPDC() : ePDC;
                // 内部的 endTimeStamp 需要更新
                if (!usePDC.getBuffPDCs().isEmpty()) {
                    long curr = System.currentTimeMillis() + 99L;  // 手动延迟1tick
                    for (BuffPDC bPDC : usePDC.getBuffPDCs()) {
                        bPDC.setEndTimeStamp(bPDC.getDeltaTime() + curr);
                    }
                }
                // 切回主线程
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    if (!PDCAPI.setCalculablePDC(p, usePDC)) logWarning("设置玩家PDC 失败");

                    plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                        TwItemManager.getBuffManager().updateHoldBuffs(event.getPlayer());
                        TwItemManager.getBuffManager().onPlayerJoin(p);
                    }, 1L);
                });
            });
    }

    /**
     * 背包的 副手、护甲物品变化
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryChange(@NotNull PlayerInventorySlotChangeEvent event) {
        int slot = event.getSlot();
        if (!isArmorSlot(slot) && !isHandSlot(slot)) return;
        /* 原本位置的物品是否是twItem */
        if (TwItemManager.getItemManager().isNotItem(event.getOldItemStack()) && TwItemManager.getItemManager().isNotItem(event.getNewItemStack())) return;
        /* 这里获取的物品，延迟执行不会变化 */
        /* 考虑到主副手交换，两个全都删除 */
        changeBuff(event.getPlayer());
    }

    /**
     * 处理shift点击事件
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(@NotNull InventoryClickEvent event) {
        /* shift 放入发射器, 得到的是发射器而不是Player */
        if (!(event.getInventory().getHolder() instanceof Player player)) return;
        /* event 的 getCurrentItem() 太抽象了, 直接全部更新得了 */
        if (TwItemManager.getItemManager().isNotItem(event.getCurrentItem()) && TwItemManager.getItemManager().isNotItem(event.getCursor())) return;
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> changeBuff(player), 1L);
    }

    /**
     * 主手手持变化
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onItemHeld(@NotNull PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        ItemStack previousItem = player.getInventory().getItem(event.getPreviousSlot());
        ItemStack newItem = player.getInventory().getItem(event.getNewSlot());

        /* 原物品是插件物品则需要 */
        if (TwItemManager.getItemManager().isNotItem(previousItem) &&  TwItemManager.getItemManager().isNotItem(newItem)) return;
        changeBuff(player);
    }

    /**
     * 玩家丢弃物品事件
     * @param event 事件
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onItemDrop(@NotNull PlayerDropItemEvent event) {
        Player p = event.getPlayer();
        ItemStack item = event.getItemDrop().getItemStack();
        // 更新buff
        if (!TwItemManager.getItemManager().isNotItem(item)) changeBuff(p);
    }

    /**
     * 玩家捡起物品事件
     * @param event 事件
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onItemPickup(@NotNull EntityPickupItemEvent event) {
        ItemStack item = event.getItem().getItemStack();
        // 更新buff
        if (!TwItemManager.getItemManager().isNotItem(item)) changeBuff(event.getEntity());
    }

    /**
     * 玩家物品的 HoldBuff 生效
     */
    private void changeBuff(@NotNull LivingEntity e) {
        if (Config.debug) logInfo(e.getName() + ": buff updated");
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> TwItemManager.getBuffManager().updateHoldBuffs(e), 1L);
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
