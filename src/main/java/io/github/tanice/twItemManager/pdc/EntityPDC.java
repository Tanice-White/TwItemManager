package io.github.tanice.twItemManager.pdc;

import io.github.tanice.twItemManager.config.Config;
import io.github.tanice.twItemManager.manager.item.base.impl.Consumable;
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

    /* 所食用的可食用物次数 */
    /** key=可食用物品内部名 value=Integer */
    private final Map<String, Integer> consumeTimes;

    /* 所食用可食用物品冷却(储存下一次可食用时间) */
    /** key=可食用物品内部名 value=Long */
    private final Map<String, Long> consumeCD;

    public EntityPDC(){
        version = Config.version;
        consumeTimes = new ConcurrentHashMap<>();
        consumeCD = new ConcurrentHashMap<>();
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
        return "EntityPDC{" +
                "version=" + version + ", " +
                "consumeCD=" + Tool.mapToString2(consumeCD) + ", " +
                "consumeTimes=" + Tool.mapToString3(consumeTimes) +
                "}";
    }
}
