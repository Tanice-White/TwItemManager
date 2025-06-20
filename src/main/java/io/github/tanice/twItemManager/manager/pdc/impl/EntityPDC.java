package io.github.tanice.twItemManager.manager.pdc.impl;

import io.github.tanice.twItemManager.manager.pdc.CalculablePDC;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.io.Serial;
import java.io.Serializable;
import java.util.*;

/**
 * 实体持有的 buff属性
 */
@Getter
public class EntityPDC implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /* 影响属性的值 */
    /* 构造函数内初始化会触发序列化的问题 */
    private final Map<String, BuffPDC> buffs;

    public EntityPDC(){
        buffs = new HashMap<>();
    }

    @Override
    public @NotNull String toString() {
        Set<Map.Entry<String, BuffPDC>> allEntries = buffs.entrySet();

        StringBuilder sb = new StringBuilder();
        sb.append("EntityPDC{");
        boolean first = true;
        for (Map.Entry<String, BuffPDC> entry : allEntries) {
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
            if (bPDC != null){
                if (!bPDC.isEnable()) {
                    removeBuff(bPDC);
                    continue;
                }
                if (bPDC.getEndTimeStamp() < 0) res.add(bPDC);
                else if (bPDC.getEndTimeStamp() > currentTime) res.add(bPDC);
                else removeBuff(bPDC);
            }
        }
        return res;
    }

    /**
     * 覆盖式增加 buff
     */
    public void addBuff(@NotNull BuffPDC bPDC){
        if (!bPDC.isEnable()) return;
        BuffPDC pre;
        String inn = bPDC.getInnerName();

        pre = buffs.get(inn);
        if (pre == null) {
            buffs.put(inn, bPDC);
            return;
        }
        /* 都用新的buff，只更新时间戳 */
        /* 永续则直接覆盖 */
        if (bPDC.getEndTimeStamp() >= 0)
            bPDC.setEndTimeStamp(Math.max(pre.getEndTimeStamp(), bPDC.getEndTimeStamp()));
        buffs.put(inn, bPDC);
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

    /**
     * 清空 Buff
     */
    public void removeBuffs(){
        buffs.clear();
    }
}
