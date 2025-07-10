package io.github.tanice.twItemManager.util;

import io.github.tanice.twItemManager.TwItemManager;
import io.github.tanice.twItemManager.manager.item.base.BaseItem;
import io.github.tanice.twItemManager.manager.item.base.impl.Item;
import io.github.tanice.twItemManager.pdc.CalculablePDC;
import io.github.tanice.twItemManager.pdc.impl.AttributePDC;
import io.github.tanice.twItemManager.pdc.impl.ItemPDC;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.github.tanice.twItemManager.infrastructure.PDCAPI.getCalculablePDC;
import static io.github.tanice.twItemManager.infrastructure.PDCAPI.getSlot;

public class EquipmentUtil {
    // TODO 原版护甲的护甲值按照正常的提供
    private static final Map<Material, AttributePDC> ORI_ITEM_ATTRIBUTES = new HashMap<>();

    /**
     * 获取实体装备的内部 Item
     * 用于获取 buff
     */
    public static List<Item> getActiveEquipmentItem(@NotNull LivingEntity living) {
        EntityEquipment equip = living.getEquipment();
        if (equip == null) return List.of();

        List<Item> res = new ArrayList<>(12);

        ItemStack it;
        BaseItem bit;
        it = equip.getItemInMainHand();
        bit = TwItemManager.getItemManager().getBaseItem(it);
        if (bit instanceof Item i && SlotUtil.mainHandJudge(getSlot(it)) && isValidItem(it)) {
            res.add(i);
        }

        it = equip.getItemInOffHand();
        bit = TwItemManager.getItemManager().getBaseItem(it);
        if (bit instanceof Item i && SlotUtil.offHandJudge(getSlot(it)) && isValidItem(it)) {
            res.add(i);
        }

        it = equip.getHelmet();
        bit = TwItemManager.getItemManager().getBaseItem(it);
        if (bit instanceof Item i && SlotUtil.helmetJudge(getSlot(it)) && isValidItem(it)) {
            res.add(i);
        }

        it = equip.getChestplate();
        bit = TwItemManager.getItemManager().getBaseItem(it);
        if (bit instanceof Item i && SlotUtil.chestJudge(getSlot(it)) && isValidItem(it)) {
            res.add(i);
        }

        it = equip.getLeggings();
        bit = TwItemManager.getItemManager().getBaseItem(it);
        if (bit instanceof Item i && SlotUtil.legsJudge(getSlot(it)) && isValidItem(it)) {
            res.add(i);
        }

        it = equip.getBoots();
        bit = TwItemManager.getItemManager().getBaseItem(it);
        if (bit instanceof Item i && SlotUtil.bootsJudge(getSlot(it)) && isValidItem(it)) {
            res.add(i);
        }

        return res;
    }

    /**
     * 获取实体装备的内部 CalculablePDC
     * 用于计算属性
     */
    public static List<CalculablePDC> getActiveEquipmentCalculatePDC(@NotNull LivingEntity living) {
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
        // 手上的护甲不能生效

        it = equip.getItemInOffHand();
        if (SlotUtil.offHandJudge(getSlot(it))) {
            cp = getCalculablePDC(it);
            if (cp != null) {
                if (cp instanceof ItemPDC) ((ItemPDC) cp).selfCalculate();
                res.add(cp);
            }
        } else {
            AttributePDC a = ORI_ITEM_ATTRIBUTES.get(it.getType());
            if (a != null) res.add(a);
        }

        it = equip.getHelmet();
        if (it != null && SlotUtil.helmetJudge(getSlot(it))) {
            cp = getCalculablePDC(it);
            if (cp != null) {
                if (cp instanceof ItemPDC) ((ItemPDC) cp).selfCalculate();
                res.add(cp);
            }
        } else {
            if (it != null) {
                AttributePDC a = ORI_ITEM_ATTRIBUTES.get(it.getType());
                if (a != null) res.add(a);
            }
        }

        it = equip.getChestplate();
        if (it != null && SlotUtil.chestJudge(getSlot(it))) {
            cp = getCalculablePDC(it);
            if (cp != null) {
                if (cp instanceof ItemPDC) ((ItemPDC) cp).selfCalculate();
                res.add(cp);
            }
        } else {
            if (it != null) {
                AttributePDC a = ORI_ITEM_ATTRIBUTES.get(it.getType());
                if (a != null) res.add(a);
            }
        }


        it = equip.getLeggings();
        if (it != null && SlotUtil.legsJudge(getSlot(it))) {
            cp = getCalculablePDC(it);
            if (cp != null) {
                if (cp instanceof ItemPDC) ((ItemPDC) cp).selfCalculate();
                res.add(cp);
            }
        } else {
            if (it != null) {
                AttributePDC a = ORI_ITEM_ATTRIBUTES.get(it.getType());
                if (a != null) res.add(a);
            }
        }

        it = equip.getBoots();
        if (it != null && SlotUtil.bootsJudge(getSlot(it))) {
            cp = getCalculablePDC(it);
            if (cp != null) {
                if (cp instanceof ItemPDC) ((ItemPDC) cp).selfCalculate();
                res.add(cp);
            }
        } else {
            if (it != null) {
                AttributePDC a = ORI_ITEM_ATTRIBUTES.get(it.getType());
                if (a != null) res.add(a);
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

    /* 原版防护类物品的PDC */
    static {
        // ===================== 海龟壳 =====================
        ORI_ITEM_ATTRIBUTES.put(Material.TURTLE_HELMET, new AttributePDC("turtle_helmet", 0, 2, 0));

        // ===================== 皮革 =====================
        ORI_ITEM_ATTRIBUTES.put(Material.LEATHER_HELMET, new AttributePDC("leather_helmet", 0, 1, 0));
        ORI_ITEM_ATTRIBUTES.put(Material.LEATHER_CHESTPLATE, new AttributePDC("leather_chestplate", 0, 3, 0));
        ORI_ITEM_ATTRIBUTES.put(Material.LEATHER_LEGGINGS, new AttributePDC("leather_leggings", 0, 2, 0));
        ORI_ITEM_ATTRIBUTES.put(Material.LEATHER_BOOTS, new AttributePDC("leather_boots", 0, 1, 0));

        // ===================== 锁链 =====================
        ORI_ITEM_ATTRIBUTES.put(Material.CHAINMAIL_HELMET, new AttributePDC("chainmail_helmet", 0, 2, 0));
        ORI_ITEM_ATTRIBUTES.put(Material.CHAINMAIL_CHESTPLATE, new AttributePDC("chainmail_chestplate", 0, 5, 0));
        ORI_ITEM_ATTRIBUTES.put(Material.CHAINMAIL_LEGGINGS, new AttributePDC("chainmail_leggings", 0, 4, 0));
        ORI_ITEM_ATTRIBUTES.put(Material.CHAINMAIL_BOOTS, new AttributePDC("chainmail_boots", 0, 1, 0));

        // ===================== 铁 =====================
        ORI_ITEM_ATTRIBUTES.put(Material.IRON_HELMET, new AttributePDC("iron_helmet", 0, 2, 0));
        ORI_ITEM_ATTRIBUTES.put(Material.IRON_CHESTPLATE, new AttributePDC("iron_chestplate", 0, 6, 0));
        ORI_ITEM_ATTRIBUTES.put(Material.IRON_LEGGINGS, new AttributePDC("iron_leggings", 0, 5, 0));
        ORI_ITEM_ATTRIBUTES.put(Material.IRON_BOOTS, new AttributePDC("iron_boots", 0, 2, 0));

        // ===================== 金 =====================
        ORI_ITEM_ATTRIBUTES.put(Material.GOLDEN_HELMET, new AttributePDC("golden_helmet", 0, 2, 0));
        ORI_ITEM_ATTRIBUTES.put(Material.GOLDEN_CHESTPLATE, new AttributePDC("golden_chestplate", 0, 5, 0));
        ORI_ITEM_ATTRIBUTES.put(Material.GOLDEN_LEGGINGS, new AttributePDC("golden_leggings", 0, 3, 0));
        ORI_ITEM_ATTRIBUTES.put(Material.GOLDEN_BOOTS, new AttributePDC("golden_boots", 0, 1, 0));

        // ===================== 钻石 =====================
        ORI_ITEM_ATTRIBUTES.put(Material.DIAMOND_HELMET, new AttributePDC("diamond_helmet", 0, 3, 2));
        ORI_ITEM_ATTRIBUTES.put(Material.DIAMOND_CHESTPLATE, new AttributePDC("diamond_chestplate", 0, 8, 2));
        ORI_ITEM_ATTRIBUTES.put(Material.DIAMOND_LEGGINGS, new AttributePDC("diamond_leggings", 0, 6, 2));
        ORI_ITEM_ATTRIBUTES.put(Material.DIAMOND_BOOTS, new AttributePDC("diamond_boots", 0, 3, 2));

        // ===================== 下届合金 =====================
        ORI_ITEM_ATTRIBUTES.put(Material.NETHERITE_HELMET, new AttributePDC("netherite_helmet", 0, 3, 3));
        ORI_ITEM_ATTRIBUTES.put(Material.NETHERITE_CHESTPLATE, new AttributePDC("netherite_chestplate", 0, 8, 3));
        ORI_ITEM_ATTRIBUTES.put(Material.NETHERITE_LEGGINGS, new AttributePDC("netherite_leggings", 0, 6, 3));
        ORI_ITEM_ATTRIBUTES.put(Material.NETHERITE_BOOTS, new AttributePDC("netherite_boots", 0, 3, 3));

        // ===================== 其他 =====================
        ORI_ITEM_ATTRIBUTES.put(Material.LEATHER_HORSE_ARMOR, new AttributePDC("leather_horse_armor", 0, 3, 0));
        ORI_ITEM_ATTRIBUTES.put(Material.IRON_HORSE_ARMOR, new AttributePDC("iron_horse_armor", 0, 5, 3));
        ORI_ITEM_ATTRIBUTES.put(Material.GOLDEN_HORSE_ARMOR, new AttributePDC("leather_horse_armor", 0, 7, 3));
        ORI_ITEM_ATTRIBUTES.put(Material.DIAMOND_HORSE_ARMOR, new AttributePDC("leather_horse_armor", 0, 11, 2));
        ORI_ITEM_ATTRIBUTES.put(Material.WOLF_ARMOR, new AttributePDC("wolf_armor", 0, 11, 0));
    }
}
