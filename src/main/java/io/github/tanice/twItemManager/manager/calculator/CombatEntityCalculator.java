package io.github.tanice.twItemManager.manager.calculator;

import io.github.tanice.twItemManager.manager.pdc.CalculablePDC;
import io.github.tanice.twItemManager.manager.pdc.impl.AttributePDC;
import io.github.tanice.twItemManager.manager.pdc.type.AttributeCalculateSection;
import io.github.tanice.twItemManager.manager.pdc.type.DamageFromType;
import io.github.tanice.twItemManager.manager.pdc.type.DamageType;
import lombok.Getter;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.List;

@Getter
public class CombatEntityCalculator extends Calculator {
    /**
     * 是否使用 玩家减伤 的平衡计算方法
     */
    private boolean useDamageReductionBalance;

    public CombatEntityCalculator(@NotNull LivingEntity entity) {
        super(entity);
    }

    @Override
    public DamageType getCombatValue() {
        // 获取最高加成，作为伤害来源
        EnumMap<DamageType, Double> tMap = new EnumMap<>(DamageType.class);
        double base = 0, add = 0, mul = 0, fin = 0, critical_chance = 0, critical_damage = 0, fix = 0;
        CalculablePDC cPDC;
        for (AttributeCalculateSection acs : resulMap.keySet()) {
            /* 排除 */
            if (acs == AttributeCalculateSection.BEFORE_DAMAGE || acs == AttributeCalculateSection.AFTER_DAMAGE || acs == AttributeCalculateSection.OTHER) continue;
            /* 数值整合计算 */
            cPDC = resulMap.get(acs);
            /* 1.属性统计(逐个CalculatePDC统计) */
        }

        return DamageType.OTHER;
    }

    // TODO 针对某一类特定的伤害类型做出减免
    @Override
    public double calculateFinalDamage(double CombatValue, @Nullable DamageFromType damageFromType) {
        return 0;
    }

    /**
     * 减伤平衡算法
     * @param oriDr 理论伤害减免比例
     * @return 平衡后的伤害减免比例
     */
    private double drBalance(double oriDr) {
        if (useDamageReductionBalance) return oriDr / (1 + oriDr);
        return oriDr;
    }
}
