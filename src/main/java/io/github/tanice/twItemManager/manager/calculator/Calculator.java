package io.github.tanice.twItemManager.manager.calculator;

import io.github.tanice.twItemManager.config.Config;
import io.github.tanice.twItemManager.infrastructure.PDCAPI;
import io.github.tanice.twItemManager.manager.pdc.CalculablePDC;
import io.github.tanice.twItemManager.manager.pdc.impl.AttributePDC;
import io.github.tanice.twItemManager.manager.pdc.impl.BuffPDC;
import io.github.tanice.twItemManager.manager.pdc.EntityPDC;
import io.github.tanice.twItemManager.manager.pdc.type.AttributeCalculateSection;
import io.github.tanice.twItemManager.manager.pdc.type.AttributeType;
import io.github.tanice.twItemManager.manager.pdc.type.BuffActiveCondition;
import io.github.tanice.twItemManager.manager.pdc.type.DamageType;
import io.github.tanice.twItemManager.util.EquipmentUtil;
import lombok.Getter;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

import static io.github.tanice.twItemManager.util.Logger.logInfo;

public abstract class Calculator {
    /** 是否使用玩家减伤平衡算法 */
    private boolean useDamageReductionBalance;
    /**
     * 最终的伤害属性计算map
     * 获取伤害计算各个区的顺序
     * [BASE, DAMAGE_TYPE_ADDITION, ADD, MULTIPLY, FIX]
     * 根据攻击实体武器的伤害类型
     */
    @Getter
    private EnumMap<AttributeType, Double> damageModifiers;
    /**
     * 根据就算区分好的属性Map
     */
    @Getter
    protected final EnumMap<DamageType, Double> damageTypeMap = new EnumMap<>(DamageType.class);
    @Getter
    protected final EnumMap<AttributeCalculateSection, CalculablePDC> resultMap = new EnumMap<>(AttributeCalculateSection.class);

    protected final List<BuffPDC> beforeList = new ArrayList<>();
    protected final List<BuffPDC> betweenList = new ArrayList<>();
    protected final List<BuffPDC> afterList = new ArrayList<>();

    /* 序列化使用 */
    public Calculator() {
    }

    public Calculator(@NotNull LivingEntity e) {
        List<CalculablePDC> PDCs = EquipmentUtil.getActiveEquipmentItemPDC(e);
        PDCs.addAll(EquipmentUtil.getEffectiveAccessoryAttributePDC(e));
        /* buff计算 */
        PDCs.addAll(getEntityCalculablePDC(e));

        if (Config.debug) {
            StringBuilder s = new StringBuilder("debug: [Calculator] PDCs in " + e.getName() + ": ");
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
            else {
                aPDC = resultMap.getOrDefault(acs, new AttributePDC(acs));
                aPDC.merge(pdc, 1);
                resultMap.put(acs, aPDC);
            }
        }
        useDamageReductionBalance = Config.useDamageReductionBalanceForPlayer;
        damageModifiers = new EnumMap<>(AttributeType.class);

        initDamageMap();
        initDamageModifiers();
    }

    /**
     * 获取伤害前生效的 buff
     */
    public List<BuffPDC> getBeforeList(@NotNull BuffActiveCondition role) {
        // 内部的 buffActiveCondition == role || buffActiveCondition == BuffActiveCondition.ALL 才返回
        return beforeList.stream()
                .filter(buff ->
                        buff.isEnable() && (buff.getBuffActiveCondition() == role || buff.getBuffActiveCondition() == BuffActiveCondition.ALL)
                ).collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * 获取防御计算前的生效 buff
     */
    public List<BuffPDC> getBetweenList(@NotNull BuffActiveCondition role) {
        return betweenList.stream()
                .filter(buff ->
                        buff.isEnable() && (buff.getBuffActiveCondition() == role || buff.getBuffActiveCondition() == BuffActiveCondition.ALL)
                ).collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * 获取最后生效的 buff
     */
    public List<BuffPDC> getAfterList(@NotNull BuffActiveCondition role) {
        return afterList.stream()
                .filter(buff ->
                        buff.isEnable() && (buff.getBuffActiveCondition() == role || buff.getBuffActiveCondition() == BuffActiveCondition.ALL)
                ).collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * 获取目标生效的buff
     */
    protected @NotNull List<CalculablePDC> getEntityCalculablePDC(@NotNull LivingEntity e) {
        EntityPDC ePDC = PDCAPI.getEntityPDC(e);
        if (ePDC == null) return new ArrayList<>();
        return ePDC.getActiveBuffPDCs(System.currentTimeMillis());
    }

    /**
     * 获取玩家 DamageType 属性 Map
     */
    protected void initDamageMap() {
        CalculablePDC cPDC;
        EnumMap<DamageType, Double> ctMap;
        for (AttributeCalculateSection acs : resultMap.keySet()) {
            /* 排除 */
            if (acs == AttributeCalculateSection.BEFORE_DAMAGE || acs == AttributeCalculateSection.AFTER_DAMAGE || acs == AttributeCalculateSection.OTHER || acs == AttributeCalculateSection.TIMER || acs == AttributeCalculateSection.BETWEEN_DAMAGE_ADN_DEFENCE) continue;
            /* 数值整合计算 */
            cPDC = resultMap.get(acs);
            ctMap = cPDC.getTMap();
            /* 1.属性统计(逐个CalculatePDC统计) */
            for (DamageType dt : DamageType.values()) {
                damageTypeMap.put(dt, damageTypeMap.getOrDefault(dt, 0D) + ctMap.getOrDefault(dt, 0D));
            }
        }
    }

    private void initDamageModifiers() {
        CalculablePDC pdc;
        Double result = null;

        for (AttributeType attrType : AttributeType.values()) {
            pdc = resultMap.get(AttributeCalculateSection.BASE);
            if (pdc != null) result = pdc.getVMap().get(attrType);
            if (result == null) result = 0D;
            if (result != 0D){
                /* 计算顺序：BASE * ADD * MULTIPLY * FIX */
                pdc = resultMap.get(AttributeCalculateSection.ADD);
                if (pdc != null) {
                    Double value = pdc.getVMap().get(attrType);
                    result *= (1 + value);
                }
                pdc = resultMap.get(AttributeCalculateSection.MULTIPLY);
                if (pdc != null) {
                    Double value = pdc.getVMap().get(attrType);
                    result *= (1 + value);
                }
                pdc = resultMap.get(AttributeCalculateSection.FIX);
                if (pdc != null) {
                    Double value = pdc.getVMap().get(attrType);
                    result *= (1 + value);
                }
            }
            damageModifiers.put(attrType, result);
        }

        damageModifiers.put(AttributeType.PRE_ARMOR_REDUCTION, drBalance(damageModifiers.get(AttributeType.PRE_ARMOR_REDUCTION)));
        damageModifiers.put(AttributeType.AFTER_ARMOR_REDUCTION, drBalance(damageModifiers.get(AttributeType.AFTER_ARMOR_REDUCTION)));
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
