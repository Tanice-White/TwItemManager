package io.github.tanice.twItemManager.manager.pdc.impl;

import io.github.tanice.twItemManager.manager.pdc.CalculablePDC;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.io.Serial;
import java.io.Serializable;
import java.util.*;

import static io.github.tanice.twItemManager.util.Logger.logWarning;

/**
 * 实体持有的 buff属性
 */
@Getter
public class EntityPDC implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /* 影响属性的值 */
    /* 构造函数内初始化会触发序列化的问题 */
    private final Map<String, BuffPDC> holdBuffs;
    private final Map<String, BuffPDC> otherBuffs;

    public EntityPDC(){
        holdBuffs = new HashMap<>();
        otherBuffs = new HashMap<>();
    }

    @Override
    public @NotNull String toString() {
        Set<Map.Entry<String, BuffPDC>> allEntries = new HashSet<>(holdBuffs.entrySet());
        allEntries.addAll(otherBuffs.entrySet());

        StringBuilder sb = new StringBuilder();
        sb.append("{");
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
        for (BuffPDC bPDC : otherBuffs.values()) {
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
        res.addAll(holdBuffs.values());
        return res;
    }

    /**
     * 覆盖式增加 buff
     */
    public void addBuff(@NotNull BuffPDC bPDC, boolean isHoldBuff){
        if (!bPDC.isEnable()) return;

        BuffPDC pre;
        String inn = bPDC.getInnerName();
        if (isHoldBuff) {
            pre = holdBuffs.get(inn);
            if (pre == null) {
                holdBuffs.put(inn, bPDC);
                return;
            }
            /* 都用新的buff，只更新时间戳 */
            /* 永续则直接覆盖 */
            if (bPDC.getEndTimeStamp() >= 0)
                bPDC.setEndTimeStamp(Math.max(pre.getEndTimeStamp(), bPDC.getEndTimeStamp()));
            holdBuffs.put(inn, bPDC);
        } else {
            pre = otherBuffs.get(inn);
            if (pre == null) {
                otherBuffs.put(inn, bPDC);
                return;
            }
            /* 都用新的buff，只更新时间戳 */
            if (bPDC.getEndTimeStamp() >= 0)
                bPDC.setEndTimeStamp(Math.max(pre.getEndTimeStamp(), bPDC.getEndTimeStamp()));
            otherBuffs.put(inn, bPDC);
        }
    }

    /**
     * 删除buff
     */
    public void removeBuff(String innerName){
        holdBuffs.remove(innerName);
        otherBuffs.remove(innerName);
    }
    /**
     * 删除buff
     */
    public void removeBuff(@NotNull BuffPDC bPDC){
        holdBuffs.remove(bPDC.getInnerName());
        otherBuffs.remove(bPDC.getInnerName());
    }

    /**
     * 清空 holdBuff
     */
    public void removeHoldBuffs(){
        holdBuffs.clear();
    }

    /**
     * 清空 otherBuff
     */
    public void removeOtherBuffs(){
        otherBuffs.clear();
    }

    /**
     * 清空 buff
     */
    public void removeAllBuffs(){
        holdBuffs.clear();
        otherBuffs.clear();
    }
}
