package io.github.tanice.twItemManager.manager.calculator;

import io.github.tanice.twItemManager.infrastructure.PDCAPI;
import io.github.tanice.twItemManager.manager.item.ItemManager;
import io.github.tanice.twItemManager.manager.pdc.CalculablePDC;
import io.github.tanice.twItemManager.manager.pdc.impl.AttributePDC;
import io.github.tanice.twItemManager.manager.pdc.impl.EntityPDC;
import io.github.tanice.twItemManager.manager.pdc.impl.ItemPDC;
import io.github.tanice.twItemManager.manager.pdc.type.AttributeAdditionFromType;
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

    // 统计数据
    protected final List<ItemPDC> equipmentPDCList;
    protected final EntityPDC entityPDC;
    protected final List<ItemPDC> accessoryPDCList;

    protected ItemManager itemManager;

    // 结果属性
    protected final EnumMap<AttributeAdditionFromType, AttributePDC> vMap = new EnumMap<>(AttributeAdditionFromType.class);

    public Calculator(@NotNull ItemManager im, @NotNull LivingEntity living) {
        itemManager = im;
        equipmentPDCList = getEffectiveEquipmentPDC(living);
        entityPDC = getEntityCalculablePDC(living);
        accessoryPDCList = getEffectiveAccessory(living);
        calculateAttributePDCByType();
    }

    /**
     * 遍历物体生效槽位返回物品列表
     * 返回6个实体 [主手 副手 鞋子 裤子 胸甲 头盔]
     */
    protected @NotNull List<ItemPDC> getEffectiveEquipmentPDC(@NotNull LivingEntity entity) {
        EntityEquipment equip = entity.getEquipment();
        if (equip == null) return new ArrayList<>();

        List<ItemPDC> res = new ArrayList<>();
        List<ItemStack> itemList = new ArrayList<>();
        itemList.add(equip.getItemInMainHand());
        itemList.add(equip.getItemInOffHand());
        itemList.addAll(List.of(equip.getArmorContents()));

        int i = -1;
        CalculablePDC cPDC;
        String slot;
        for (ItemStack item : itemList) {
            i ++;
            if (item == null || item.getType() == Material.AIR) {
                res.add(null);
                continue;
            }
            slot = getSlot(item);
            if (slot == null || !(slot.equalsIgnoreCase(SLOTS[i]) || slot.equalsIgnoreCase("any"))) {
                res.add(null);
                continue;
            }
            cPDC = getItemCalculablePDC(item);
            if (cPDC == null) continue;
            res.add((ItemPDC) cPDC);
        }
        return res;
    }

    /**
     * 获取目标生效的buff
     */
    protected @Nullable EntityPDC getEntityCalculablePDC(@NotNull LivingEntity entity) {
        return PDCAPI.getEntityCalculablePDC(entity);
    }

    /**
     * 遍历目标的饰品
     */
    protected @NotNull List<ItemPDC> getEffectiveAccessory(@NotNull LivingEntity entity) {
        return new ArrayList<>();
    }

    /**
     * 得到不同type的结果AttributePDC
     */
    protected void calculateAttributePDCByType() {
        /* 初始化 */
        for (AttributeAdditionFromType type : AttributeAdditionFromType.values()) {
            vMap.put(type, new AttributePDC());
        }

        for (ItemPDC item : equipmentPDCList) {

        }
        // 保存 品质 装备
        vMap.put(AttributeAdditionFromType.QUALITY, attributePDC);

        attributePDC = new AttributePDC();
        for (ItemPDC item : accessoryPDCList) {

        }
        vMap.put(AttributeAdditionFromType.ACCESSORY, attributePDC);

        for (String buffName : entityPDC.getBuffName()) {
            AttributePDC a = itemManager.getBuffPDC(buffName);
            if (a == null) continue;
            vMap.get(AttributeAdditionFromType.BUFF).merge(a);
        }
    }

    /**
     * 获取计算器最终的计算结果
     */
    public abstract double getFinalV(@NotNull LivingEntity entity);
}
