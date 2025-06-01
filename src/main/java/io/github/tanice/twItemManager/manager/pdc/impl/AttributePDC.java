package io.github.tanice.twItemManager.manager.pdc.impl;

import io.github.tanice.twItemManager.manager.pdc.CalculablePDC;
import io.github.tanice.twItemManager.manager.pdc.type.AttributeAdditionFromType;
import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

import java.io.Serial;
import java.util.Arrays;

import static io.github.tanice.twItemManager.util.Logger.logWarning;
import static io.github.tanice.twItemManager.util.MiniMessageUtil.stripAllTags;

/**
 * 单纯的某一个属性（基础属性）
 * 可用于计算
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

    public AttributePDC(@NotNull String displayName, @NotNull AttributeAdditionFromType aft, @NotNull ConfigurationSection cfg) {
        super(stripAllTags(displayName), aft, cfg.getConfigurationSection(ATTR_SECTION_KEY));
        this.displayName = displayName;
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
