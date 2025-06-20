package io.github.tanice.twItemManager.manager.calculator;

import io.github.tanice.twItemManager.config.Config;
import io.github.tanice.twItemManager.infrastructure.PDCAPI;
import io.github.tanice.twItemManager.manager.pdc.CalculablePDC;
import io.github.tanice.twItemManager.manager.pdc.impl.AttributePDC;
import io.github.tanice.twItemManager.manager.pdc.impl.BuffPDC;
import io.github.tanice.twItemManager.manager.pdc.impl.EntityPDC;
import io.github.tanice.twItemManager.manager.pdc.impl.ItemPDC;
import io.github.tanice.twItemManager.manager.pdc.type.AttributeCalculateSection;
import io.github.tanice.twItemManager.manager.pdc.type.AttributeType;
import io.github.tanice.twItemManager.manager.pdc.type.BuffActiveCondition;
import io.github.tanice.twItemManager.manager.pdc.type.DamageType;
import io.github.tanice.twItemManager.util.SlotUtil;
import lombok.Getter;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

import static io.github.tanice.twItemManager.infrastructure.PDCAPI.*;
import static io.github.tanice.twItemManager.util.Logger.logInfo;

public abstract class Calculator {
    /**
     * 根据就算区分好的属性Map
     * Timer 和 Other 不记录
     * Before_Damage 和 After_damage 单独记录
     */
    @Getter
    protected final EnumMap<DamageType, Double> damageTypeMap = new EnumMap<>(DamageType.class);
    @Getter
    protected final EnumMap<AttributeCalculateSection, CalculablePDC> resultMap = new EnumMap<>(AttributeCalculateSection.class);
    protected final List<BuffPDC> beforeList = new ArrayList<>();
    protected final List<BuffPDC> betweenList = new ArrayList<>();
    protected final List<BuffPDC> afterList = new ArrayList<>();

    /* 给序列机使用 */
    public Calculator() {

    }

    public Calculator(@NotNull LivingEntity e) {
        List<CalculablePDC> PDCs = getEffectiveEquipmentPDC(e);
        PDCs.addAll(getEffectiveAccessory(e));
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
        initDamageMap();
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
     * 遍历物体生效槽位返回物品列表
     */
    protected @NotNull List<CalculablePDC> getEffectiveEquipmentPDC(@NotNull LivingEntity entity) {
        EntityEquipment equip = entity.getEquipment();
        if (equip == null) return new ArrayList<>();

        List<CalculablePDC> res = new ArrayList<>();
        CalculablePDC cp;
        ItemStack it;

        it = equip.getItemInMainHand();
        if (SlotUtil.mainHandJudge(getSlot(it))) {
            cp = getCalculablePDC(it);
            if (cp != null) {
                if (cp instanceof ItemPDC) ((ItemPDC) cp).selfCalculate();
                res.add(cp);
            }
        }

        it = equip.getItemInOffHand();
        if (SlotUtil.offHandJudge(getSlot(it))) {
            cp = getCalculablePDC(it);
            if (cp != null) {
                if (cp instanceof ItemPDC) ((ItemPDC) cp).selfCalculate();
                res.add(cp);
            }
        }

        it = equip.getHelmet();
        if (it != null) {
            if (SlotUtil.helmetJudge(getSlot(it))) {
                cp = getCalculablePDC(it);
                if (cp != null) {
                    if (cp instanceof ItemPDC) ((ItemPDC) cp).selfCalculate();
                    res.add(cp);
                }
            }
        }

        it = equip.getChestplate();
        if (it != null) {
            if (SlotUtil.chestJudge(getSlot(it))) {
                cp = getCalculablePDC(it);
                if (cp != null) {
                    if (cp instanceof ItemPDC) ((ItemPDC) cp).selfCalculate();
                    res.add(cp);
                }
            }
        }

        it = equip.getLeggings();
        if (it != null) {
            if (SlotUtil.legsJudge(getSlot(it))) {
                cp = getCalculablePDC(it);
                if (cp != null) {
                    if (cp instanceof ItemPDC) ((ItemPDC) cp).selfCalculate();
                    res.add(cp);
                }
            }
        }

        it = equip.getBoots();
        if (it != null) {
            if (SlotUtil.bootsJudge(getSlot(it))) {
                cp = getCalculablePDC(it);
                if (cp != null) {
                    if (cp instanceof ItemPDC) ((ItemPDC) cp).selfCalculate();
                    res.add(cp);
                }
            }
        }

        return res;
    }

    /**
     * 获取目标生效的buff
     */
    protected @NotNull List<CalculablePDC> getEntityCalculablePDC(@NotNull LivingEntity e) {
        EntityPDC ePDC = PDCAPI.getCalculablePDC(e);
        if (ePDC == null) return new ArrayList<>();
        return ePDC.getBuffPDCs(System.currentTimeMillis());
    }

    /**
     * 遍历目标的饰品
     */
    protected @NotNull List<AttributePDC> getEffectiveAccessory(@NotNull LivingEntity entity) {
        return new ArrayList<>();
    }

    /**
     * 获取伤害计算各个区的顺序
     * [BASE, DAMAGE_TYPE_ADDITION, ADD, MULTIPLY, FIX]
     * 根据攻击实体武器的伤害类型
     */
    public abstract EnumMap<AttributeType, Double> getAttrsValue();

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
}
