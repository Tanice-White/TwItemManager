package io.github.tanice.twItemManager.manager.pdc.impl;

import io.github.tanice.twItemManager.manager.pdc.CalculablePDC;
import io.github.tanice.twItemManager.manager.pdc.type.AttributeCalculateSection;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static io.github.tanice.twItemManager.util.Tool.enumMapToString;

/**
 * 实体持有的属性
 */
@Getter
public class EntityPDC extends CalculablePDC {
    @Serial
    private static final long serialVersionUID = 1L;

    /* 影响属性的值 */
    private Map<String, BuffPDC> buffs;

    public EntityPDC(){
        super();
    }

    public EntityPDC(AttributeCalculateSection s){
        super(s);
    }

    public EntityPDC(@NotNull String innerName) {
        super(innerName, AttributeCalculateSection.OTHER, null);
    }

    @Override
    public String toString() {
        return "CalculablePDC{" +
                "priority=" + priority + ", " +
                "itemInnerName=" + innerName + ", " +
                "buffName={" + buffs.keySet() + "}, " +
                "attributeCalculateSection=" + attributeCalculateSection + ", " +
                "attribute-addition=" + enumMapToString(vMap) +
                "type-addition=" + enumMapToString(tMap) +
                "}";
    }

    /**
     * 获取buff名称
     */
    public List<CalculablePDC> getBuffPDCs(long currentTime){
        List<CalculablePDC> res = new ArrayList<>();
        for (BuffPDC bPDC : buffs.values()) {
            if (bPDC != null && bPDC.getEndTimeStamp() < currentTime) res.add(bPDC);
        }
        return res;
    }
}
