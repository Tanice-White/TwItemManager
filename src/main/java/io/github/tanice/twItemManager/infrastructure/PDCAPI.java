package io.github.tanice.twItemManager.infrastructure;

import io.github.tanice.twItemManager.TwItemManager;
import io.github.tanice.twItemManager.config.Config;
import io.github.tanice.twItemManager.manager.pdc.CalculablePDC;
import io.github.tanice.twItemManager.manager.pdc.impl.ItemPDC;
import io.github.tanice.twItemManager.manager.pdc.impl.EntityPDC;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.LivingEntity;
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
    
    private static final String ITEM_PDC_DATA_KEY = "TwItemManager-PDC";
    private static final String INNER_NAME_KEY = "inner-name";
    private static final String OWNER_KEY = "owner";
    private static final String UPDATE_CODE_KEY = "update-code";
    private static final String TIME_STAMP_KEY = "time-stamp";
    private static final String CONSUMABLE_KEY = "consumable";
    private static final String SLOT_KEY = "slot";
    private static final String MAX_DAMAGE_KEY = "damage";
    private static final String CURRENT_DAMAGE_KEY = "current-damage";

    public static @Nullable CalculablePDC getCalculablePDC(@NotNull ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;
        return getCalculablePDC(meta);
    }

    public static @Nullable CalculablePDC getCalculablePDC(@NotNull ItemMeta meta) {
        byte[] dataBytes = meta.getPersistentDataContainer().get(
           new NamespacedKey(PDC_NAMESPACE, ITEM_PDC_DATA_KEY),
           PersistentDataType.BYTE_ARRAY
        );
        if (dataBytes == null) return null;
        return (CalculablePDC) deserialize(dataBytes);
    }

    public static @Nullable EntityPDC getCalculablePDC(@NotNull LivingEntity entity) {
        byte[] dataBytes =  entity.getPersistentDataContainer().get(
                new NamespacedKey(PDC_NAMESPACE, ITEM_PDC_DATA_KEY),
                PersistentDataType.BYTE_ARRAY
        );
        if (dataBytes == null) return null;
        return (EntityPDC) deserialize(dataBytes);
    }

    public static boolean setCalculablePDC(@NotNull ItemStack item, @NotNull CalculablePDC cPDC) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        if (!setCalculablePDC(meta, cPDC)) return false;
        item.setItemMeta(meta);
        return true;
    }

    public static boolean setCalculablePDC(@NotNull ItemMeta meta, @NotNull CalculablePDC cPDC) {
        byte[] dataBytes = serialize(cPDC);
        meta.getPersistentDataContainer().set(
                new NamespacedKey(PDC_NAMESPACE, ITEM_PDC_DATA_KEY),
                PersistentDataType.BYTE_ARRAY,
                dataBytes
        );
        return true;
    }

    public static boolean setCalculablePDC(@NotNull LivingEntity entity, @NotNull EntityPDC ePDC) {
        if (ePDC.getVersion() != Config.version) {
            ePDC = new EntityPDC();
        }
        entity.getPersistentDataContainer().set(
            new NamespacedKey(PDC_NAMESPACE, ITEM_PDC_DATA_KEY),
            PersistentDataType.BYTE_ARRAY,
            serialize(ePDC)
        );
        return true;
    }

    /**
     * 获取物品内部名称
     */
    public static @Nullable String getInnerName(@Nullable ItemStack item) {
        if (item == null) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;
        return getInnerName(item.getItemMeta());
    }

    public static @Nullable String getInnerName(@Nullable ItemMeta meta) {
        if (meta == null) return null;
        return meta.getPersistentDataContainer().get(
                new NamespacedKey(PDC_NAMESPACE, INNER_NAME_KEY),
                PersistentDataType.STRING
        );
    }

    public static void setInnerName(@NotNull ItemStack item, @NotNull String innerName){
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
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
        if (meta == null) return;
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
    public static @Nullable Long getUpdateCode(@NotNull ItemStack item) {
        return getUpdateCode(item.getItemMeta());
    }

    public static @Nullable Long getUpdateCode(@NotNull ItemMeta meta) {
        return meta.getPersistentDataContainer().get(
                new NamespacedKey(PDC_NAMESPACE, UPDATE_CODE_KEY),
                PersistentDataType.LONG
        );
    }

    public static void updateUpdateCode(@NotNull ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        updateUpdateCode(meta);
        item.setItemMeta(meta);
    }

    public static void updateUpdateCode(@NotNull ItemMeta meta) {
        meta.getPersistentDataContainer().set(
                new NamespacedKey(PDC_NAMESPACE, UPDATE_CODE_KEY),
                PersistentDataType.LONG,
                TwItemManager.getUpdateCode()
        );
    }

    /**
     * 物品耐久相关
     */
    public static @Nullable Integer getCurrentDamage(@NotNull ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;
        return getCurrentDamage(meta);
    }

    public static @Nullable Integer getCurrentDamage(@NotNull ItemMeta meta) {
        return meta.getPersistentDataContainer().get(
                new NamespacedKey(PDC_NAMESPACE, CURRENT_DAMAGE_KEY),
                PersistentDataType.INTEGER
        );
    }

    public static @Nullable Integer getMaxDamage(@NotNull ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;
        return getMaxDamage(meta);
    }

    public static @Nullable Integer getMaxDamage(@NotNull ItemMeta meta) {
        return meta.getPersistentDataContainer().get(
                new NamespacedKey(PDC_NAMESPACE, MAX_DAMAGE_KEY),
                PersistentDataType.INTEGER
        );
    }

    public static void setMaxDamage(@NotNull ItemStack item, int maxDamage) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        setMaxDamage(meta, maxDamage);
        item.setItemMeta(meta);
    }

    public static void setMaxDamage(@NotNull ItemMeta meta, int maxDamage) {
        meta.getPersistentDataContainer().set(
                new NamespacedKey(PDC_NAMESPACE, MAX_DAMAGE_KEY),
                PersistentDataType.INTEGER,
                maxDamage
        );
    }

    public static void setCurrentDamage(@NotNull ItemStack item, int currentDamage) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        setCurrentDamage(meta, currentDamage);
        item.setItemMeta(meta);
    }

    public static void setCurrentDamage(@NotNull ItemMeta meta, int currentDamage) {
        meta.getPersistentDataContainer().set(
                new NamespacedKey(PDC_NAMESPACE, CURRENT_DAMAGE_KEY),
                PersistentDataType.INTEGER,
                currentDamage
        );
    }

    /**
     * 属性生效槽位
     */
    public static @Nullable String getSlot(@NotNull ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;
        return getSlot(meta);
    }

    public static @Nullable String getSlot(@NotNull ItemMeta meta) {
        return meta.getPersistentDataContainer().get(
                new NamespacedKey(PDC_NAMESPACE, SLOT_KEY),
                PersistentDataType.STRING
        );
    }

    public static void setSlot(@NotNull ItemStack item, @NotNull String slot) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
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
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return "";
        return getQualityName(meta);
    }
    
    public static @NotNull String getQualityName(@NotNull ItemMeta meta) {
        ItemPDC itemPDC = (ItemPDC) getCalculablePDC(meta);
        if (itemPDC == null) return "";
        return itemPDC.getQualityName();
    }
    
    public static boolean setQualityName(@NotNull ItemStack item, @NotNull String qualityInnerName) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        if (!setQualityName(meta, qualityInnerName)) return false;
        item.setItemMeta(meta);
        return true;
    }
    
    public static boolean setQualityName(@NotNull ItemMeta meta, @NotNull String qualityInnerName) {
        ItemPDC itemPDC = (ItemPDC) getCalculablePDC(meta);
        if (itemPDC == null) return false;
        itemPDC.setQualityName(qualityInnerName);
        setCalculablePDC(meta, itemPDC);
        return true;
    }

    /**
     * 宝石相关操作
     */
    public static String @Nullable [] getGems(@NotNull ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;
        return getGems(meta);
    }

    public static String @Nullable [] getGems(@NotNull ItemMeta meta) {
        ItemPDC itemPDC = (ItemPDC) getCalculablePDC(meta);
        if (itemPDC == null) return null;
        return itemPDC.getGems();
    }

    public static boolean addGem(@NotNull ItemStack item, @NotNull String gemInnerName) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        if (!addGem(meta, gemInnerName)) return false;
        item.setItemMeta(meta);
        return true;
    }

    public static boolean addGem(@NotNull ItemMeta meta, @NotNull String gemInnerName) {
        ItemPDC itemPDC = (ItemPDC) getCalculablePDC(meta);
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
        if (meta == null) return false;
        if (!removeGem(meta, gemInnerName)) return false;
        item.setItemMeta(meta);
        return true;
    }

    /**
     * 原本没有对应的宝石，若使用移除，会返回false
     */
    public static boolean removeGem(@NotNull ItemMeta meta, String gemInnerName) {
        ItemPDC itemPDC = (ItemPDC) getCalculablePDC(meta);
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
        if (meta == null || !emptyGems(meta)) return false;
        item.setItemMeta(meta);
        return true;
    }

    public static boolean emptyGems(@NotNull ItemMeta meta) {
        ItemPDC itemPDC = (ItemPDC) getCalculablePDC(meta);
        if (itemPDC == null) return true;
        if (!itemPDC.emptyGems()) return false;
        setCalculablePDC(meta, itemPDC);
        return true;
    }

    /**
     * 等级相关操作
     */
    public static @NotNull Integer getLevel(@NotNull ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return 0;
        return getLevel(meta);
    }

    public static @NotNull Integer getLevel(@NotNull ItemMeta meta) {
        ItemPDC itemPDC = (ItemPDC) getCalculablePDC(meta);
        if (itemPDC == null) return 0;
        return itemPDC.getLevel();
    }

    public static void setLevel(@NotNull ItemStack item, int level) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        setLevel(meta, level);
        item.setItemMeta(meta);
    }

    public static void setLevel(@NotNull ItemMeta meta, int level) {
        ItemPDC itemPDC = (ItemPDC) getCalculablePDC(meta);
        if (itemPDC == null) return;
        itemPDC.setLevel(level);
        setCalculablePDC(meta, itemPDC);
    }

    /**
     * 不会检测合法性，统一在属性绑定时检测
     */
    public static void levelUp(@NotNull ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        levelUp(meta);
        item.setItemMeta(meta);
    }

    /**
     * 不会检测合法性，统一在属性绑定时检测
     */
    public static void levelUp(@NotNull ItemMeta meta) {
        ItemPDC itemPDC = (ItemPDC) getCalculablePDC(meta);
        if (itemPDC == null) return;
        itemPDC.levelUp();
    }

    /**
     * 不会检测合法性，统一在属性绑定时检测
     */
    public static void levelDown(@NotNull ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        levelDown(meta);
        item.setItemMeta(meta);
    }

    /**
     * 不会检测合法性，统一在属性绑定时检测
     */
    public static void levelDown(@NotNull ItemMeta meta) {
        ItemPDC itemPDC = (ItemPDC) getCalculablePDC(meta);
        if (itemPDC == null) return;
        itemPDC.levelDown();
        setCalculablePDC(meta, itemPDC);
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
        if (meta == null) return;
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
        if (meta == null) return;
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
