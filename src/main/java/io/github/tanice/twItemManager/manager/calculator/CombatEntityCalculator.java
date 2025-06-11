package io.github.tanice.twItemManager.manager.calculator;

import io.github.tanice.twItemManager.manager.pdc.CalculablePDC;
import io.github.tanice.twItemManager.manager.pdc.type.AttributeCalculateSection;
import io.github.tanice.twItemManager.manager.pdc.type.AttributeType;
import lombok.Getter;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;

import java.util.EnumMap;

@Getter
public class CombatEntityCalculator extends Calculator {
    /**
     * 是否使用 玩家减伤 的平衡计算方法
     */
    private boolean useDamageReductionBalance = false;

    public CombatEntityCalculator() {
        super(null);
    }

    public CombatEntityCalculator(@NotNull Entity entity) {
        super(entity);
    }

    @Override
    public EnumMap<AttributeType, Double> getAttrsValue() {
        CalculablePDC pdc;
        EnumMap<AttributeType, Double> res = new EnumMap<>(AttributeType.class);
        Double result = null;

        for (AttributeType attrType : AttributeType.values()) {
            pdc = resultMap.get(AttributeCalculateSection.BASE);
            if (pdc != null) result = pdc.getVMap().get(attrType);
            if (result == null) result = 0D;
            if (result != 0D){
                /* 计算顺序：BASE * ADD * MULTIPLY * FIX */
                pdc = resultMap.get(AttributeCalculateSection.ADD);
                if (pdc != null) {
                    Double value = pdc.getVMap().get(attrType);
                    result *= (1 + value);
                }
                pdc = resultMap.get(AttributeCalculateSection.MULTIPLY);
                if (pdc != null) {
                    Double value = pdc.getVMap().get(attrType);
                    result *= (1 + value);
                }
                pdc = resultMap.get(AttributeCalculateSection.FIX);
                if (pdc != null) {
                    Double value = pdc.getVMap().get(attrType);
                    result *= (1 + value);
                }
            }
            res.put(attrType, result);
        }

        res.put(AttributeType.PRE_ARMOR_REDUCTION, drBalance(res.get(AttributeType.PRE_ARMOR_REDUCTION)));
        res.put(AttributeType.AFTER_ARMOR_REDUCTION, drBalance(res.get(AttributeType.AFTER_ARMOR_REDUCTION)));

        return res;
    }

    /**
     * 减伤平衡算法
     * @param oriDr 理论伤害减免比例
     * @return 平衡后的伤害减免比例
     */
    public double drBalance(Double oriDr) {
        if (oriDr == null) return 0D;
        if (useDamageReductionBalance) return oriDr / (1 + oriDr);
        return oriDr;
    }
}
