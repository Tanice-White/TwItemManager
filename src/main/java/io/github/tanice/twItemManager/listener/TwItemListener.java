package io.github.tanice.twItemManager.listener;

import io.github.tanice.twItemManager.TwItemManager;
import io.github.tanice.twItemManager.config.Config;
import io.github.tanice.twItemManager.manager.item.base.impl.Item;
import io.github.tanice.twItemManager.manager.pdc.impl.BuffPDC;
import io.github.tanice.twItemManager.manager.pdc.impl.EntityPDC;
import io.github.tanice.twItemManager.manager.pdc.type.AttributeCalculateSection;
import io.papermc.paper.persistence.PersistentDataContainerView;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.inventory.PrepareGrindstoneEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemBreakEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static io.github.tanice.twItemManager.infrastructure.PDCAPI.getEntityCalculablePDC;
import static io.github.tanice.twItemManager.infrastructure.PDCAPI.setEntityCalculablePDC;
import static io.github.tanice.twItemManager.util.Logger.logWarning;

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

    /**
     * 第一获取者默认为 owner
     * TODO 测试用，预计使用释放技能或者别的触发方式时绑定
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerHeldSoulBind(@NotNull PlayerItemHeldEvent event) {
        ItemStack i = event.getPlayer().getInventory().getItem(event.getNewSlot());
        if (i == null) return;
        PersistentDataContainerView c = i.getPersistentDataContainer();
        NamespacedKey k2= new NamespacedKey(plugin, "owner");

        if(isTwItem(i) && isSoulBind(i) && c.get(k2, PersistentDataType.STRING).isEmpty()) {
            i.editMeta(meta -> meta.getPersistentDataContainer().set(k2, PersistentDataType.STRING, event.getPlayer().getName()));
        }

        // TODO 触发自定义事件 - > 更新lore
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
     * 禁止在工作台中 “合成” 原版物品
     */
    // @EventHandler(priority = EventPriority.LOWEST)
    public void onPrepareCraft(@NotNull PrepareItemCraftEvent event) {
        Recipe recipe = event.getRecipe();
        if (recipe == null) return;
        ItemStack result = recipe.getResult();
        // 如果结果是原版物品 则取消
        if (!isTwItem(result)) event.getInventory().setResult(null); // 清空合成结果
    }

    private boolean isTwItem(@Nullable ItemStack i) {
        return !TwItemManager.getItemManager().isNotTwItem(i);
    }

    private boolean isSoulBind(ItemStack i) {
        return TwItemManager.getItemManager().getItemByItemStack(i).isSoulBind();
    }

    private boolean isCancelDamage(ItemStack i) {
        return TwItemManager.getItemManager().getItemByItemStack(i).isCancelDamage();
    }

    private boolean isLoseWhenBreak(ItemStack i) {
        return TwItemManager.getItemManager().getItemByItemStack(i).isLoseWhenBreak();
    }
}
