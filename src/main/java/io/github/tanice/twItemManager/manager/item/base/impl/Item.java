package io.github.tanice.twItemManager.manager.item.base.impl;

import io.github.tanice.twItemManager.infrastructure.PDCAPI;
import io.github.tanice.twItemManager.manager.item.base.BaseItem;
import io.github.tanice.twItemManager.manager.pdc.impl.ItemPDC;
import io.github.tanice.twItemManager.manager.pdc.type.AttributeCalculateSection;
import io.github.tanice.twItemManager.manager.pdc.type.DamageType;
import io.github.tanice.twItemManager.util.MiniMessageUtil;
import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import static io.github.tanice.twItemManager.constance.key.AttributeKey.*;
import static io.github.tanice.twItemManager.constance.key.ConfigKey.*;
import static io.github.tanice.twItemManager.infrastructure.PDCAPI.*;
import static io.github.tanice.twItemManager.util.Logger.logWarning;

/**
 * 插件中的武器、护甲
 */
public class Item extends BaseItem {
    /** 技能组名 */
    private List<String> skills;
    /** 可选品质组组名 */
    private List<String> qualityGroups;
    /** 等级模板名 */
    @Getter
    private String levelTemplateName;
    /** 是否取消伤害 */
    @Getter
    private boolean cancelDamage;
    /** 耐久消失是否销毁物品 */
    @Getter
    private boolean loseWhenBreak;
    /** 是否进行灵魂绑定 */
    @Getter
    private boolean soulBind;
    /** 宝石槽数量 */
    @Getter
    private Integer gemStackNumber;
    /** 所属套装 */
    @Getter
    private String setName;

    /** 物品需要有伤害类型 */
    @Getter
    private DamageType damageType;

    /** 持有自带buff */
    @Getter
    private List<String> holdBuffs;
    /** 攻击生效buff */
    @Getter
    private List<String> attackBuffs;
    /** 受击生效buff */
    @Getter
    private List<String> defenseBuffs;

    /**
     * 依据内部名称和对应的config文件创建mc基础物品
     */
    public Item(@NotNull String innerName, @NotNull ConfigurationSection config) {
        super(innerName, config);
    }

    @Override
    protected void loadClassValues() {
        skills = cfg.getStringList(SKILLS);
        qualityGroups = cfg.getStringList(QUALITY_GROUPS);
        cancelDamage = cfg.getBoolean(CANCEL_DAMAGE, false);
        loseWhenBreak = cfg.getBoolean(LOSE_WHEN_BREAK, false);
        soulBind = cfg.getBoolean(SOUL_BIND, false);
        levelTemplateName = cfg.getString(LEVEL_TEMPLATE_NAME, "");
        gemStackNumber = cfg.getInt(GEM_STACK_NUMBER, 0);
        setName = cfg.getString(SET_NAME, "");
        damageType = DamageType.valueOf(cfg.getString(DAMAGE_TYPE, "OTHER").toUpperCase());
        holdBuffs = cfg.getStringList(HOLD_BUFF);
        attackBuffs = cfg.getStringList(ATTACK_BUFF);
        defenseBuffs = cfg.getStringList(DEFENCE_BUFF);
    }

    /**
     * 读取物品基础信息
     */
    @Override
    protected void loadBase(@NotNull ItemMeta meta) {
        meta.displayName(MiniMessageUtil.deserialize(cfg.getString(DISPLAY_NAME,"")));
        meta.setMaxStackSize(cfg.getInt(MAX_STACK, 1));
        meta.setUnbreakable(true);
        int i = cfg.getInt(CUSTOM_MODEL_DATA);
        if (i != 0) meta.setCustomModelData(i);
        for (String flagName : cfg.getStringList(HIDE_FLAGS)) meta.addItemFlags(ItemFlag.valueOf("HIDE_" + flagName.toUpperCase()));
        i = cfg.getInt(MAX_DURABILITY, -1);
        setMaxDamage(meta, i);
        setCurrentDamage(meta, i);
        if (cfg.contains(COLOR)){
            if (meta instanceof LeatherArmorMeta) ((LeatherArmorMeta) meta).setColor(MiniMessageUtil.gethexColor(cfg.getString(COLOR)));
            else logWarning(innerName + "不是皮革制品，颜色无效。");
        }
    }

    @Override
    protected void loadPDCs(@NotNull ItemMeta meta) {
        /* PDC默认值 */
        if (!setCalculablePDC(meta, new ItemPDC(innerName, AttributeCalculateSection.BASE, cfg))) {
            logWarning("ItemPDC 设置出错");
        }
        setSlot(meta ,cfg.getString(SLOT,"ANY"));
        /* 灵魂绑定的玩家名(暂时不使用UUID) */
        setOwner(meta, "");
        updateUpdateCode(meta);
    }

    @Override
    public @NotNull List<String> selfUpdate(@NotNull ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        /* 基础信息重载 */
        loadBase(meta);

        /* PDC 不能直接设置 */
        ItemPDC prePDC = (ItemPDC) PDCAPI.getCalculablePDC(item);
        ItemPDC newPDC = (ItemPDC) PDCAPI.getCalculablePDC(this.item);
        if (newPDC == null) {
            logWarning("物品更新后找不到对应的底层: " + this.innerName);
            return List.of();
        }
        List<String> externalGems = newPDC.inheritanceFrom(prePDC);
        
        /* 原版属性操作 */
        if (prePDC != null) prePDC.removeOriAttrsFrom(item);
        newPDC.attachOriAttrsTo(item);
        PDCAPI.setCalculablePDC(item, newPDC);

        item.setItemMeta(meta);
        /* 多余的宝石给予玩家 */
        return externalGems;
    }

    public @NotNull List<String> getSkills() {return new ArrayList<>(skills);}

    public @NotNull List<String> getQualityGroups() {return new ArrayList<>(qualityGroups);}
}
