package io.github.tanice.twItemManager.manager.pdc;

import io.github.tanice.twItemManager.manager.pdc.impl.AttributePDC;
import io.github.tanice.twItemManager.manager.pdc.type.AttributeCalculateSection;
import io.github.tanice.twItemManager.manager.pdc.type.AttributeType;
import io.github.tanice.twItemManager.manager.pdc.type.DamageType;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serial;
import java.io.Serializable;
import java.util.*;

import static io.github.tanice.twItemManager.constance.key.AttributeKey.*;
import static io.github.tanice.twItemManager.constance.key.ConfigKey.CHANCE;
import static io.github.tanice.twItemManager.constance.key.ConfigKey.DURATION;

/**
 * 属性抽象
 */
@Getter
@Setter
public abstract class CalculablePDC implements Serializable, Comparable<CalculablePDC> {
    @Serial
    private static final long serialVersionUID = 1L;
    /* 属性在具体配置中的所属路径 */
    protected static final String ATTR_SECTION_KEY = "attrs";

    /* 属性名称 */
    protected String innerName;
    /* 优先级 */
    protected int priority;
    /* 所属计算区 */
    protected AttributeCalculateSection attributeCalculateSection;

    /* 属性结束时间(负数则永续) */
    protected long endTimeStamp;
    /* 激活几率 */
    protected double chance;
    /* 持续时间 */
    protected int duration;

    /* 属性具体值 */
    protected EnumMap<AttributeType, Double> vMap;
    /* 职业增伤 */
    protected EnumMap<DamageType, Double> tMap;

    /**
     * 供序列化使用
     */
    public CalculablePDC() {
        innerName = "default";
        priority = Integer.MAX_VALUE;
        attributeCalculateSection = AttributeCalculateSection.OTHER;
        endTimeStamp = -1;
        chance = 1;
        duration = -1;
        vMap = new EnumMap<>(AttributeType.class);
        tMap = new EnumMap<>(DamageType.class);
    }

    public CalculablePDC(AttributeCalculateSection attributeCalculateSection) {
        innerName = "default";
        priority = Integer.MAX_VALUE;
        this.attributeCalculateSection = attributeCalculateSection;
        endTimeStamp = -1;
        chance = 1;
        duration = -1;
        vMap = new EnumMap<>(AttributeType.class);
        tMap = new EnumMap<>(DamageType.class);
    }

    /**
     * 根据配置文件生成属性
     * @param innerName 属性内部名
     * @param acs 属性计算区
     * @param cfg attr下的配置即可
     */
    public CalculablePDC(@NotNull String innerName, @NotNull AttributeCalculateSection acs, @Nullable ConfigurationSection cfg) {
        this.innerName = innerName;
        priority = Integer.MAX_VALUE;
        attributeCalculateSection = acs;
        endTimeStamp = -1;
        chance = 1D;
        duration = -1;

        vMap = new EnumMap<>(AttributeType.class);
        tMap = new EnumMap<>(DamageType.class);

        if (cfg == null) return;
        /* 覆写 */
        chance = cfg.getDouble(CHANCE, 1D);
        duration = cfg.getInt(DURATION, -1);
        /* vMap初始化 */
        vMap.put(AttributeType.DAMAGE, cfg.getDouble(BASE_DAMAGE, 0D));
        vMap.put(AttributeType.ARMOR, cfg.getDouble(ARMOR, 0D));
        vMap.put(AttributeType.CRITICAL_STRIKE_CHANCE, cfg.getDouble(CRITICAL_STRIKE_CHANCE, 0D));
        vMap.put(AttributeType.CRITICAL_STRIKE_DAMAGE, cfg.getDouble(CRITICAL_STRIKE_DAMAGE, 0D));
        vMap.put(AttributeType.ARMOR_TOUGHNESS, cfg.getDouble(ARMOR_TOUGHNESS, 0D));
        vMap.put(AttributeType.PRE_ARMOR_REDUCTION, cfg.getDouble(PRE_ARMOR_REDUCTION, 0D));
        vMap.put(AttributeType.AFTER_ARMOR_REDUCTION, cfg.getDouble(AFTER_ARMOR_REDUCTION, 0D));
        vMap.put(AttributeType.MANA_COST, cfg.getDouble(MANA_COST, 0D));
        vMap.put(AttributeType.SKILL_COOL_DOWN, cfg.getDouble(SKILL_COOLDOWN, 0D));
        /* tMap初始化 */
        tMap.put(DamageType.MELEE, cfg.getDouble(MELEE, 0D));
        tMap.put(DamageType.MAGIC, cfg.getDouble(MAGIC, 0D));
        tMap.put(DamageType.RANGED, cfg.getDouble(RANGED, 0D));
        tMap.put(DamageType.ROUGE, cfg.getDouble(ROUGE, 0D));
        tMap.put(DamageType.SUMMON, cfg.getDouble(SUMMON, 0D));
        tMap.put(DamageType.OTHER, cfg.getDouble(OTHER, 0D));
    }

    /**
     * 数据整合(只整合统一的数据项)
     * 不同的type需要分类merge！
     * 此时的时间属性无效
     */
    public void merge(CalculablePDC @NotNull ... o) {
        for (CalculablePDC cPDC : o) {
            if (cPDC == null || attributeCalculateSection != cPDC.attributeCalculateSection) continue;
            for (AttributeType type : AttributeType.values()) {
                vMap.put(type, vMap.getOrDefault(type, 0D) + cPDC.vMap.getOrDefault(type, 0D));
            }
            for (DamageType type : DamageType.values()) {
                tMap.put(type, tMap.getOrDefault(type, 0D) + cPDC.tMap.getOrDefault(type, 0D));
            }
        }
    }

    /**
     * 显示形式
     */
    public abstract String toString();

    /**
     * 返回自身属于的类型
     * @return 数据来源类型
     */
    public AttributeCalculateSection fromType(){
        return attributeCalculateSection;
    }

    /**
     * 将类转为普通的数值计算基类
     */
    public @NotNull AttributePDC toAttributePDC() {
        AttributePDC pdc = new AttributePDC();
        pdc.attributeCalculateSection = attributeCalculateSection;
        pdc.merge(this);
        return pdc;
    }

    @Override
    public int compareTo(@NotNull CalculablePDC other) {
        // 按优先级升序排序
        return Integer.compare(this.priority, other.priority);
    }
}
