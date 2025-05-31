package io.github.tanice.twItemManager.manager.pdc;

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
     * 自计算，将影响相关数据的属性计算得出最终的属性
     */
    public abstract void selfCalculate();

    /**
     * 数据整合
     */
    public abstract void merge(CalculablePDC... o);

    /**
     * 显示形式
     */
    public abstract String toString();

    /**
     * 处理配置中的数值(将%转为小数)
     */
    protected Double getCfgValue(@NotNull String str) {
        double v = Double.parseDouble(str.replace("%", ""));
        if (str.endsWith("%")) v /= 100;
        return v;
    }
    /**
     * 返回自身属于的类型
     * @return 数据来源类型
     */
    public AttributeAdditionFromType fromType(){
        return fromType;
    }

    /**
     * 将内部名称哈希，可作为key
     */
    protected @NotNull String hashInnerName() {
        return Hashing.sha256().hashString(innerName, StandardCharsets.UTF_8).toString().toLowerCase();
    }
}
