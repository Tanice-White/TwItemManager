package io.github.tanice.twItemManager.manager.pdc.impl;

import io.github.tanice.twItemManager.manager.pdc.CalculablePDC;
import io.github.tanice.twItemManager.manager.pdc.type.AttributeAdditionFromType;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.io.Serial;
import java.util.List;

import static io.github.tanice.twItemManager.util.Logger.logWarning;

/**
 * 实体持有的属性
 */
@Getter
public class EntityPDC extends CalculablePDC {
    @Serial
    private static final long serialVersionUID = 1L;

    /* 影响属性的值 */
    private List<String> buffName;

    public EntityPDC(){
        super();
    }

    public EntityPDC(@NotNull String innerName, @NotNull AttributeAdditionFromType aft) {
        super(innerName, aft);
    }

    @Override
    public void selfCalculate() {
        // TODO 从buffManager中找到对应的AttributePDC并进行自计算
        return;
    }

    @Override
    public void merge(CalculablePDC... o) {
        logWarning("一个实体的BUFF不可能合并，请检查代码");
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("CalculablePDC{");
        sb.append("BuffNames=[");
        boolean f = true;
        for(String buffName : buffName) {
            if (!f) sb.append(",");
            sb.append(buffName);
            f = false;
        }
        sb.append("],");
        sb.append("fromType=").append(fromType).append(",");
        sb.append("itemInnerName=").append(innerName).append(",");
        sb.append("damage=[").append(damage[0]).append(damage[1]).append(damage[2]).append("],");
        sb.append("criticalStrikeChance=").append(criticalStrikeChance).append(",");
        sb.append("criticalStrikeDamage=").append(criticalStrikeDamage).append(",");
        sb.append("armor=").append(armor).append(",");
        sb.append("armorToughness=").append(armorToughness).append(",");
        sb.append("preArmorReduction=").append(preArmorReduction).append(",");
        sb.append("afterArmorReduction=").append(afterArmorReduction);
        sb.append("}");
        return sb.toString();
    }
}
