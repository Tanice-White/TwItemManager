package io.github.tanice.twItemManager.manager.pdc.impl;

import io.github.tanice.twItemManager.manager.pdc.CalculablePDC;
import io.github.tanice.twItemManager.manager.pdc.type.AttributeCalculateSection;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.io.Serial;
import java.util.List;

import static io.github.tanice.twItemManager.util.Tool.enumMapToString;

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

    public EntityPDC(@NotNull String innerName) {
        super(innerName, AttributeCalculateSection.OTHER, null);
    }

    @Override
    public String toString() {
        return "CalculablePDC{" +
                "priority=" + priority + ", " +
                "itemInnerName=" + innerName + ", " +
                "buffName=" + buffName + ", " +
                "attributeCalculateSection=" + attributeCalculateSection + ", " +
                "attribute-addition=" + enumMapToString(vMap) +
                "type-addition=" + enumMapToString(tMap) +
                "}";
    }
}
