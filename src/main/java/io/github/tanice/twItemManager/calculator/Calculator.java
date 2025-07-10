package io.github.tanice.twItemManager.calculator;

import io.github.tanice.twItemManager.TwItemManager;
import io.github.tanice.twItemManager.config.Config;
import io.github.tanice.twItemManager.infrastructure.PDCAPI;
import io.github.tanice.twItemManager.pdc.CalculablePDC;
import io.github.tanice.twItemManager.pdc.impl.AttributePDC;
import io.github.tanice.twItemManager.pdc.impl.BuffPDC;
import io.github.tanice.twItemManager.pdc.EntityPDC;
import io.github.tanice.twItemManager.pdc.type.AttributeCalculateSection;
import io.github.tanice.twItemManager.pdc.type.AttributeType;
import io.github.tanice.twItemManager.pdc.type.BuffActiveCondition;
import io.github.tanice.twItemManager.pdc.type.DamageType;
import io.github.tanice.twItemManager.util.EquipmentUtil;
import lombok.Getter;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

import static io.github.tanice.twItemManager.util.Logger.logInfo;

public abstract class Calculator {
    /** 是否使用玩家减伤平衡算法 */
    protected boolean useDamageReductionBalance;
    /**
     * 最终的伤害属性计算map
     * 获取伤害计算各个区的顺序
     * [BASE, DAMAGE_TYPE_ADDITION, ADD, MULTIPLY, FIX]
     * 根据攻击实体武器的伤害类型
     */
    @Getter
    private EnumMap<AttributeType, Double> AttributeTypeModifiers;
    /**
     * 玩家 DamageType 对应的值 (武器伤害类型的增伤)
     */
    @Getter
    protected EnumMap<DamageType, Double> damageTypeModifiers;

    protected List<BuffPDC> beforeList;
    protected List<BuffPDC> betweenList;
    protected List<BuffPDC> afterList;

    /**
     * 中间值 用于将属性从 AttributeCalculateSection 转化为 AttributeType 的 Map
     */
    protected EnumMap<AttributeCalculateSection, CalculablePDC> transformTmp;

    /* 序列化使用 */
    protected Calculator() {
    }

    public Calculator(@NotNull LivingEntity livingEntity) {
        useDamageReductionBalance = Config.useDamageReductionBalanceForPlayer;
        damageTypeModifiers = new EnumMap<>(DamageType.class);
        transformTmp = new EnumMap<>(AttributeCalculateSection.class);
        beforeList = new ArrayList<>();
        betweenList = new ArrayList<>();
        afterList = new ArrayList<>();
        AttributeTypeModifiers = new EnumMap<>(AttributeType.class);

        initBuffListsAndTransformTmp(livingEntity);
        initDamageTypeModifiers();
        initAttributeTypeModifiers();
    }

    /**
     * 获取伤害前生效的 buff
     */
    public List<BuffPDC> getOrderedBeforeList(@NotNull BuffActiveCondition role) {
        // 内部的 buffActiveCondition == role || buffActiveCondition == BuffActiveCondition.ALL 才返回
        return beforeList.stream()
                .filter(buff ->
                        buff.isEnable() && (buff.getBuffActiveCondition() == role || buff.getBuffActiveCondition() == BuffActiveCondition.ALL)
                ).collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * 获取防御计算前的生效 buff
     */
    public List<BuffPDC> getOrderedBetweenList(@NotNull BuffActiveCondition role) {
        return betweenList.stream()
                .filter(buff ->
                        buff.isEnable() && (buff.getBuffActiveCondition() == role || buff.getBuffActiveCondition() == BuffActiveCondition.ALL)
                ).collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * 获取最后生效的 buff
     */
    public List<BuffPDC> getOrderedAfterList(@NotNull BuffActiveCondition role) {
        return afterList.stream()
                .filter(buff ->
                        buff.isEnable() && (buff.getBuffActiveCondition() == role || buff.getBuffActiveCondition() == BuffActiveCondition.ALL)
                ).collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * 获取目标生效的buff
     */
    private @NotNull List<CalculablePDC> getEntityBuffsAsCalculablePDC(@NotNull LivingEntity entity) {
        EntityPDC ePDC = PDCAPI.getEntityPDC(entity);
        if (ePDC == null) return new ArrayList<>();
        return TwItemManager.getBuffManager().getEntityActiveBuffs(entity);
    }

    /**
     * 获取 BuffList和中间 Map
     */
    private void initBuffListsAndTransformTmp(LivingEntity entity) {
        List<CalculablePDC> PDCs = EquipmentUtil.getActiveEquipmentItemPDC(entity);
        PDCs.addAll(EquipmentUtil.getEffectiveAccessoryAttributePDC(entity));
        /* buff计算 */
        PDCs.addAll(getEntityBuffsAsCalculablePDC(entity));

        if (Config.debug) {
            StringBuilder s = new StringBuilder("[Calculator] PDCs in " + entity.getName() + ": ");
            for (CalculablePDC pdc : PDCs) s.append(pdc.getInnerName()).append(" ");
            logInfo(s.toString());
        }

        AttributeCalculateSection acs;
        CalculablePDC aPDC;
        for (CalculablePDC pdc : PDCs) {
            acs = pdc.getAttributeCalculateSection();
            if (acs == AttributeCalculateSection.OTHER) continue;
            /* 具体 */
            if (acs == AttributeCalculateSection.BEFORE_DAMAGE) beforeList.add((BuffPDC) pdc);
            else if (acs == AttributeCalculateSection.BETWEEN_DAMAGE_ADN_DEFENCE) betweenList.add((BuffPDC) pdc);
            else if (acs == AttributeCalculateSection.AFTER_DAMAGE) afterList.add((BuffPDC) pdc);
            /* BASE ADD MULTIPLY FIX */
            else {
                aPDC = transformTmp.getOrDefault(acs, new AttributePDC(acs));
                aPDC.merge(pdc, 1);
                transformTmp.put(acs, aPDC);
            }
        }
    }

    /**
     * 获取玩家 DamageType 属性 Map
     */
    private void initDamageTypeModifiers() {
        CalculablePDC cPDC;
        EnumMap<DamageType, Double> ctMap;
        for (AttributeCalculateSection acs : transformTmp.keySet()) {
            /* 排除 */
            if (acs == AttributeCalculateSection.BEFORE_DAMAGE || acs == AttributeCalculateSection.AFTER_DAMAGE || acs == AttributeCalculateSection.OTHER || acs == AttributeCalculateSection.TIMER || acs == AttributeCalculateSection.BETWEEN_DAMAGE_ADN_DEFENCE) continue;
            /* 数值整合计算 */
            cPDC = transformTmp.get(acs);
            ctMap = cPDC.getDamageTypeModifiers();
            /* 1.属性统计(逐个CalculatePDC统计) */
            for (DamageType dt : DamageType.values()) {
                damageTypeModifiers.put(dt, damageTypeModifiers.getOrDefault(dt, 0D) + ctMap.getOrDefault(dt, 0D));
            }
        }
    }

    private void initAttributeTypeModifiers() {
        CalculablePDC pdc;
        Double result = null;

        for (AttributeType attrType : AttributeType.values()) {
            pdc = transformTmp.get(AttributeCalculateSection.BASE);
            if (pdc != null) result = pdc.getAttributeTypeModifiers().get(attrType);
            if (result == null) result = 0D;
            if (result != 0D){
                /* 计算顺序：BASE * ADD * MULTIPLY * FIX */
                pdc = transformTmp.get(AttributeCalculateSection.ADD);
                if (pdc != null) {
                    Double value = pdc.getAttributeTypeModifiers().get(attrType);
                    result *= (1 + value);
                }
                pdc = transformTmp.get(AttributeCalculateSection.MULTIPLY);
                if (pdc != null) {
                    Double value = pdc.getAttributeTypeModifiers().get(attrType);
                    result *= (1 + value);
                }
                pdc = transformTmp.get(AttributeCalculateSection.FIX);
                if (pdc != null) {
                    Double value = pdc.getAttributeTypeModifiers().get(attrType);
                    result *= (1 + value);
                }
            }
            AttributeTypeModifiers.put(attrType, result);
        }

        AttributeTypeModifiers.put(AttributeType.PRE_ARMOR_REDUCTION, drBalance(AttributeTypeModifiers.get(AttributeType.PRE_ARMOR_REDUCTION)));
        AttributeTypeModifiers.put(AttributeType.AFTER_ARMOR_REDUCTION, drBalance(AttributeTypeModifiers.get(AttributeType.AFTER_ARMOR_REDUCTION)));
    }

    /**
     * 减伤平衡算法
     * @param oriDr 理论伤害减免比例
     * @return 平衡后的伤害减免比例
     */
    private double drBalance(Double oriDr) {
        if (oriDr == null) return 0D;
        if (useDamageReductionBalance) return oriDr / (1 + oriDr);
        return oriDr;
    }
}
