package io.github.tanice.twItemManager.manager.pdc.impl;

import io.github.tanice.twItemManager.manager.pdc.CalculablePDC;
import io.github.tanice.twItemManager.manager.pdc.type.AttributeAdditionFromType;
import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serial;
import java.util.Arrays;

import static io.github.tanice.twItemManager.util.Logger.logWarning;
import static io.github.tanice.twItemManager.util.MiniMessageUtil.stripAllTags;

/**
 * 单纯的某一个属性（基础属性）
 */
@Getter
public class AttributePDC extends CalculablePDC {
    @Serial
    private static final long serialVersionUID = 1L;

    private String displayName;

    public AttributePDC(){
        super();
    }

    public AttributePDC(@NotNull String displayName, @NotNull AttributeAdditionFromType aft) {
        super(stripAllTags(displayName), aft);
        this.displayName = displayName;
    }

    public AttributePDC(@NotNull String displayName, @NotNull AttributeAdditionFromType aft, @Nullable ConfigurationSection cfg) {
        super(stripAllTags(displayName), aft, cfg);
        this.displayName = displayName;
    }

    @Override
    public void selfCalculate() {
        logWarning("基础属性不需要自计算，请检查代码");
    }

    @Override
    public void merge(CalculablePDC... o) {
        logWarning("基础属性不可能合并，请检查代码");
    }

    @Override
    public String toString() {
        return "CalculablePDC{" +
                "fromType=" + fromType + "," +
                "itemInnerName=" + innerName + "," +
                "damage=" + Arrays.toString(damage) + "," +
                "criticalStrikeChance=" + criticalStrikeChance + "," +
                "criticalStrikeDamage=" + criticalStrikeDamage + "," +
                "armor=" + armor + "," +
                "armorToughness=" + armorToughness + "," +
                "preArmorReduction=" + preArmorReduction + "," +
                "afterArmorReduction=" + afterArmorReduction +
                "manaCost=" + Arrays.toString(manaCost) + ", " +
                "skillCoolDown=" + Arrays.toString(skillCoolDown) +
                "}";
    }
}
