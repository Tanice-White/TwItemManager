package io.github.tanice.twItemManager.manager.item;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * ItemManager的方法抽象
 */
public interface Manager {
    /**
     * 生成Item类物品
     */
    ItemStack generateItem(@NotNull String innerName);

    /**
     * 更新物品悬停展示界面（包含描述、等级和品质部分）
     */
    void updateItem(@NotNull ItemStack item);

    /**
     * 生成宝石类物品
     */
    ItemStack generateGemItem(@NotNull String innerName);

    /**
     * 物品品质重铸
     */
    boolean recast(@NotNull ItemStack item);

    /**
     * 物品升级
     */
    boolean levelUp(Player player, ItemStack item, ItemStack levelUpNeed, boolean check);

    /**
     * 物品降级-主动（仅指令可用）
     */
    void levelDown(Player player, ItemStack item);
}
