package io.github.tanice.twItemManager.manager.item;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * ItemManager的方法抽象
 */
public interface Manager {
    /**
     * 生成Item类物品
     */
    ItemStack generateItem(@NotNull String innerName);

    /**
     * 完善物品悬停展示界面（包含描述、等级和品质部分）
     */
    void completeItem(@NotNull ItemStack item);

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
    boolean levelUp(@NotNull Player player, @NotNull ItemStack item, @Nullable ItemStack levelUpNeed, boolean check);

    /**
     * 物品降级-主动（仅指令可用）
     */
    void levelDown(@NotNull Player player,@NotNull  ItemStack item);

    /**
     * 设置物品等级
     */
    void levelSet(@NotNull Player player, @NotNull ItemStack item, int level);

    /**
     * 宝石镶嵌
     */
    boolean insertGem(Player player, ItemStack item, ItemStack gem);

    /**
     * 宝石随机移除
     */
    ItemStack removeGem(Player player, ItemStack item);

    /**
     * 依照最新的版本更新物品
     * 返回多余的宝石内部名
     */
    List<String> updateItem(@NotNull Player player, @NotNull ItemStack item);
}
