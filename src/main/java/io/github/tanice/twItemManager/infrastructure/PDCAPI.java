package io.github.tanice.twItemManager.infrastructure;

import io.github.tanice.twItemManager.TwItemManager;
import io.github.tanice.twItemManager.manager.pdc.CalculablePDC;
import io.github.tanice.twItemManager.manager.pdc.impl.ItemPDC;
import io.github.tanice.twItemManager.manager.pdc.type.AttributeAdditionFromType;
import io.papermc.paper.persistence.PersistentDataContainerView;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static io.github.tanice.twItemManager.util.serialize.OriSerializationUtil.serialize;
import static io.github.tanice.twItemManager.util.serialize.OriSerializationUtil.deserialize;

/**
 * PDCAPI 依赖paperAPI向游戏内实体或者物品注入PDC
 * 所有涉及到PDC的操作都通过此类实现
 */
public class PDCAPI {

    private static final Plugin PDC_NAMESPACE = TwItemManager.getInstance();
    
    private static final String ITEM_PDC_DATA_KEY_PREFIX = "TwItemManager-PDC-";
    private static final String INNER_NAME_KEY = "inner-name";
    private static final String OWNER_KEY = "owner";
    private static final String UPDATE_CODE_KEY = "update-code";
    private static final String BROKEN_KEY = "broken";
    private static final String TIME_STAMP_KEY = "time-stamp";
    private static final String CONSUMABLE_KEY = "consumable";
    private static final String SLOT_KEY = "slot";

//    /**
//     * 更改计算用的PDC属性
//     */
//    public static @Nullable ItemPDC getItemPDC(@NotNull ItemStack item) {
//        return getItemPDC(item.getItemMeta());
//    }
//
//    public static @Nullable ItemPDC getItemPDC(@NotNull ItemMeta meta) {
//        byte[] dataBytes = meta.getPersistentDataContainer().get(
//                new NamespacedKey(PDC_NAMESPACE, ITEM_PDC_DATA_KEY),
//                PersistentDataType.BYTE_ARRAY
//        );
//        if (dataBytes == null) return null;
//        return deserialize(dataBytes, ItemPDC.class);
//    }
//
//    public static void setItemPDC(@NotNull ItemStack item, @NotNull ItemPDC aPDC){
//        ItemMeta meta = item.getItemMeta();
//        setItemPDC(meta, aPDC);
//        item.setItemMeta(meta);
//    }
//
//    public static void setItemPDC(@NotNull ItemMeta meta, @NotNull ItemPDC aPDC) {
//        byte[] dataBytes = serialize(aPDC);
//        if (dataBytes == null) return;
//        meta.getPersistentDataContainer().set(
//                new NamespacedKey(PDC_NAMESPACE, ITEM_PDC_DATA_KEY),
//                PersistentDataType.BYTE_ARRAY,
//                dataBytes
//        );
//    }

    public static @Nullable Object getCalculablePDC(@NotNull ItemStack item, @NotNull AttributeAdditionFromType aft) {
        ItemMeta meta = item.getItemMeta();
        return getCalculablePDC(meta, aft);
    }

    public static @Nullable Object getCalculablePDC(@NotNull ItemMeta meta, @NotNull AttributeAdditionFromType aft) {
        byte[] dataBytes = meta.getPersistentDataContainer().get(
           new NamespacedKey(PDC_NAMESPACE, ITEM_PDC_DATA_KEY_PREFIX + aft),
           PersistentDataType.BYTE_ARRAY
        );
        if (dataBytes == null) return null;
        return deserialize(dataBytes);
    }

    public static boolean setCalculablePDC(@NotNull ItemStack item, @NotNull CalculablePDC cPDC) {
        ItemMeta meta = item.getItemMeta();
        if (!setCalculablePDC(meta, cPDC)) return false;
        item.setItemMeta(meta);
        return true;
    }

    public static boolean setCalculablePDC(@NotNull ItemMeta meta, @NotNull CalculablePDC cPDC) {
        byte[] dataBytes = serialize(cPDC);
        meta.getPersistentDataContainer().set(
                new NamespacedKey(PDC_NAMESPACE, ITEM_PDC_DATA_KEY_PREFIX + cPDC.fromType()),
                PersistentDataType.BYTE_ARRAY,
                dataBytes
        );
        return true;
    }

    /**
     * 获取物品内部名称
     */
    public static @Nullable String getInnerName(@NotNull ItemStack item) {
        return getInnerName(item.getItemMeta());
    }

    public static @Nullable String getInnerName(@NotNull ItemMeta meta) {
        return meta.getPersistentDataContainer().get(
                new NamespacedKey(PDC_NAMESPACE, INNER_NAME_KEY),
                PersistentDataType.STRING
        );
    }

    public static void setInnerName(@NotNull ItemStack item, @NotNull String innerName){
        ItemMeta meta = item.getItemMeta();
        setInnerName(meta, innerName);
        item.setItemMeta(meta);
    }

    public static void setInnerName(@NotNull ItemMeta meta, @NotNull String innerName){
        meta.getPersistentDataContainer().set(
                new NamespacedKey(PDC_NAMESPACE, INNER_NAME_KEY),
                PersistentDataType.STRING,
                innerName
        );
    }

    /**
     * 获取物品的灵魂绑定对象的ID
     */
    public static @Nullable String getOwner(@NotNull ItemStack item) {
        return getOwner(item.getItemMeta());
    }

    public static @Nullable String getOwner(@NotNull ItemMeta meta) {
        return meta.getPersistentDataContainer().get(
                new NamespacedKey(PDC_NAMESPACE, OWNER_KEY),
                PersistentDataType.STRING
        );
    }

    public static void setOwner(@NotNull ItemStack item, @NotNull String owner) {
        ItemMeta meta = item.getItemMeta();
        setOwner(meta, owner);
        item.setItemMeta(meta);
    }

    public static void setOwner(@NotNull ItemMeta meta, @NotNull String owner) {
        meta.getPersistentDataContainer().set(
                new NamespacedKey(PDC_NAMESPACE, OWNER_KEY),
                PersistentDataType.STRING,
                owner
        );
    }

    /**
     * 获取物品的更新code
     */
    public static @Nullable Double getUpdateCode(@NotNull ItemStack item) {
        return getUpdateCode(item.getItemMeta());
    }

    public static @Nullable Double getUpdateCode(@NotNull ItemMeta meta) {
        return meta.getPersistentDataContainer().get(
                new NamespacedKey(PDC_NAMESPACE, UPDATE_CODE_KEY),
                PersistentDataType.DOUBLE
        );
    }

    public static void setUpdateCode(@NotNull ItemStack item, @NotNull Double code) {
        ItemMeta meta = item.getItemMeta();
        setUpdateCode(meta, code);
        item.setItemMeta(meta);
    }

    public static void setUpdateCode(@NotNull ItemMeta meta, @NotNull Double code) {
        meta.getPersistentDataContainer().set(
                new NamespacedKey(PDC_NAMESPACE, UPDATE_CODE_KEY),
                PersistentDataType.DOUBLE,
                code
        );
    }

    /**
     * 物品是否损坏  用于算坏不丢失的事件监听
     */
    public static boolean isBroken(@NotNull ItemStack item) {
        return isBroken(item.getItemMeta());
    }

    public static boolean isBroken(@NotNull ItemMeta meta) {
        PersistentDataContainerView p = meta.getPersistentDataContainer();
        Object obj = p.get(new NamespacedKey(PDC_NAMESPACE, BROKEN_KEY), PersistentDataType.BOOLEAN);
        if (obj == null) return false;
        return obj.equals(Boolean.TRUE);
    }

    public static void setBroken(@NotNull ItemStack item, @NotNull Boolean broken) {
        ItemMeta meta = item.getItemMeta();
        setBroken(meta, broken);
        item.setItemMeta(meta);
    }

    public static void setBroken(@NotNull ItemMeta meta, @NotNull Boolean broken) {
        meta.getPersistentDataContainer().set(
                new NamespacedKey(PDC_NAMESPACE, BROKEN_KEY),
                PersistentDataType.BOOLEAN,
                broken
        );
    }

    /**
     * 属性生效槽位
     */
    public static @Nullable String getSlot(@NotNull ItemStack item) {
        return getSlot(item.getItemMeta());
    }

    public static @Nullable String getSlot(@NotNull ItemMeta meta) {
        return meta.getPersistentDataContainer().get(
                new NamespacedKey(PDC_NAMESPACE, SLOT_KEY),
                PersistentDataType.STRING
        );
    }

    public static void setSlot(@NotNull ItemStack item, @NotNull String slot) {
        ItemMeta meta = item.getItemMeta();
        setSlot(meta , slot);
        item.setItemMeta(meta);
    }

    public static void setSlot(@NotNull ItemMeta meta, @NotNull String slot) {
        meta.getPersistentDataContainer().set(
                new NamespacedKey(PDC_NAMESPACE, SLOT_KEY),
                PersistentDataType.STRING,
                slot
        );
    }

    /**
     * 品质相关
     */
    public static @NotNull String getQualityName(@NotNull ItemStack item) {
        return getQualityName(item.getItemMeta());
    }
    
    public static @NotNull String getQualityName(@NotNull ItemMeta meta) {
        ItemPDC itemPDC = (ItemPDC) getCalculablePDC(meta, AttributeAdditionFromType.ITEM);
        if (itemPDC == null) return "";
        return itemPDC.getQualityName();
    }
    
    public static boolean setQualityName(@NotNull ItemStack item, @NotNull String qualityInnerName) {
        ItemMeta meta = item.getItemMeta();
        if (!setQualityName(meta, qualityInnerName)) return false;
        item.setItemMeta(meta);
        return true;
    }
    
    public static boolean setQualityName(@NotNull ItemMeta meta, @NotNull String qualityInnerName) {
        ItemPDC itemPDC = (ItemPDC) getCalculablePDC(meta, AttributeAdditionFromType.ITEM);
        if (itemPDC == null) return false;
        itemPDC.setQualityName(qualityInnerName);
        setCalculablePDC(meta, itemPDC);
        return true;
    }

    /**
     * 宝石相关操作
     */
    public static String @Nullable [] getGems(@NotNull ItemStack item) {
        return getGems(item.getItemMeta());
    }

    public static String @Nullable [] getGems(@NotNull ItemMeta meta) {
        ItemPDC itemPDC = (ItemPDC) getCalculablePDC(meta, AttributeAdditionFromType.ITEM);
        if (itemPDC == null) return null;
        return itemPDC.getGems();
    }

    public static boolean addGem(@NotNull ItemStack item, @NotNull String gemInnerName) {
        ItemMeta meta = item.getItemMeta();
        if (!addGem(meta, gemInnerName)) return false;
        item.setItemMeta(meta);
        return true;
    }

    public static boolean addGem(@NotNull ItemMeta meta, @NotNull String gemInnerName) {
        ItemPDC itemPDC = (ItemPDC) getCalculablePDC(meta, AttributeAdditionFromType.ITEM);
        if (itemPDC == null) return false;
        if (!itemPDC.addGem(gemInnerName)) return false;
        setCalculablePDC(meta, itemPDC);
        return true;
    }

    /**
     * 原本没有对应的宝石，若使用移除，会返回false
     */
    public static boolean removeGem(@NotNull ItemStack item, @NotNull String gemInnerName) {
        ItemMeta meta = item.getItemMeta();
        if (!removeGem(meta, gemInnerName)) return false;
        item.setItemMeta(meta);
        return true;
    }

    /**
     * 原本没有对应的宝石，若使用移除，会返回false
     */
    public static boolean removeGem(@NotNull ItemMeta meta, String gemInnerName) {
        ItemPDC itemPDC = (ItemPDC) getCalculablePDC(meta, AttributeAdditionFromType.ITEM);
        if (itemPDC == null) return false;
        if (!itemPDC.removeGem(gemInnerName)) return false;
        setCalculablePDC(meta, itemPDC);
        return true;
    }

    /**
     * 直接清除物品的所有宝石(槽位留存)
     */
    public static boolean emptyGems(@NotNull ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (!emptyGems(meta)) return false;
        item.setItemMeta(meta);
        return true;
    }

    public static boolean emptyGems(@NotNull ItemMeta meta) {
        ItemPDC itemPDC = (ItemPDC) getCalculablePDC(meta, AttributeAdditionFromType.ITEM);
        if (itemPDC == null) return true;
        if (!itemPDC.emptyGems()) return false;
        setCalculablePDC(meta, itemPDC);
        return true;
    }

    /**
     * 等级相关操作
     */
    public static @NotNull Integer getLevel(@NotNull ItemStack item) {
        return getLevel(item.getItemMeta());
    }

    public static @NotNull Integer getLevel(@NotNull ItemMeta meta) {
        ItemPDC itemPDC = (ItemPDC) getCalculablePDC(meta, AttributeAdditionFromType.ITEM);
        if (itemPDC == null) return 0;
        return itemPDC.getLevel();
    }

    public static boolean setLevel(@NotNull ItemStack item, int level) {
        ItemMeta meta = item.getItemMeta();
        if (!setLevel(meta, level)) return false;
        item.setItemMeta(meta);
        return true;
    }

    public static boolean setLevel(@NotNull ItemMeta meta, int level) {
        ItemPDC itemPDC = (ItemPDC) getCalculablePDC(meta, AttributeAdditionFromType.ITEM);
        if (itemPDC == null) return false;
        itemPDC.setLevel(level);
        setCalculablePDC(meta, itemPDC);
        return true;
    }

    /**
     * 不会检测合法性，统一在属性绑定时检测
     */
    public static boolean levelUp(@NotNull ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (!levelUp(meta)) return false;
        item.setItemMeta(meta);
        return true;
    }

    /**
     * 不会检测合法性，统一在属性绑定时检测
     */
    public static boolean levelUp(@NotNull ItemMeta meta) {
        ItemPDC itemPDC = (ItemPDC) getCalculablePDC(meta, AttributeAdditionFromType.ITEM);
        if (itemPDC == null) return false;
        itemPDC.levelUp();
        return true;
    }

    /**
     * 不会检测合法性，统一在属性绑定时检测
     */
    public static boolean levelDown(@NotNull ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (!levelDown(meta)) return false;
        item.setItemMeta(meta);
        return true;
    }

    /**
     * 不会检测合法性，统一在属性绑定时检测
     */
    public static boolean levelDown(@NotNull ItemMeta meta) {
        ItemPDC itemPDC = (ItemPDC) getCalculablePDC(meta, AttributeAdditionFromType.ITEM);
        if (itemPDC == null) return false;
        itemPDC.levelDown();
        setCalculablePDC(meta, itemPDC);
        return true;
    }

    /**
     * 时间戳相关
     */
    public static @Nullable String getTimeStamp(@NotNull ItemStack item) {
        return item.getPersistentDataContainer().get(
                new NamespacedKey(PDC_NAMESPACE, TIME_STAMP_KEY),
                PersistentDataType.STRING
        );
    }

    public static @Nullable String getTimeStamp(@NotNull ItemMeta meta) {
        return meta.getPersistentDataContainer().get(
                new NamespacedKey(PDC_NAMESPACE, TIME_STAMP_KEY),
                PersistentDataType.STRING
        );
    }

    public static void setTimeStamp(@NotNull ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        setTimeStamp(meta);
        item.setItemMeta(meta);
    }

    public static void setTimeStamp(@NotNull ItemMeta meta) {
        meta.getPersistentDataContainer().set(
                new NamespacedKey(PDC_NAMESPACE, TIME_STAMP_KEY),
                PersistentDataType.STRING,
                String.valueOf(System.currentTimeMillis())
        );
    }

    /**
     * 可食用物部分
     */
    public static void setConsumable(@NotNull ItemStack item, boolean consumable) {
        ItemMeta meta = item.getItemMeta();
        setConsumable(meta, consumable);
        item.setItemMeta(meta);
    }

    public static void setConsumable(@NotNull ItemMeta meta, boolean consumable) {
        meta.getPersistentDataContainer().set(
                new NamespacedKey(PDC_NAMESPACE, CONSUMABLE_KEY),
                PersistentDataType.BOOLEAN,
                consumable
        );
    }

    public static boolean isConsumable(@NotNull ItemStack item) {
        return isConsumable(item.getItemMeta());
    }

    public static boolean isConsumable(@NotNull ItemMeta meta) {
        Object obj = meta.getPersistentDataContainer().get(
                new NamespacedKey(PDC_NAMESPACE, CONSUMABLE_KEY),
                PersistentDataType.BOOLEAN
        );
        if (obj == null) return false;
        return obj.equals(Boolean.TRUE);
    }
}
