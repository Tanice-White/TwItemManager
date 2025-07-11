package io.github.tanice.twItemManager.event.entity;

import io.github.tanice.twItemManager.TwItemManager;
import io.github.tanice.twItemManager.config.Config;
import io.github.tanice.twItemManager.manager.global.DamageAttributes;
import io.github.tanice.twItemManager.calculator.LivingEntityCombatPowerCalculator;
import io.github.tanice.twItemManager.manager.item.base.BaseItem;
import io.github.tanice.twItemManager.manager.item.base.impl.Item;
import io.github.tanice.twItemManager.pdc.impl.BuffPDC;
import io.github.tanice.twItemManager.pdc.type.AttributeType;
import io.github.tanice.twItemManager.pdc.type.BuffActiveCondition;
import io.github.tanice.twItemManager.pdc.type.DamageType;
import io.github.tanice.twItemManager.manager.global.SkillDamageData;
import lombok.Getter;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;

import static io.github.tanice.twItemManager.util.Logger.logInfo;
import static io.github.tanice.twItemManager.util.Tool.enumMapToString;

@Getter
public class TwSkillDamageEvent extends TwEntityDamageByEntityEvent {
    /** 技能伤害元数据 */
    private final SkillDamageData skillDamageData;

    public TwSkillDamageEvent(@NotNull LivingEntity caster, @NotNull LivingEntity target, @NotNull SkillDamageData skillDamageData) {
        super(caster, target, 0, false);
        this.skillDamageData = skillDamageData;
    }

    @Override
    public double getFinalDamage() {
        double finalDamage = 0;

        /* 获取玩家攻击类型 */
        DamageType entityDamageType = DamageType.OTHER;

        EntityEquipment equipment = attacker.getEquipment();
        if (equipment != null) {
            ItemStack mainHandWeapon = equipment.getItemInMainHand();
            BaseItem bit = TwItemManager.getItemManager().getBaseItem(mainHandWeapon);
            if (bit instanceof Item it) entityDamageType = it.getDamageType();
            /* 否则武器类型保持OTHER不变 */
        }

        /* 获取玩家计算属性 */
        LivingEntityCombatPowerCalculator attackerCalculator = (LivingEntityCombatPowerCalculator) TwItemManager.getEntityAttributeManager().getCalculator(attacker);
        LivingEntityCombatPowerCalculator defenderCalculator = (LivingEntityCombatPowerCalculator) TwItemManager.getEntityAttributeManager().getCalculator(defender);
        EnumMap<AttributeType, Double> attackerAttributeTypeModifiers = attackerCalculator.getAttributeTypeModifiers();
        EnumMap<AttributeType, Double> defenderAttributeTypeModifiers = defenderCalculator.getAttributeTypeModifiers();

        DamageAttributes damageAttributes = new DamageAttributes(attacker, defender, 0, attackerAttributeTypeModifiers, defenderAttributeTypeModifiers, entityDamageType);
        /* BEFORE_DAMAGE 事件计算 */
        // TODO condition优化
        List<BuffPDC> bd = attackerCalculator.getOrderedBeforeList(BuffActiveCondition.ATTACKER);
        bd.addAll(defenderCalculator.getOrderedBeforeList(BuffActiveCondition.DEFENDER));
        Collections.sort(bd);
        for (BuffPDC pdc : bd) {
            Object answer = pdc.execute(damageAttributes);
            /* 可能被更改 */
            finalDamage = damageAttributes.getDamage();
            if (answer.equals(false)) return finalDamage;
        }

        /* 计算武器伤害 */
        // 有 damage 则使用 damage
        if (skillDamageData.getDamage() != 0) finalDamage = skillDamageData.getDamage();
        // 否则使用 damageK
        else if (skillDamageData.getDamageK() != 0) {
            if (Config.debug) {
                logInfo("[TwDamageEvent] attacker attribute map: " + enumMapToString(attackerAttributeTypeModifiers));
                logInfo("[TwDamageEvent] attacker damage type map: " + enumMapToString(attackerCalculator.getDamageTypeModifiers()));
            }
            finalDamage = attackerAttributeTypeModifiers.get(AttributeType.ATTACK_DAMAGE);
            finalDamage *=  (1 + attackerCalculator.getDamageTypeModifiers().getOrDefault(entityDamageType, 0D));
        }
        // 两个都为0 则不计算

        /* 暴击计算 */
        critical = false;
        if (skillDamageData.isCanCritical()) {
            double chance = skillDamageData.getCriticalChance();
            if (chance == 0) chance = attackerAttributeTypeModifiers.get(AttributeType.CRITICAL_STRIKE_CHANCE) * skillDamageData.getCriticalChance();

            if (rand.nextDouble() < chance){
                critical = true;
                finalDamage *= attackerAttributeTypeModifiers.get(AttributeType.CRITICAL_STRIKE_DAMAGE) < 1 ? 1: attackerAttributeTypeModifiers.get(AttributeType.CRITICAL_STRIKE_DAMAGE);
            }
        }

        /* 伤害浮动 */
        if (damageFloat) finalDamage *= rand.nextDouble(1 - floatRange, 1 + floatRange);
        damageAttributes.setDamage(finalDamage);

        /* 中间属性生效 */
        List<BuffPDC> be = attackerCalculator.getOrderedBetweenList(BuffActiveCondition.ATTACKER);
        be.addAll(defenderCalculator.getOrderedBetweenList(BuffActiveCondition.DEFENDER));
        Collections.sort(be);
        for (BuffPDC pdc : be) {
            Object answer = pdc.execute(damageAttributes);
            /* 可能被更改 */
            finalDamage = damageAttributes.getDamage();
            if (answer.equals(false)) return finalDamage;
        }

        /* 最终伤害计算 */
        finalDamage *= (1 - defenderAttributeTypeModifiers.get(AttributeType.PRE_ARMOR_REDUCTION));
        finalDamage -= defenderAttributeTypeModifiers.get(AttributeType.ARMOR) * worldK;
        /* 数值修正 */
        finalDamage = Math.max(0, finalDamage);
        finalDamage *= (1 - defenderAttributeTypeModifiers.get(AttributeType.AFTER_ARMOR_REDUCTION));

        /* AFTER_DAMAGE 事件计算 */
        damageAttributes.setDamage(finalDamage);

        List<BuffPDC> ad = attackerCalculator.getOrderedAfterList(BuffActiveCondition.ATTACKER);
        ad.addAll(defenderCalculator.getOrderedAfterList(BuffActiveCondition.DEFENDER));
        Collections.sort(ad);
        for (BuffPDC pdc : ad) {
            Object answer = pdc.execute(damageAttributes);
            /* 可能被更改 */
            finalDamage = damageAttributes.getDamage();
            if (answer.equals(false)) return finalDamage;
        }

        this.activateBuffForAttackerAndDefender(attacker, defender);

        return finalDamage;
    }
}
