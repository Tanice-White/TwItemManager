package io.github.tanice.twItemManager.manager.calculator;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public abstract class Calculator {
    /**
     * 遍历物体生效槽位返回物品列表
     * 返回6个实体 [主手 副手 鞋子 裤子 胸甲 头盔]
     */
    protected @NotNull List<ItemStack> getEffectiveEquipment(@NotNull Entity entity) {
        if (!(entity instanceof LivingEntity)) return new ArrayList<>();
        EntityEquipment equip = ((LivingEntity) entity).getEquipment();
        if (equip == null) return new ArrayList<>();

        List<ItemStack> res = new ArrayList<>();
        res.add(equip.getItemInMainHand());
        res.add(equip.getItemInOffHand());
        res.addAll(List.of(equip.getArmorContents()));
        return res;
    }

    /**
     * 获取目标生效的buff
     */
    protected @NotNull List<ItemStack> getEffectiveBuff(@NotNull Entity entity) {
        return new ArrayList<>();
    }

    /**
     * 遍历目标的饰品
     */
    protected @NotNull List<ItemStack> getEffectiveAccessory(@NotNull Entity entity) {
        return new ArrayList<>();
    }

    /**
     * 获取计算器最终的计算结果
     */
    public abstract double getFinalV(@NotNull Entity entity, double percentage);
}
