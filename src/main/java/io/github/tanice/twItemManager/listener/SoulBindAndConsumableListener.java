package io.github.tanice.twItemManager.listener;

import io.github.tanice.twItemManager.TwItemManager;
import io.github.tanice.twItemManager.infrastructure.PDCAPI;
import io.github.tanice.twItemManager.manager.item.base.BaseItem;
import io.github.tanice.twItemManager.manager.item.base.impl.Consumable;
import io.github.tanice.twItemManager.manager.item.base.impl.Item;
import io.github.tanice.twItemManager.manager.pdc.EntityPDC;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 处理灵魂绑定和可食用物品
 */
public class SoulBindAndConsumableListener implements Listener {
    private final JavaPlugin plugin;

    public SoulBindAndConsumableListener(JavaPlugin plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
        plugin.getLogger().info("TwItemListener loaded");
    }

    /**
     * 第一获取者默认为 owner
     * TODO 测试用，预计使用释放技能或者别的触发方式时绑定
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerHeldSoulBind(@NotNull PlayerItemHeldEvent event) {
        ItemStack item = event.getPlayer().getInventory().getItem(event.getNewSlot());
        if (item == null) return;
        if(isTwItem(item) && isSoulBind(item)) {
            TwItemManager.getItemManager().doSoulBind(item, event.getPlayer());
            TwItemManager.getItemManager().updateItemDisplayView(item);
        }
    }

    /**
     * 只要有绑定就不能丢弃，即使不是你的
     */
    @EventHandler
    public void onPlayerDrop(@NotNull PlayerDropItemEvent event) {
        ItemStack i = event.getItemDrop().getItemStack();
        if (isTwItem(i) && isSoulBind(i)) event.setCancelled(true);
    }

    /**
     * 可食用物品监听
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void PlayerInteractConsumable(@NotNull PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        ItemStack item = event.getItem();
        if (item == null) return;

        BaseItem bit = TwItemManager.getItemManager().getBaseItem(item);
        if (!(bit instanceof Consumable consumable)) return;

        EntityPDC ePDC = PDCAPI.getEntityPDC(event.getPlayer());
        if (ePDC == null) ePDC = new EntityPDC();

        long ct = System.currentTimeMillis();
        if (ePDC.canConsume(consumable, ct) && consumable.activate(event.getPlayer())) {
            ePDC.consume(consumable, ct);
            item.setAmount(item.getAmount() - 1);
            PDCAPI.setCalculablePDC(event.getPlayer(), ePDC);
            // 播放音效
            consumable.playSound(event.getPlayer());
            return;
        }
        PDCAPI.setCalculablePDC(event.getPlayer(), ePDC);
        event.getPlayer().sendMessage("§c有股神秘的力量在阻止你吃下它");
        event.setCancelled(true);
    }

    private boolean isTwItem(@Nullable ItemStack i) {
        return !TwItemManager.getItemManager().isNotItemClassInTwItem(i);
    }

    private boolean isSoulBind(ItemStack i) {
        BaseItem bit = TwItemManager.getItemManager().getBaseItem(i);
        if (bit instanceof Item it) return it.isSoulBind();
        return false;
    }
}
