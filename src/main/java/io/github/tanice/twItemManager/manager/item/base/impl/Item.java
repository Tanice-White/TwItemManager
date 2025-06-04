package io.github.tanice.twItemManager.manager.item.base.impl;

import io.github.tanice.twItemManager.TwItemManager;
import io.github.tanice.twItemManager.manager.item.base.BaseItem;
import io.github.tanice.twItemManager.manager.pdc.impl.ItemPDC;
import io.github.tanice.twItemManager.manager.pdc.type.AttributeCalculateSection;
import io.github.tanice.twItemManager.manager.pdc.type.DamageType;
import io.github.tanice.twItemManager.util.MiniMessageUtil;
import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemFlag;
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
    /** 模板名 */
    @Getter
    private String levelTemplateName;  //TODO 冗余，PDC中也有levelTemplateName，需要删除PDC中的
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
    /** 攻击挂载buff */
    @Getter
    private List<String> attackBuffs;

    /**
     * 依据内部名称和对应的config文件创建mc基础物品
     */
    public Item(@NotNull String innerName, @NotNull ConfigurationSection config) {
        super(innerName, config);
        this.skills = new ArrayList<>();
        this.qualityGroups = new ArrayList<>();
        this.holdBuffs = new ArrayList<>();
        this.attackBuffs = new ArrayList<>();
        generate(config);
    }

    @Override
    protected void loadUnchangeableVar(@NotNull ConfigurationSection config) {
        skills = config.getStringList(SKILLS);
        qualityGroups = config.getStringList(QUALITY_GROUPS);
        cancelDamage = config.getBoolean(CANCEL_DAMAGE, false);
        loseWhenBreak = config.getBoolean(LOSE_WHEN_BREAK, false);
        soulBind = config.getBoolean(SOUL_BIND, false);
        levelTemplateName = config.getString(LEVEL_TEMPLATE_NAME, "");
        gemStackNumber = config.getInt(GEM_STACK_NUMBER, 0);
        setName = config.getString(SET_NAME, "");
        damageType = DamageType.valueOf(config.getString(DAMAGE_TYPE, "OTHER").toUpperCase());
        holdBuffs = config.getStringList(HOLD_BUFF);
        attackBuffs = config.getStringList(ATTACK_BUFF);
    }

    /**
     * 读取物品基础信息
     */
    @Override
    protected void loadBase(@NotNull ItemMeta meta, @NotNull ConfigurationSection config) {
        meta.displayName(MiniMessageUtil.deserialize(config.getString(DISPLAY_NAME,"")));
        meta.setMaxStackSize(config.getInt(MAX_STACK, 1));
        meta.setUnbreakable(true);
        int i = config.getInt(CUSTOM_MODEL_DATA);
        if (i != 0) meta.setCustomModelData(i);
        for (String flagName : config.getStringList(HIDE_FLAGS)) meta.addItemFlags(ItemFlag.valueOf("HIDE_" + flagName.toUpperCase()));
        i = config.getInt(MAX_DURABILITY, -1);
        setMaxDamage(meta, i);
        setCurrentDamage(meta, i);
        if (config.contains(COLOR)){
            if (meta instanceof LeatherArmorMeta) ((LeatherArmorMeta) meta).setColor(MiniMessageUtil.gethexColor(config.getString(COLOR)));
            else logWarning(innerName + "不是皮革制品，颜色无效。");
        }
    }

    @Override
    protected void loadPDCs(@NotNull ItemMeta meta, @NotNull ConfigurationSection config) {
        /* PDC默认值 */
        if (!setItemCalculablePDC(meta, new ItemPDC(innerName, AttributeCalculateSection.BASE, config))) {
            logWarning("ItemPDC 设置出错");
        }
        setSlot(meta ,config.getString(SLOT,"ANY"));
        /* 灵魂绑定的玩家名(暂时不使用UUID) */
        setOwner(meta, "");
        // TODO 使用更好的方式，比如哈希值
        setUpdateCode(meta, TwItemManager.getUpdateCode());
    }

    public @NotNull List<String> getSkills() {return new ArrayList<>(skills);}

    public @NotNull List<String> getQualityGroups() {return new ArrayList<>(qualityGroups);}
}
