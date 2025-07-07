package io.github.tanice.twItemManager.manager.buff;

import io.github.tanice.twItemManager.manager.pdc.type.AttributeType;
import io.github.tanice.twItemManager.manager.pdc.type.DamageType;
import io.github.tanice.twItemManager.manager.skill.SkillDamageData;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

import java.util.EnumMap;

/**
 * 将数据传给js使用的类
 */
@Setter
@Getter
public class DamageInnerAttr {
    /** 攻击方 */
    private LivingEntity attacker;
    /** 防御方 */
    private LivingEntity defender;
    /** 伤害 */
    private double damage;
    /** 双方属性 */
    private EnumMap<AttributeType, Double> attackerAttributes;
    private EnumMap<AttributeType, Double> defenderAttributes;
    /** 攻击来源类型 */
    private DamageType damageType;
    /** 技能数据 */
    private SkillDamageData skillDamageData;

    public DamageInnerAttr(@NotNull LivingEntity attacker, @NotNull LivingEntity defender, double damage, @NotNull EnumMap<AttributeType, Double> attackerAttributes, @NotNull EnumMap<AttributeType, Double> defenderAttributes, @NotNull DamageType damageType) {
        this.attacker = attacker;
        this.defender = defender;
        this.damage = damage;
        this.attackerAttributes = attackerAttributes;
        this.defenderAttributes = defenderAttributes;
        this.damageType = damageType;
        this.skillDamageData = null;
    }

    public DamageInnerAttr(@NotNull LivingEntity attacker, @NotNull LivingEntity defender, double damage, @NotNull EnumMap<AttributeType, Double> attackerAttributes, @NotNull EnumMap<AttributeType, Double> defenderAttributes, @NotNull DamageType damageType, @NotNull SkillDamageData skillDamageData) {
        this.attacker = attacker;
        this.defender = defender;
        this.damage = damage;
        this.attackerAttributes = attackerAttributes;
        this.defenderAttributes = defenderAttributes;
        this.damageType = damageType;
        this.skillDamageData = skillDamageData;
    }
}
