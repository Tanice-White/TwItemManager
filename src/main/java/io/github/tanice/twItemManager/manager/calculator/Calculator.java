package io.github.tanice.twItemManager.manager.calculator;

import io.github.tanice.twItemManager.infrastructure.PDCAPI;
import io.github.tanice.twItemManager.manager.pdc.CalculablePDC;
import io.github.tanice.twItemManager.manager.pdc.impl.AttributePDC;
import io.github.tanice.twItemManager.manager.pdc.impl.BuffPDC;
import io.github.tanice.twItemManager.manager.pdc.impl.EntityPDC;
import io.github.tanice.twItemManager.manager.pdc.impl.ItemPDC;
import io.github.tanice.twItemManager.manager.pdc.type.AttributeCalculateSection;
import io.github.tanice.twItemManager.manager.pdc.type.AttributeType;
import io.github.tanice.twItemManager.manager.pdc.type.DamageType;
import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static io.github.tanice.twItemManager.infrastructure.PDCAPI.*;
import static io.github.tanice.twItemManager.util.Logger.logWarning;

@Getter
public abstract class Calculator {
    /**
     * 根据就算区分好的属性Map
     * Timer 和 Other 不记录
     * Before_Damage 和 After_damage 单独记录
     */
    protected final EnumMap<DamageType, Double> damageTypeMap = new EnumMap<>(DamageType.class);
    protected final EnumMap<AttributeCalculateSection, CalculablePDC> resultMap = new EnumMap<>(AttributeCalculateSection.class);
    protected final List<BuffPDC> beforeList = new ArrayList<>();
    protected final List<BuffPDC> betweenList = new ArrayList<>();
    protected final List<BuffPDC> afterList = new ArrayList<>();

    public Calculator(@Nullable Entity e) {
        if (!(e instanceof LivingEntity living)) return;
        List<CalculablePDC> PDCs = getEffectiveEquipmentPDC(living);
        PDCs.addAll(getEffectiveAccessory(living));
        PDCs.addAll(getEntityCalculablePDC(living));

        AttributeCalculateSection acs;
        CalculablePDC aPDC;
        for (CalculablePDC pdc : PDCs) {
            acs = pdc.getAttributeCalculateSection();
            if (acs == AttributeCalculateSection.OTHER) continue;
            /* 具体 */
            if (acs == AttributeCalculateSection.BEFORE_DAMAGE) beforeList.add((BuffPDC) pdc);
            else if (acs == AttributeCalculateSection.AFTER_DAMAGE) beforeList.add((BuffPDC) pdc);
            else if (acs == AttributeCalculateSection.BETWEEN_DAMAGE_ADN_DEFENCE) betweenList.add((BuffPDC) pdc);
            else {
                aPDC = resultMap.getOrDefault(acs, new AttributePDC(acs));
                aPDC.merge(pdc);
                resultMap.put(acs, aPDC);
            }
        }
        initDamageMap();
    }

    /**
     * 遍历物体生效槽位返回物品列表
     */
    protected @NotNull List<CalculablePDC> getEffectiveEquipmentPDC(@NotNull LivingEntity entity) {
        EntityEquipment equip = entity.getEquipment();
        if (equip == null) return new ArrayList<>();

        List<CalculablePDC> res = new ArrayList<>();
        CalculablePDC cp;
        String slot;
        ItemStack it;
        it = equip.getItemInMainHand();
        slot = getSlot(it);
        if (slot != null && (slot.equalsIgnoreCase("hand") || slot.equalsIgnoreCase("mainHand"))) {
            cp = getItemCalculablePDC(it);
            if (cp != null) res.add(cp);
        }

        equip.getItemInOffHand();
        slot = getSlot(it);
        if (slot != null && (slot.equalsIgnoreCase("hand") || slot.equalsIgnoreCase("offHand"))) {
            cp = getItemCalculablePDC(it);
            if (cp != null) res.add(cp);
        }

        equip.getHelmet();
        slot = getSlot(it);
        if (slot != null && (slot.equalsIgnoreCase("head") || slot.equalsIgnoreCase("helmet"))) {
            cp = getItemCalculablePDC(it);
            if (cp != null) res.add(cp);
        }

        equip.getChestplate();
        slot = getSlot(it);
        if (slot != null && (slot.equalsIgnoreCase("chest") || slot.equalsIgnoreCase("chestPlate"))) {
            cp = getItemCalculablePDC(it);
            if (cp != null) res.add(cp);
        }

        equip.getLeggings();
        if (slot != null && (slot.equalsIgnoreCase("legs") || slot.equalsIgnoreCase("leggings"))) {
            cp = getItemCalculablePDC(it);
            if (cp != null) res.add(cp);
        }

        equip.getBoots();
        if (slot != null && (slot.equalsIgnoreCase("boots") || slot.equalsIgnoreCase("feet"))) {
            cp = getItemCalculablePDC(it);
            if (cp != null) res.add(cp);
        }
        return res;
    }

    /**
     * 获取目标生效的buff
     */
    protected @NotNull List<CalculablePDC> getEntityCalculablePDC(@NotNull LivingEntity entity) {
        EntityPDC ePDC = PDCAPI.getEntityCalculablePDC(entity);
        if (ePDC == null) return new ArrayList<>();

        long ct = System.currentTimeMillis();
        return ePDC.getBuffPDCs(ct);
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
            if (acs == AttributeCalculateSection.BEFORE_DAMAGE || acs == AttributeCalculateSection.AFTER_DAMAGE || acs == AttributeCalculateSection.OTHER) continue;
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
