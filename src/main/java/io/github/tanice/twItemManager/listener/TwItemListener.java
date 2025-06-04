package io.github.tanice.twItemManager.listener;

import io.github.tanice.twItemManager.TwItemManager;
import io.github.tanice.twItemManager.config.Config;
import io.github.tanice.twItemManager.manager.calculator.CombatEntityCalculator;
import io.github.tanice.twItemManager.manager.pdc.CalculablePDC;
import io.github.tanice.twItemManager.manager.pdc.impl.BuffPDC;
import io.github.tanice.twItemManager.manager.pdc.type.AttributeCalculateSection;
import io.github.tanice.twItemManager.util.ReflectUtil;
import io.papermc.paper.persistence.PersistentDataContainerView;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
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

import static io.github.tanice.twItemManager.util.Logger.logWarning;

/**
 * 所有和本插件生成的物品事件
 */
public class TwItemListener implements Listener {
    private final JavaPlugin plugin;
    // 待检查玩家列表
    private final List<Player> playerList = new ArrayList<>();

    public TwItemListener(JavaPlugin plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
        plugin.getLogger().info("TwItemListener loaded");
    }

    // 灵魂绑定事件
    /**
     * 第一获取者默认为 owner
     * TODO 测试用，预计使用释放技能或者别的触发方式时绑定
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerHeldSoulBind(PlayerItemHeldEvent event) {
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
    public void onPlayerDrop(PlayerDropItemEvent event) {
        ItemStack i = event.getItemDrop().getItemStack();
        if (isTwItem(i) && isSoulBind(i)) event.setCancelled(true);
    }

    /**
     * 物品损坏事件
     */
    @EventHandler
    public void onItemBreak(PlayerItemBreakEvent event) {
        ItemStack i = event.getBrokenItem();
        if (isTwItem(i) && isLoseWhenBreak(i)) {
            // TODO 限制物品使用, 禁用属性
            // plugin.getLogger().warning(event.getBrokenItem().getItemMeta().displayName() + " broken");
        }
    }

    /**
     * 禁止铁砧修复
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        if (Config.anvilRepairable) return;
        ItemStack fi = event.getInventory().getFirstItem();
        ItemStack si = event.getInventory().getSecondItem();
        if (fi == null || si == null) return;
        if ((si.getType() == si.getType()) && (isTwItem(fi) || isTwItem(si))) {
            event.setResult(null);
        }
    }

    /**
     * 禁止砂轮修复
     */
    @EventHandler
    public void onPrepareGrindstone(PrepareGrindstoneEvent event) {
        if (Config.grindstoneRepairable) return;
        ItemStack fi = event.getInventory().getItem(0);
        ItemStack si = event.getInventory().getItem(1);
        // 两个都是才能修复
        if (isTwItem(fi) && isTwItem(si)) event.setResult(null);
    }

    /**
     * 禁止附魔台附魔
     */
    @EventHandler
    public void onEnchant(EnchantItemEvent event) {
        if (Config.canEnchant) return;
        ItemStack item = event.getItem();
        if (isTwItem(item)) event.setCancelled(true);
    }

    /**
     * 禁止铁砧砸经验修补
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPrepareAnvilAddMending(PrepareAnvilEvent event) {
        if (Config.canEnchant) return;
        ItemStack fi = event.getInventory().getFirstItem();
        ItemStack si = event.getInventory().getSecondItem();
        if (fi == null || si == null) return;

        // 检查第一个物品是否有经验修补附魔 (防绕过)
        if ((isMendingBook(fi) && isTwItem(si)) || (isMendingBook(si) && isTwItem(fi))) {
            event.setResult(null);
        }
    }

    /**
     * 禁止在工作台中“合成” 原版物品
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPrepareCraft(PrepareItemCraftEvent event) {
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
        return TwItemManager.getItemManager().getItem(i).isSoulBind();
    }

    private boolean isCancelDamage(ItemStack i) {
        return TwItemManager.getItemManager().getItem(i).isCancelDamage();
    }

    private boolean isLoseWhenBreak(ItemStack i) {
        return TwItemManager.getItemManager().getItem(i).isLoseWhenBreak();
    }

    private boolean isMendingBook(@NotNull ItemStack i) {
        return i.getType() == Material.ENCHANTED_BOOK && ((EnchantmentStorageMeta)i.getItemMeta()).hasStoredEnchant(Enchantment.MENDING);
    }
}
