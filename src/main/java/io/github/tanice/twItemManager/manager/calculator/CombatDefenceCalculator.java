package io.github.tanice.twItemManager.manager.calculator;

import lombok.Getter;
import org.bukkit.entity.Entity;

@Getter
public class CombatDefenceCalculator {
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
    private double preArmorReduction;
    /**
     * 防御计算之后的减伤
     */
    private double afterArmorReduction;

    /**
     * 是否使用 玩家减伤 的平衡计算方法
     */
    private boolean damageReductionBalance;

    public CombatDefenceCalculator(Entity entity) {
        // 遍历装备
        // 遍历BUFF
        // 遍历饰品
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
