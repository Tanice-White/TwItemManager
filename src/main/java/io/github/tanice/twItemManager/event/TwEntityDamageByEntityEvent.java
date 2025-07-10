package io.github.tanice.twItemManager.event;

import io.github.tanice.twItemManager.TwItemManager;
import io.github.tanice.twItemManager.config.Config;
import io.github.tanice.twItemManager.calculator.LivingEntityCombatPowerCalculator;
import io.github.tanice.twItemManager.manager.item.base.BaseItem;
import io.github.tanice.twItemManager.manager.item.base.impl.Item;
import io.github.tanice.twItemManager.pdc.impl.BuffPDC;
import io.github.tanice.twItemManager.pdc.type.AttributeType;
import io.github.tanice.twItemManager.pdc.type.BuffActiveCondition;
import io.github.tanice.twItemManager.pdc.type.DamageType;
import io.github.tanice.twItemManager.manager.buff.DamageAttributes;
import io.github.tanice.twItemManager.util.EquipmentUtil;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;

import static io.github.tanice.twItemManager.util.Logger.logInfo;
import static io.github.tanice.twItemManager.util.Tool.enumMapToString;

@Getter
public class TwEntityDamageByEntityEvent extends TwEntityDamageEvent {
    /** 攻击方 */
    @NotNull
    protected LivingEntity attacker;

    public TwEntityDamageByEntityEvent(@NotNull LivingEntity attacker, @NotNull LivingEntity defender, double oriDamage, boolean critical) {
        super(defender, oriDamage);
        this.attacker = attacker;
        this.critical = critical;
    }

    public double getFinalDamage() {
        LivingEntityCombatPowerCalculator attackerCalculator = (LivingEntityCombatPowerCalculator) TwItemManager.getEntityAttributeManager().getCalculator(attacker);
        LivingEntityCombatPowerCalculator defenderCalculator = (LivingEntityCombatPowerCalculator) TwItemManager.getEntityAttributeManager().getCalculator(defender);
        EnumMap<AttributeType, Double> attackerAttributeTypeModifiers = attackerCalculator.getAttributeTypeModifiers();
        EnumMap<AttributeType, Double> defenderAttributeTypeModifiers = defenderCalculator.getAttributeTypeModifiers();

        if (Config.debug) {
            logInfo("[TwDamageEvent] attacker attribute map: " + enumMapToString(attackerAttributeTypeModifiers));
            logInfo("[TwDamageEvent] attacker damage type map: " + enumMapToString(attackerCalculator.getDamageTypeModifiers()));
        }
        /* 计算玩家生效属性 */
        /* 非法属性都在OTHER中 */
        DamageType weaponDamageType = DamageType.OTHER;
        ItemStack mainHandItem = new ItemStack(Material.AIR);

        EntityEquipment equipment = attacker.getEquipment();
        if (equipment != null) {
            mainHandItem = equipment.getItemInMainHand();  // 单独写出来用于后续判断
            BaseItem bit = TwItemManager.getItemManager().getBaseItem(mainHandItem);
            if (bit instanceof Item it) weaponDamageType = it.getDamageType();
            /* 否则武器类型保持OTHER不变 */
        }

        double finalDamage;

        DamageAttributes damageAttributes = new DamageAttributes(attacker, defender, 0, attackerAttributeTypeModifiers, defenderAttributeTypeModifiers, weaponDamageType);
        /* BEFORE_DAMAGE 事件计算 */
        List<BuffPDC> bd = attackerCalculator.getOrderedBeforeList(BuffActiveCondition.ATTACKER);
        bd.addAll(defenderCalculator.getOrderedBeforeList(BuffActiveCondition.DEFENDER));
        Collections.sort(bd);
        for (BuffPDC pdc : bd) {
            Object answer = pdc.execute(damageAttributes);
            /* 可能被更改 */
            finalDamage = damageAttributes.getDamage();
            if (answer.equals(false)) return finalDamage;
        }

        /* 武器的对外白值（品质+宝石+白值） */
        finalDamage = attackerAttributeTypeModifiers.get(AttributeType.ATTACK_DAMAGE);
        /* 不影响原版武器伤害的任何计算 */
        /* 原版弓箭伤害就是会飘 */
        if (TwItemManager.getItemManager().isNotItemClassInTwItem(mainHandItem)) finalDamage += damage;
            /* 怪物拿起的武器不受这个影响，伤害是满额的 */
            /* 所以玩家才计算比例 */
        else if (attacker instanceof Player p) {
            // 不确定玩家的cooldown会不会受到武器影响，应该要受到武器的影响
            finalDamage *= 0.15 + p.getAttackCooldown() * 0.85;
        }

        finalDamage *=  (1 + attackerCalculator.getDamageTypeModifiers().getOrDefault(weaponDamageType, 0D));
        if (critical) finalDamage *= 1 + Config.originalCriticalStrikeAddition;

        /* 暴击 */
        critical = false;
        if (rand.nextDouble() < attackerAttributeTypeModifiers.get(AttributeType.CRITICAL_STRIKE_CHANCE)){
            critical = true;
            finalDamage *= attackerAttributeTypeModifiers.get(AttributeType.CRITICAL_STRIKE_DAMAGE) < 1 ? 1: attackerAttributeTypeModifiers.get(AttributeType.CRITICAL_STRIKE_DAMAGE);
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

    /**
     * 为攻击方和防御方激活相关 buff
     */
    protected void activateBuffForAttackerAndDefender(@NotNull LivingEntity attacker, @NotNull LivingEntity defender) {
        /* attacker 给 defender 增加 buff */
        boolean fa = false, fb = false;
        for (Item i : EquipmentUtil.getActiveEquipmentItem(attacker)){
            fa |= TwItemManager.getBuffManager().activateBuffs(attacker, i.getAttackBuffs().getFirst());
            fb |= TwItemManager.getBuffManager().activateBuffs(defender, i.getAttackBuffs().get(1));
        }
        /* defender 给 attacker 增加 buff */
        for (Item i : EquipmentUtil.getActiveEquipmentItem(defender)){
            fb |= TwItemManager.getBuffManager().activateBuffs(defender, i.getDefenseBuffs().getFirst());
            fa |= TwItemManager.getBuffManager().activateBuffs(attacker, i.getDefenseBuffs().get(1));
        }
        if (fa) Bukkit.getPluginManager().callEvent(new EntityAttributeChangeEvent(attacker));
        if (fb) Bukkit.getPluginManager().callEvent(new EntityAttributeChangeEvent(defender));
    }
}
