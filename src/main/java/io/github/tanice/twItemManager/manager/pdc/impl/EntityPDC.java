package io.github.tanice.twItemManager.manager.pdc.impl;

import io.github.tanice.twItemManager.manager.pdc.CalculablePDC;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.io.Serial;
import java.io.Serializable;
import java.util.*;

/**
 * 实体持有的属性
 * 计算区域无效
 */
@Getter
public class EntityPDC implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /* 影响属性的值 */
    private final Map<String, BuffPDC> buffs = new HashMap<>();

    public EntityPDC(){
    }

    @Override
    public @NotNull String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        boolean first = true;
        for (Map.Entry<String, BuffPDC> entry : buffs.entrySet()) {
            if (!first) sb.append(", ");
            sb.append(entry.getKey()).append('=').append(entry.getValue());
            first = false;
        }
        sb.append("}");
        return sb.toString();
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
        buffs.put(innerName, null);
    }
    /**
     * 删除buff
     */
    public void removeBuff(@NotNull BuffPDC bPDC){
        buffs.put(bPDC.getInnerName(), null);
    }

    /**
     * 清空buff
     */
    public void removeAllBuffs(){
        buffs.clear();
    }
}
