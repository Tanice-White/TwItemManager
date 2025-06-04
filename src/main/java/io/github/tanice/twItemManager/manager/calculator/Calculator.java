package io.github.tanice.twItemManager.manager.calculator;

import io.github.tanice.twItemManager.infrastructure.PDCAPI;
import io.github.tanice.twItemManager.manager.pdc.CalculablePDC;
import io.github.tanice.twItemManager.manager.pdc.impl.AttributePDC;
import io.github.tanice.twItemManager.manager.pdc.impl.BuffPDC;
import io.github.tanice.twItemManager.manager.pdc.impl.EntityPDC;
import io.github.tanice.twItemManager.manager.pdc.impl.ItemPDC;
import io.github.tanice.twItemManager.manager.pdc.type.AttributeCalculateSection;
import io.github.tanice.twItemManager.manager.pdc.type.DamageFromType;
import io.github.tanice.twItemManager.manager.pdc.type.DamageType;
import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static io.github.tanice.twItemManager.infrastructure.PDCAPI.*;

@Getter
public abstract class Calculator {
    private static final String[] SLOTS = new String[]{"HAND", "OFF_HAND", "FEET", "LEGS", "CHEST", "HEAD"};

    /**
     * 根据就算区分好的属性Map
     * Timer 和 Other 不记录
     * Before_Damage 和 After_damage 单独记录
     */
    protected final EnumMap<AttributeCalculateSection, CalculablePDC> resulMap = new EnumMap<>(AttributeCalculateSection.class);
    protected final List<BuffPDC> beforeList = new ArrayList<>();
    protected final List<BuffPDC> afterList = new ArrayList<>();

    public Calculator(@NotNull LivingEntity living) {
        List<CalculablePDC> PDCs = getEffectiveEquipmentPDC(living);
        PDCs.addAll(getEffectiveAccessory(living));
        PDCs.addAll(getEntityCalculablePDC(living));

        AttributeCalculateSection acs;
        for (CalculablePDC pdc : PDCs) {
            acs = pdc.getAttributeCalculateSection();
            if (acs == AttributeCalculateSection.OTHER) continue;
            /* 具体细节 */
            if (acs == AttributeCalculateSection.BEFORE_DAMAGE) beforeList.add((BuffPDC) pdc);
            else if (acs == AttributeCalculateSection.AFTER_DAMAGE) beforeList.add((BuffPDC) pdc);
            else resulMap.getOrDefault(pdc.getAttributeCalculateSection(), new AttributePDC()).merge(pdc);
        }

    }

    /**
     * 遍历物体生效槽位返回物品列表
     * 返回 <=6个实体 [主手 副手 鞋子 裤子 胸甲 头盔]
     */
    protected @NotNull List<CalculablePDC> getEffectiveEquipmentPDC(@NotNull LivingEntity entity) {
        EntityEquipment equip = entity.getEquipment();
        if (equip == null) return new ArrayList<>();

        List<CalculablePDC> res = new ArrayList<>();
        List<ItemStack> itemList = new ArrayList<>();
        itemList.add(equip.getItemInMainHand());
        itemList.add(equip.getItemInOffHand());
        itemList.addAll(List.of(equip.getArmorContents()));

        int i = -1;
        CalculablePDC cPDC;
        ItemPDC iPDC;
        String slot;
        for (ItemStack item : itemList) {
            i ++;
            if (item == null || item.getType() == Material.AIR) continue;
            slot = getSlot(item);
            if (slot == null || !(slot.equalsIgnoreCase(SLOTS[i]) || slot.equalsIgnoreCase("any"))) continue;

            cPDC = getItemCalculablePDC(item);
            if (cPDC == null) continue;
            /* 转为itemPDC再调用 */
            iPDC = (ItemPDC)cPDC;
            iPDC.selfCalculate();
            res.add(iPDC);
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
     * 获取伤害计算值并返回此实体的伤害来源
     */
    public abstract DamageType getCombatValue();

    /**
     * 获取防御计算 并得到最终结果
     */
    public abstract double calculateFinalDamage(double CombatValue , @Nullable DamageFromType damageFromType);
}
