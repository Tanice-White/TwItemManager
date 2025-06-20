package io.github.tanice.twItemManager.listener;

import io.github.tanice.twItemManager.TwItemManager;
import io.github.tanice.twItemManager.infrastructure.PDCAPI;
import io.github.tanice.twItemManager.manager.item.base.BaseItem;
import io.github.tanice.twItemManager.manager.item.base.impl.Item;
import io.papermc.paper.persistence.PersistentDataContainerView;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 所有和本插件生成的物品事件
 */
public class TwItemListener implements Listener {
    private final JavaPlugin plugin;

    public TwItemListener(JavaPlugin plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
        plugin.getLogger().info("TwItemListener loaded");
    }

    public void onReload() {
        plugin.getLogger().info("TwItemListener reloaded");
    }

    /**
     * 第一获取者默认为 owner
     * TODO 测试用，预计使用释放技能或者别的触发方式时绑定
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerHeldSoulBind(@NotNull PlayerItemHeldEvent event) {
        ItemStack item = event.getPlayer().getInventory().getItem(event.getNewSlot());
        if (item == null) return;
        if(isTwItem(item) && isSoulBind(item)) TwItemManager.getItemManager().doSoulBind(item, event.getPlayer());
        TwItemManager.getItemManager().updateItemDisplayView(item);
    }

    /**
     * 只要有绑定就不能丢弃，即使不是你的
     */
    @EventHandler
    public void onPlayerDrop(@NotNull PlayerDropItemEvent event) {
        ItemStack i = event.getItemDrop().getItemStack();
        if (isTwItem(i) && isSoulBind(i)) event.setCancelled(true);
    }

    private boolean isTwItem(@Nullable ItemStack i) {
        return !TwItemManager.getItemManager().isNotItem(i);
    }

    private boolean isSoulBind(ItemStack i) {
        BaseItem bit = TwItemManager.getItemManager().getBaseItem(i);
        if (bit instanceof Item it) return it.isSoulBind();
        return false;
    }
}
