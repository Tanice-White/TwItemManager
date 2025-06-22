package io.github.tanice.twItemManager.listener;

import io.github.tanice.twItemManager.TwItemManager;
import io.github.tanice.twItemManager.config.Config;
import io.github.tanice.twItemManager.infrastructure.PDCAPI;
import io.github.tanice.twItemManager.manager.pdc.CalculablePDC;
import io.github.tanice.twItemManager.manager.pdc.impl.EntityPDC;
import io.papermc.paper.event.player.PlayerInventorySlotChangeEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
        EntityPDC ePDC = PDCAPI.getCalculablePDC(p);
        if (ePDC != null) {
            /* 若直接 new 可能会频繁扩容，增加 GC 的压力 */
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                ePDC.removeBuffs();
                PDCAPI.setCalculablePDC(p, ePDC);
                TwItemManager.getBuffManager().deactivatePlayerTimerBuff(p.getUniqueId());
            }, 1L);
        }
    }

    @EventHandler
    public void onPlayerRespawn(@NotNull PlayerRespawnEvent event) {
        Player p =event.getPlayer();
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> TwItemManager.getBuffManager().updateHoldBuffs(p, (ItemStack) null), 1L);
    }

    @EventHandler
    public void onPlayerQuit(@NotNull PlayerQuitEvent event) {
        Player p =event.getPlayer();
        TwItemManager.getBuffManager().onPlayerQuit(p);
        TwItemManager.getDatabaseManager().saveEntityPDC(p.getUniqueId().toString(), PDCAPI.getCalculablePDC(p));

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> TwItemManager.getBuffManager().deactivatePlayerTimerBuff(p.getUniqueId()), 1L);
    }

    @EventHandler
    public void onPlayerJoin(@NotNull PlayerJoinEvent event) {
        Player p =event.getPlayer();
        // TODO 不同版本需要覆盖，否则不变
        EntityPDC ePDC = TwItemManager.getDatabaseManager().getEntityPDC(p.getUniqueId().toString());
        if (ePDC != null) {
            // 内部的 endTimeStamp 需要更新
            if (!ePDC.getBuffPDCs().isEmpty()) {
                long curr = System.currentTimeMillis() + 100L;  // 手动延迟2tick
                for (CalculablePDC bPDC : ePDC.getBuffPDCs()) bPDC.setEndTimeStamp(bPDC.getDeltaTime() + curr);
            }

        } else ePDC = new EntityPDC();

        if (!PDCAPI.setCalculablePDC(p, ePDC)) logWarning("设置玩家PDC 失败");

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            TwItemManager.getBuffManager().updateHoldBuffs(event.getPlayer(), (ItemStack) null);
            TwItemManager.getBuffManager().onPlayerJoin(p);
        }, 10L);
    }

    /**
     * 背包的 副手、护甲物品变化
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryChange(@NotNull PlayerInventorySlotChangeEvent event) {
        int slot = event.getSlot();
        if (!isArmorSlot(slot) && !isHandSlot(slot)) return;
        /* 原本位置的物品是否是twItem */
        if (TwItemManager.getItemManager().isNotItem(event.getOldItemStack()) && TwItemManager.getItemManager().isNotItem(event.getNewItemStack())) return;
        /* 这里获取的物品，延迟执行不会变化 */
        /* 考虑到主副手交换，两个全都删除 */
        changeBuff(event.getPlayer(), event.getOldItemStack(), event.getNewItemStack());
    }

    /**
     * 主手手持变化
     */
    @EventHandler
    public void onItemHeld(@NotNull PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        ItemStack previousItem = player.getInventory().getItem(event.getPreviousSlot());
        ItemStack newItem = player.getInventory().getItem(event.getNewSlot());

        /* 原物品是插件物品则需要 */
        if (TwItemManager.getItemManager().isNotItem(previousItem) &&  TwItemManager.getItemManager().isNotItem(newItem)) return;
        changeBuff(player, previousItem, newItem);
    }

    /**
     * 玩家丢弃物品事件
     * @param event 事件
     */
    @EventHandler(ignoreCancelled = true)
    public void onItemDrop(@NotNull PlayerDropItemEvent event) {
        Player p = event.getPlayer();
        ItemStack item = event.getItemDrop().getItemStack();
        // 更新buff
        if (!TwItemManager.getItemManager().isNotItem(item)) changeBuff(p, item);
    }

    /**
     * 玩家捡起物品事件
     * @param event 事件
     */
    @EventHandler
    public void onItemPickup(@NotNull EntityPickupItemEvent event) {
        ItemStack item = event.getItem().getItemStack();
        // 更新buff
        if (!TwItemManager.getItemManager().isNotItem(item)) changeBuff(event.getEntity(), (ItemStack) null);
    }

    /**
     * 玩家物品的 HoldBuff 生效
     */
    private void changeBuff(@NotNull LivingEntity e, ItemStack @Nullable ... pre) {
        if (Config.debug) logInfo(e.getName() + ": buff updated");
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> TwItemManager.getBuffManager().updateHoldBuffs(e, pre), 1L);
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
