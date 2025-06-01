package io.github.tanice.twItemManager.manager.pdc;

import io.github.tanice.twItemManager.manager.pdc.impl.AttributePDC;
import io.github.tanice.twItemManager.manager.pdc.type.AttributeAdditionFromType;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serial;
import java.io.Serializable;
import com.google.common.hash.Hashing;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static io.github.tanice.twItemManager.constance.key.AttributeKey.*;

/**
 * 属性抽象
 */
@Getter
@Setter
public abstract class CalculablePDC implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    protected static final String ATTR_SECTION_KEY = "attrs";

    /* PDC所在物品的内部名称 */
    /* ITEM类型-可以为中文  其余-中英文都可 */
    protected String innerName;

    /* 属性来源分类 */
    protected AttributeAdditionFromType fromType;

    /**
     * 具体属性
     * 0-属性增加的白值 1-增伤比例
     * AttributeAdditionFromType.BASE 类的damage只有第1个有效，默认白值
     * 非 AttributeAdditionFromType.BASE 类的damage有第1（白值计算）、2（比例计算）个值有效
     */
    protected double[] damage;  /* 任意物品提供的增伤比例都是加算的, 不同物品才乘算 */
    /* 只能加算 */
    protected double criticalStrikeChance;
    protected double criticalStrikeDamage;
    protected double armor;
    protected double armorToughness;
    /* [0,FROM_TYPE_NUM)-百分比增加 */
    protected List<Double> preArmorReduction;
    protected List<Double> afterArmorReduction;
    /* 技能相关 */
    /* 不同物品提供的直接对应相加 */
    /* 0-白值 1-百分比 */
    protected double[] manaCost;
    protected double[] skillCoolDown;

    /**
     * 供序列化使用
     */
    public CalculablePDC() {
        innerName = "default";
        fromType = AttributeAdditionFromType.ITEM;
        damage = new double[]{0D, 0D};
        criticalStrikeChance = 0D;
        criticalStrikeDamage = 0D;
        armor = 0D;
        armorToughness = 0D;
        preArmorReduction = new ArrayList<>();
        afterArmorReduction = new ArrayList<>();
        manaCost = new double[]{0D, 0D};
        skillCoolDown = new double[]{0D, 0D};
    }

    public CalculablePDC(@NotNull String innerName, @NotNull AttributeAdditionFromType aft) {
        this.innerName = innerName.isEmpty() ? "empty-inner-name-is-invalid" : innerName;
        fromType = aft;
        damage = new double[]{0D, 0D};
        criticalStrikeChance = 0D;
        criticalStrikeDamage = 0D;
        armor = 0D;
        armorToughness = 0D;
        preArmorReduction = new ArrayList<>();
        afterArmorReduction = new ArrayList<>();
        manaCost = new double[]{0D, 0D};
        skillCoolDown = new double[]{0D, 0D};
    }
    /* 传入attr下的配置即可 */
    public CalculablePDC(@NotNull String innerName, @NotNull AttributeAdditionFromType aft, @Nullable ConfigurationSection cfg) {
        boolean cfgNull = cfg == null;
        this.innerName = innerName;
        fromType = aft;

        /* 攻击力设置 */
        damage = new double[]{0D, 0D};
        /* 非 AttributeAdditionFromType.BASE 类的damage只有第1（白值计算）、2（比例计算）个值有效 */
        String vs = cfgNull ? null : cfg.getString(BASE_DAMAGE);
        if (vs != null) {
            if (vs.endsWith("%")) damage[1] = Double.parseDouble(vs.replace("%", "")) / 100;
            else damage[0] = Double.parseDouble(vs);
        }

        criticalStrikeChance = cfgNull ? 0D : cfg.getDouble(CRITICAL_STRIKE_CHANCE, 0D);
        criticalStrikeDamage = cfgNull ? 0D : cfg.getDouble(CRITICAL_STRIKE_DAMAGE, 0D);
        armor = cfgNull ? 0D : cfg.getDouble(ARMOR, 0D);
        armorToughness = cfgNull ? 0D : cfg.getDouble(ARMOR_TOUGHNESS, 0D);

        double v;
        preArmorReduction = new ArrayList<>();
        v = cfgNull ? 0D : cfg.getDouble(PRE_ARMOR_REDUCTION, 0D);
        if (v != 0D) preArmorReduction.add(v);
        afterArmorReduction = new ArrayList<>();
        v = cfgNull ? 0D : cfg.getDouble(AFTER_ARMOR_REDUCTION, 0D);
        if (v != 0D) afterArmorReduction.add(v);
    }

    /**
     * 数据整合(只整合统一的数据项)
     * 不同的type需要分类merge！
     */
    public void merge(CalculablePDC @NotNull ... o) {
        for (CalculablePDC cPDC : o) {
            if (cPDC == null) continue;
            this.damage[0] += cPDC.damage[0];
            this.damage[1] += cPDC.damage[1];
            this.criticalStrikeChance += cPDC.criticalStrikeChance;
            this.criticalStrikeDamage += cPDC.criticalStrikeDamage;
            this.armor += cPDC.armor;
            this.armorToughness += cPDC.armorToughness;
            this.preArmorReduction.addAll(cPDC.preArmorReduction);
            this.afterArmorReduction.addAll(cPDC.afterArmorReduction);
            this.manaCost[0] += cPDC.manaCost[0];
            this.manaCost[1] += cPDC.manaCost[1];
            this.skillCoolDown[0] += cPDC.skillCoolDown[0];
            this.skillCoolDown[1] += cPDC.skillCoolDown[1];
        }
    }

    /**
     * 显示形式
     */
    public abstract String toString();
    /**
     * 返回自身属于的类型
     * @return 数据来源类型
     */
    public AttributeAdditionFromType fromType(){
        return fromType;
    }

    /**
     * 将类转为普通的数值计算基类
     */
    public @NotNull AttributePDC toAttributePDC() {
        AttributePDC pdc = new AttributePDC();
        pdc.merge(this);
        return pdc;
    }

    /**
     * 将内部名称哈希，可作为key
     */
    protected @NotNull String hashInnerName() {
        return Hashing.sha256().hashString(innerName, StandardCharsets.UTF_8).toString().toLowerCase();
    }

    /**
     * 处理配置中的数值(将%转为小数)
     */
    protected Double getCfgValue(@NotNull String str) {
        double v = Double.parseDouble(str.replace("%", ""));
        if (str.endsWith("%")) v /= 100;
        return v;
    }
}
