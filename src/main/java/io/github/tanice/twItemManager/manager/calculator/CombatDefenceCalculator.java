package io.github.tanice.twItemManager.manager.calculator;

import io.github.tanice.twItemManager.manager.item.ItemManager;
import lombok.Getter;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@Getter
public class CombatDefenceCalculator extends Calculator {
    /**
     * 防御值 更据转变系数转变为防御减伤
     */
    private double armor;

    /**
     * 韧性
     */
    private double armorToughness;

    /**
     * 防御减伤计算前的减伤
     */
    private List<Double> preArmorReduction;
    /**
     * 防御计算之后的减伤
     */
    private List<Double> afterArmorReduction;

    /**
     * 是否使用 玩家减伤 的平衡计算方法
     */
    private boolean damageReductionBalance;

    public CombatDefenceCalculator(@NotNull ItemManager im, @NotNull LivingEntity entity) {
        super(im, entity);
    }

    @Override
    public double getFinalV(@NotNull LivingEntity entity) {
        return 0;
    }

    /**
     * 减伤平衡算法
     * @param oriDR 理论伤害减免比例
     * @return 平衡后的伤害减免比例
     */
    private double drBalance(double oriDR) {
        return oriDR / (1 + oriDR);
    }
}
