package io.github.tanice.twItemManager.manager.buff;

import io.github.tanice.twItemManager.pdc.type.AttributeType;
import io.github.tanice.twItemManager.pdc.type.DamageType;
import io.github.tanice.twItemManager.manager.skill.SkillDamageData;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;

/**
 * 将数据传给js使用的类
 */
@Setter
@Getter
public class DamageAttributes {
    /** 攻击方 */
    @Nullable
    private LivingEntity attacker;
    /** 防御方 */
    @NotNull
    private LivingEntity defender;
    /** 伤害 */
    private double damage;
    /** 双方属性 */
    @Nullable
    private EnumMap<AttributeType, Double> attackerAttributes;
    @NotNull
    private EnumMap<AttributeType, Double> defenderAttributes;
    /** 攻击来源类型 */
    @Nullable
    private DamageType damageType;
    /** 技能数据 */
    @Nullable
    private SkillDamageData skillDamageData;

    public DamageAttributes(@NotNull LivingEntity defender, double damage, @NotNull EnumMap<AttributeType, Double> defenderAttributes) {
        this.attacker = null;
        this.defender = defender;
        this.damage = damage;
        this.attackerAttributes = null;
        this.defenderAttributes = defenderAttributes;
        this.damageType = null;
        this.skillDamageData = null;
    }

    public DamageAttributes(@NotNull LivingEntity attacker, @NotNull LivingEntity defender, double damage, @NotNull EnumMap<AttributeType, Double> attackerAttributes, @NotNull EnumMap<AttributeType, Double> defenderAttributes, @NotNull DamageType damageType) {
        this.attacker = attacker;
        this.defender = defender;
        this.damage = damage;
        this.attackerAttributes = attackerAttributes;
        this.defenderAttributes = defenderAttributes;
        this.damageType = damageType;
        this.skillDamageData = null;
    }

    public DamageAttributes(@NotNull LivingEntity attacker, @NotNull LivingEntity defender, double damage, @NotNull EnumMap<AttributeType, Double> attackerAttributes, @NotNull EnumMap<AttributeType, Double> defenderAttributes, @NotNull DamageType damageType, @NotNull SkillDamageData skillDamageData) {
        this.attacker = attacker;
        this.defender = defender;
        this.damage = damage;
        this.attackerAttributes = attackerAttributes;
        this.defenderAttributes = defenderAttributes;
        this.damageType = damageType;
        this.skillDamageData = skillDamageData;
    }
}
