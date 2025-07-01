package io.github.tanice.twItemManager.manager.item.base;

import io.github.tanice.twItemManager.infrastructure.PDCAPI;
import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.github.tanice.twItemManager.constance.key.ConfigKey.*;
import static io.github.tanice.twItemManager.infrastructure.PDCAPI.*;
import static io.github.tanice.twItemManager.util.Logger.logWarning;

/**
 * 物品抽象
 * 包含 生成后的物品、物品内部名、原始描述、描述模板名
 */
public abstract class BaseItem {
    protected ItemStack item = new ItemStack(Material.AIR);
    /** 内部名称 */
    @Getter
    protected final String innerName;

    /** 原始的展示名称 */
    @Getter
    protected final String displayName;
    /** 原始描述 */
    /* 会被归为 loreTemplate 中的 [Item] 关键词下 */
    protected List<String> lore;
    /** 描述模板名 */
    @Getter
    protected String loreTemplateName;

    /** 对应的config */
    protected final ConfigurationSection cfg;

    /* 额外的nbt */
    protected final Map<String, String> customNBT;

    /**
     * 依据内部名称和对应的config文件创建物品
     * @param innerName 客制化物品内部名称
     * @param cfg 对应的配置文件部分
     */
    public BaseItem(@NotNull String innerName, @NotNull ConfigurationSection cfg) {
        this.cfg = cfg;
        this.innerName = innerName;
        this.displayName = cfg.getString(DISPLAY_NAME, innerName);
        lore = cfg.getStringList(LORE);
        loreTemplateName = cfg.getString(LORE_TEMPLATE,"");
        customNBT = new HashMap<>();
        this.loadCustomNBT();
        this.generate();
    }

    /**
     * 生成物品
     */
    protected void generate() {
        Material material = loadMaterial();
        if (material == null) return;
        /* 加载自身类中的属性*/
        loadClassValues();
        item = new ItemStack(material, cfg.getInt(AMOUNT, 1));
        ItemMeta meta = item.getItemMeta();
        setInnerName(meta, innerName);
        /* 加载物品基础信息 */
        loadBase(meta);
        /* 加载PDC属性 */
        loadPDCs(meta);
        attachCustomNBT(meta);
        item.setItemMeta(meta);
    }

    /**
     * 根据配置文件识别原版物品id
     * @return 合法原版物品
     */
    protected @Nullable Material loadMaterial() {
        String id = cfg.getString(ORI_MATERIAL);
        if (id == null || id.isEmpty()) {
            logWarning("id 不允许为空! ");
            return null;
        }
        Material material = Material.matchMaterial(id);
        if(material == null) {
            logWarning("无效的原版基础物品: " + innerName);
            return null;
        }
        return material;
    }

    /**
     * 读取子类自身的不可变数据--需要储存在运行类中
     */
    protected abstract void loadClassValues();
    /**
     * 读取物品基础信息(原版)
     */
    protected abstract void loadBase(@NotNull ItemMeta meta);
    /**
     * 读取物品属性内容(CalculablePDC)
     */
    protected abstract void loadPDCs(@NotNull ItemMeta meta);

    /**
     * 读取其他插件可使用的PDC
     */
    public void loadCustomNBT() {
        ConfigurationSection sub = cfg.getConfigurationSection(CUSTOM_NBT);
        if (sub == null) return;
        String v;
        for (String key : sub.getKeys(false)) {
            v = sub.getString(key);
            if (v == null) continue;
            customNBT.put(key, v);
        }
    }

    public void attachCustomNBT(@NotNull ItemMeta meta) {
        for (Map.Entry<String, String> entry : customNBT.entrySet()) {
            if (!PDCAPI.setCustomNBT(meta, entry.getKey(), entry.getValue())) logWarning("物品: " + innerName + " 的nbt[" + entry.getKey() + ":" + entry.getValue() + "]写入错误");
        }
    }

    /**
     * 清除NBT(即PersistentDataContainer)
     */
    public void removeCustomNBT(@NotNull ItemMeta meta) {
        PDCAPI.removeAllCustomNBT(meta);
    }

    /**
     * 物品更新
     * 使用物品内部名找到对应的底层，并将新的底层覆盖过去
     * 即使用新的 <? extend BaseItem> 类更新 item
     * @param item 需要被更新的物品
     * @return 如果有多余的宝石，则卸下来并返回内部名列表
     */
    public @NotNull List<String> selfUpdate(@NotNull ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return List.of();

        removeCustomNBT(meta);

        loadBase(meta);
        loadPDCs(meta);
        attachCustomNBT(meta);
        item.setItemMeta(meta);
        return List.of();
    }

    /**
     * 获取物品实例
     * @return 返回物品实例可变副本(没有此物品则会返回AIR)
     */
    public @NotNull ItemStack getItem() {
        /* 重新生成时间戳 */
        if (item.getType() != Material.AIR) setTimeStamp(item);
        return item.clone();
    }

    /**
     * 获取配置文件中原始的描述
     */
    public @NotNull List<String> getItemLore() {return new ArrayList<>(lore);}
}
