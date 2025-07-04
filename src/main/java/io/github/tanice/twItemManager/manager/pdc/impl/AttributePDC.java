package io.github.tanice.twItemManager.manager.pdc.impl;

import io.github.tanice.twItemManager.manager.pdc.CalculablePDC;
import io.github.tanice.twItemManager.manager.pdc.type.AttributeCalculateSection;
import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

import java.io.Serial;
import java.util.List;

import static io.github.tanice.twItemManager.constance.key.ConfigKey.*;
import static io.github.tanice.twItemManager.util.Tool.enumMapToString;

/**
 * 单纯的某一个属性（基础属性）
 * 用于计算的基础类
 * 实例: quality 等没有
 */
@Getter
public class AttributePDC extends CalculablePDC {
    @Serial
    private static final long serialVersionUID = 1L;

    private String displayName;

    private String LoreTemplateName;

    public AttributePDC(){
        super();

    }

    public AttributePDC(AttributeCalculateSection s){
        super(s);
    }

    public AttributePDC(@NotNull String innerName, @NotNull AttributeCalculateSection acs, @NotNull ConfigurationSection cfg) {
        super(innerName, acs, cfg.getConfigurationSection(ATTR_SECTION_KEY));
        this.displayName = cfg.getString(DISPLAY_NAME, innerName);
        this.LoreTemplateName = cfg.getString(LORE_TEMPLATE);
    }

    @Override
    public @NotNull String toString() {
        return "AttributePDC{" +
                "priority=" + priority + ", " +
                "displayName='" + displayName + ", " +
                "itemInnerName=" + innerName + ", " +
                "attributeCalculateSection=" + attributeCalculateSection + ", " +
                "attribute-addition=" + enumMapToString(vMap) +
                "type-addition=" + enumMapToString(tMap) +
                "}";
    }
}
