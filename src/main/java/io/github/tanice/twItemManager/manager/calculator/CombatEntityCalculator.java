package io.github.tanice.twItemManager.manager.calculator;

import io.github.tanice.twItemManager.manager.pdc.CalculablePDC;
import io.github.tanice.twItemManager.manager.pdc.type.AttributeCalculateSection;
import io.github.tanice.twItemManager.manager.pdc.type.AttributeType;
import lombok.Getter;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;

import static io.github.tanice.twItemManager.util.Logger.logWarning;
import static io.github.tanice.twItemManager.util.Tool.enumMapToString;

@Getter
public class CombatEntityCalculator extends Calculator {
    /**
     * 是否使用 玩家减伤 的平衡计算方法
     */
    private boolean useDamageReductionBalance = false;

    public CombatEntityCalculator() {
        super(null);
    }

    public CombatEntityCalculator(@Nullable Entity entity) {
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
                for (AttributeCalculateSection section : AttributeCalculateSection.values()) {
                    if (section == AttributeCalculateSection.BASE) continue;
                    pdc = resultMap.get(section);
                    if (pdc == null) continue;
                    Double value = pdc.getVMap().get(attrType);
                    // 攻击力根据不同的section乘算
                    // if (attrType == AttributeType.DAMAGE) result *= (1 + value);
                    // 其余的都是加算
                    // else result += value;
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
