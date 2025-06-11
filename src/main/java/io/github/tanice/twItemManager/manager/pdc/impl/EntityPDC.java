package io.github.tanice.twItemManager.manager.pdc.impl;

import io.github.tanice.twItemManager.constance.key.AttributeKey;
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
 * 计算区域无效
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

    public EntityPDC(@NotNull String innerName) {
        super(innerName, AttributeCalculateSection.OTHER, null);
    }

    @Override
    public @NotNull String toString() {
        return "CalculablePDC{" +
                "priority=" + priority + ", " +
                "itemInnerName=" + innerName + ", " +
                "buffName={" + buffs.keySet() + "}, " +
                "attributeCalculateSection=" + attributeCalculateSection + ", " +
                "attribute-addition=" + enumMapToString(vMap) +
                "type-addition=" + enumMapToString(tMap) +
                "}";
    }

    @Override
    public @NotNull Map<AttributeKey, String> toLoreMap() {
        return Map.of();
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

    /**
     * 覆盖式增加 buff
     */
    public void addBuff(BuffPDC bPDC){
        buffs.put(bPDC.getInnerName(), bPDC);
    }

    /**
     * 删除buff
     */
    public void removeBuff(String innerName){
        buffs.remove(innerName);
    }
    /**
     * 删除buff
     */
    public void removeBuff(@NotNull BuffPDC bPDC){
        buffs.remove(bPDC.getInnerName());
    }
}
