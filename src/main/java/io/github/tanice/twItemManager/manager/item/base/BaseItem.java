package io.github.tanice.twItemManager.manager.item.base;

import io.github.tanice.twItemManager.manager.pdc.type.AttributeCalculateSection;
import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

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
    /** 原始描述 */
    protected List<String> lore;
    /** 描述模板名 */
    @Getter
    protected String loreTemplateName;

    /**
     * 依据内部名称和对应的config文件创建物品
     * @param innerName 客制化物品内部名称
     * @param config 对应的配置文件部分
     */
    public BaseItem(@NotNull String innerName, @NotNull ConfigurationSection config) {
        this.innerName = innerName;
        lore = config.getStringList(LORE);
        loreTemplateName = config.getString(LORE_TEMPLATE,"");
        generate(config);
    }

    protected void generate(@NotNull ConfigurationSection config) {
        Material material = loadMaterial(config);
        if (material == null) return;
        /* 子类方法1 加载自身类中的属性*/
        loadUnchangeableVar(config);
        item = new ItemStack(material, config.getInt(AMOUNT, 1));
        ItemMeta meta = item.getItemMeta();
        setInnerName(meta, innerName);
        /* 加载物品基础信息 */
        loadBase(meta, config);
        /* 加载PDC属性 */
        loadPDCs(meta, config);
        item.setItemMeta(meta);
    }

    /**
     * 根据配置文件识别原版物品id
     * @param config 配置文件示例
     * @return 合法原版物品
     */
    protected @Nullable Material loadMaterial(@NotNull ConfigurationSection config) {
        String id = config.getString(ORI_MATERIAL);
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
    protected abstract void loadUnchangeableVar(@NotNull ConfigurationSection config);
    /**
     * 读取物品基础信息(原版)
     */
    protected abstract void loadBase(@NotNull ItemMeta meta, @NotNull ConfigurationSection config);
    /**
     * 读取物品属性内容(CalculablePDC)
     */
    protected abstract void loadPDCs(@NotNull ItemMeta meta, @NotNull ConfigurationSection config);

    /**
     * 获取物品实例
     * @return 返回物品实例可变副本(没有此物品则会返回AIR)
     */
    public @NotNull ItemStack getItem() {
        /* 重新生成时间戳 */
        if (item.getType() != Material.AIR) setTimeStamp(item);;
        return item.clone();
    }

    /**
     * 获取配置文件中原始的描述
     */
    public @NotNull List<String> getLore() {return new ArrayList<>(lore);}
}
