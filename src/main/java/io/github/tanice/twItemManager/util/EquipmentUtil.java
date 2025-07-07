package io.github.tanice.twItemManager.util;

import io.github.tanice.twItemManager.TwItemManager;
import io.github.tanice.twItemManager.manager.item.base.BaseItem;
import io.github.tanice.twItemManager.manager.item.base.impl.Item;
import io.github.tanice.twItemManager.manager.pdc.CalculablePDC;
import io.github.tanice.twItemManager.manager.pdc.impl.AttributePDC;
import io.github.tanice.twItemManager.manager.pdc.impl.ItemPDC;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.List;

import static io.github.tanice.twItemManager.infrastructure.PDCAPI.getCalculablePDC;
import static io.github.tanice.twItemManager.infrastructure.PDCAPI.getSlot;

public class EquipmentUtil {
    // TODO 原版护甲的护甲值按照正常的提供？

    /**
     * 获取实体装备的内部Item
     */
    public static List<Item> getActiveEquipmentItem(@NotNull LivingEntity living) {
        EntityEquipment equip = living.getEquipment();
        if (equip == null) return List.of();

        List<Item> res = new ArrayList<>(12);

        ItemStack it;
        BaseItem bit;
        it = equip.getItemInMainHand();
        bit = TwItemManager.getItemManager().getBaseItem(it);
        if (SlotUtil.mainHandJudge(getSlot(it)) && bit instanceof Item i &&isValidItem(it)) {
            res.add(i);
        }

        it = equip.getItemInOffHand();
        bit = TwItemManager.getItemManager().getBaseItem(it);
        if (SlotUtil.offHandJudge(getSlot(it)) && bit instanceof Item i && isValidItem(it)) {
            res.add(i);
        }

        it = equip.getHelmet();
        bit = TwItemManager.getItemManager().getBaseItem(it);
        if (SlotUtil.helmetJudge(getSlot(it)) && bit instanceof Item i && isValidItem(it)) {
            res.add(i);
        }

        it = equip.getChestplate();
        bit = TwItemManager.getItemManager().getBaseItem(it);
        if (SlotUtil.chestJudge(getSlot(it)) && bit instanceof Item i && isValidItem(it)) {
            res.add(i);
        }

        it = equip.getLeggings();
        bit = TwItemManager.getItemManager().getBaseItem(it);
        if (SlotUtil.legsJudge(getSlot(it)) && bit instanceof Item i && isValidItem(it)) {
            res.add(i);
        }

        it = equip.getBoots();
        bit = TwItemManager.getItemManager().getBaseItem(it);
        if (SlotUtil.bootsJudge(getSlot(it)) && bit instanceof Item i && isValidItem(it)) {
            res.add(i);
        }

        return res;
    }

    /**
     * 获取实体装备的内部ItemPDC
     */
    public static List<CalculablePDC> getActiveEquipmentItemPDC(@NotNull LivingEntity living) {
        EntityEquipment equip = living.getEquipment();
        if (equip == null) return List.of();

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
     * 遍历目标的饰品
     */
    @Contract(pure = true)
    public static @NotNull @Unmodifiable List<AttributePDC> getEffectiveAccessoryAttributePDC(@NotNull LivingEntity entity) {
        return List.of();
    }

    /**
     * 检查是否是装备栏
     * @param slot 槽位
     * @return 是否是装备栏
     */
    public static boolean isArmorSlot(int slot) {
        return slot >= 36 && slot <= 39; // 头盔、胸甲、护腿、靴子
    }

    /**
     * 检查是否是手部栏
     * @param slot 槽位
     * @return 是否是手部栏
     */
    public static boolean isHandSlot(int slot) {
        return slot == 40 || slot == 41; // 主手、副手
    }

    /**
     * TODO 检查物品耐久
     */
    public static boolean isValidItem(@Nullable ItemStack item) {
        return true;
    }
}
