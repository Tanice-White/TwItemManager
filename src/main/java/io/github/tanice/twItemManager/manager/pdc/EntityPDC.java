package io.github.tanice.twItemManager.manager.pdc;

import io.github.tanice.twItemManager.config.Config;
import io.github.tanice.twItemManager.manager.item.base.impl.Consumable;
import io.github.tanice.twItemManager.manager.pdc.impl.BuffPDC;
import io.github.tanice.twItemManager.util.Tool;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.io.Serial;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 实体持有的 buff属性
 * 与版本号相关
 */
public class EntityPDC implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    @Getter
    private final double version;


    /* 构造函数内初始化会触发序列化的问题 */
    /** key=[uuid]-[buffName] value=BuffPDC */
    private final Map<String, BuffPDC> buffs;

    /* 所食用的可食用物次数 */
    /** key=可食用物品内部名 value=Integer */
    private final Map<String, Integer> consumeTimes;

    /* 所食用可食用物品冷却(储存下一次可食用时间) */
    /** key=可食用物品内部名 value=Long */
    private final Map<String, Long> consumeCD;

    public EntityPDC(){
        version = Config.version;
        buffs = new ConcurrentHashMap<>();
        consumeTimes = new ConcurrentHashMap<>();
        consumeCD = new ConcurrentHashMap<>();
    }

    /**
     * 获取有效的 buff 名称
     */
    public List<CalculablePDC> getActiveBuffPDCs(long currentTime){
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
     * 获取 buff 名称
     * 在数据储存后，所有的buff都是生效了的，即可以直接更新
     */
    public List<BuffPDC> getBuffPDCs(){
        List<BuffPDC> res = new ArrayList<>();
        for (BuffPDC bPDC : buffs.values()) {
            if (bPDC != null){
                if (!bPDC.isEnable()) {
                    removeBuff(bPDC);
                    continue;
                }
                res.add(bPDC);
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

    /**
     * 自我简化，同时 只更新 buff 生效时间
     * 删除 永久性buff 和不再生效的buff
     * 可食用物品部分不需要
     */
    public void simplify(long currentTime) {
        if (buffs.isEmpty()) return;

        Iterator<Map.Entry<String, BuffPDC>> iterator = buffs.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, BuffPDC> entry = iterator.next();
            BuffPDC bPDC = entry.getValue();
            if (bPDC == null) continue;

            // 移除已过期的buff或永久性buff
            if (bPDC.getEndTimeStamp() <= currentTime || bPDC.getDuration() <= 0) {
                iterator.remove();
                removeBuff(bPDC);
                /* 正常的 buff 储存还能有效的时间 */
            } else bPDC.setDeltaTime(bPDC.getEndTimeStamp() - currentTime);
        }
    }

    public boolean canConsume(@NotNull Consumable consumable, long currentTime) {
        String innerName = consumable.getInnerName();
        int cd = consumable.getCd();
        int times = consumable.getTimes();

        boolean f = false;
        /* 食用cd判定 */
        Long c = consumeCD.get(innerName);
        if (c == null || cd <= 0 || c < currentTime) f = true;
        if (!f) return false;

        /* 食用次数判定 */
        f = false;
        Integer t = consumeTimes.get(innerName);
        if (t == null || times < 0 || t < times) f = true;

        return f;
    }

    /**
     * 食用物品
     */
    public void consume(@NotNull Consumable consumable, long currentTime) {
        String innerName = consumable.getInnerName();
        int cd = consumable.getCd();

        Integer t = consumeTimes.get(innerName);
        t = t == null ? 0 : t;
        /* 可以食用 */
        consumeTimes.put(innerName, t + 1);
        if (cd > 0) consumeCD.put(innerName, currentTime + (long) cd * 1000);
    }

    @Override
    public @NotNull String toString() {
        Set<Map.Entry<String, BuffPDC>> allEntries = buffs.entrySet();

        StringBuilder sb = new StringBuilder();
        sb.append("EntityPDC{");
        sb.append("version=").append(version).append(", ");
        boolean first = true;
        for (Map.Entry<String, BuffPDC> entry : allEntries) {
            if (!first) sb.append(", ");
            sb.append(entry.getKey()).append('=').append(entry.getValue());
            first = false;
        }
        sb.append("\n");
        sb.append("consumeCD").append(Tool.mapToString2(consumeCD)).append(", ");
        sb.append("consumeTimes").append(Tool.mapToString3(consumeTimes));
        sb.append("}");
        return sb.toString();
    }
}
