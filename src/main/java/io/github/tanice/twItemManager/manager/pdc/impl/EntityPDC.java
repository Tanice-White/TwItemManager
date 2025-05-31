package io.github.tanice.twItemManager.manager.pdc.impl;

import io.github.tanice.twItemManager.manager.pdc.CalculablePDC;
import io.github.tanice.twItemManager.manager.pdc.type.AttributeAdditionFromType;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.io.Serial;
import java.util.Arrays;
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
        return "CalculablePDC{" +
                "BuffNames=" + buffName + ", " +
                "fromType=" + fromType + ", " +
                "itemInnerName=" + innerName + ", " +
                "damage=[" + Arrays.toString(damage) + "], " +
                "criticalStrikeChance=" + criticalStrikeChance + ", " +
                "criticalStrikeDamage=" + criticalStrikeDamage + ", " +
                "armor=" + armor + ", " +
                "armorToughness=" + armorToughness + ", " +
                "preArmorReduction=" + preArmorReduction + ", " +
                "afterArmorReduction=" + afterArmorReduction +
                "manaCost=" + Arrays.toString(manaCost) + ", " +
                "skillCoolDown=" + Arrays.toString(skillCoolDown) +
                "}";
    }
}
