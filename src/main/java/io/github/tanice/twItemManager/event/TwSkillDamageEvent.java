package io.github.tanice.twItemManager.event;

import io.github.tanice.twItemManager.TwItemManager;
import io.github.tanice.twItemManager.config.Config;
import io.github.tanice.twItemManager.manager.buff.DamageInnerAttr;
import io.github.tanice.twItemManager.manager.calculator.LivingEntityCombatPowerCalculator;
import io.github.tanice.twItemManager.manager.item.base.BaseItem;
import io.github.tanice.twItemManager.manager.item.base.impl.Item;
import io.github.tanice.twItemManager.manager.pdc.impl.BuffPDC;
import io.github.tanice.twItemManager.manager.pdc.type.AttributeType;
import io.github.tanice.twItemManager.manager.pdc.type.BuffActiveCondition;
import io.github.tanice.twItemManager.manager.pdc.type.DamageType;
import io.github.tanice.twItemManager.manager.skill.SkillDamageData;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;

import static io.github.tanice.twItemManager.util.Logger.logInfo;
import static io.github.tanice.twItemManager.util.Tool.enumMapToString;

public class TwSkillDamageEvent extends TwDamageEvent{
    /** 技能伤害元数据 */
    private final SkillDamageData skillDamageData;

    public TwSkillDamageEvent(@NotNull LivingEntity caster, @NotNull LivingEntity target, @NotNull SkillDamageData skillDamageData) {
        super(caster, target, 0, false);
        this.skillDamageData = skillDamageData;
    }

    @Override
    public double getFinalDamage() {

        double finalDamage = 0;

        if (skillDamageData.getDamage() != 0) finalDamage = skillDamageData.getDamage();
        else {

        }

        LivingEntityCombatPowerCalculator ac = new LivingEntityCombatPowerCalculator(attacker);
        LivingEntityCombatPowerCalculator bc = new LivingEntityCombatPowerCalculator(defender);
        EnumMap<AttributeType, Double> aAttrMap = ac.getAttributeTypeModifiers();
        EnumMap<AttributeType, Double> bAttrMap = bc.getAttributeTypeModifiers();

        if (Config.debug) {
            logInfo("[TwSkillDamageEvent] attacker attribute map: " + enumMapToString(aAttrMap));
            logInfo("[TwSkillDamageEvent] attacker damage type map: " + enumMapToString(ac.getDamageTypeModifiers()));
        }
        /* 计算玩家生效属性 */
        /* 非法属性都在OTHER中 */
        DamageType weaponDamageType = DamageType.OTHER;

        ItemStack itemStack;
        EntityEquipment equipment = attacker.getEquipment();
        if (equipment != null) {
            itemStack = equipment.getItemInMainHand();  // 单独写出来用于后续判断
            BaseItem bit = TwItemManager.getItemManager().getBaseItem(itemStack);
            if (bit instanceof Item it) weaponDamageType = it.getDamageType();
            /* 否则武器类型保持OTHER不变 */
        }

        DamageInnerAttr damageInnerAttr = new DamageInnerAttr(attacker, defender, 0, aAttrMap, bAttrMap, weaponDamageType, skillDamageData);
        /* BEFORE_DAMAGE 事件计算 */
        List<BuffPDC> bd = ac.getBeforeList(BuffActiveCondition.ATTACKER);
        bd.addAll(bc.getBeforeList(BuffActiveCondition.DEFENDER));
        Collections.sort(bd);
        for (BuffPDC pdc : bd) {
            Object answer = pdc.execute(damageInnerAttr);
            /* 可能被更改 */
            finalDamage = damageInnerAttr.getDamage();
            if (answer.equals(false)) return finalDamage;
        }

        /* 武器的对外白值（品质+宝石+白值） */
        finalDamage = aAttrMap.get(AttributeType.ATTACK_DAMAGE);

        /* 类型增伤 */
        finalDamage *=  (1 + ac.getDamageTypeModifiers().getOrDefault(weaponDamageType, 0D));

        /* 暴击 */
        if (rand.nextDouble() < aAttrMap.get(AttributeType.CRITICAL_STRIKE_CHANCE)){
            finalDamage *= aAttrMap.get(AttributeType.CRITICAL_STRIKE_DAMAGE) < 1 ? 1: aAttrMap.get(AttributeType.CRITICAL_STRIKE_DAMAGE);
        }

        /* 伤害浮动 */
        if (damageFloat) finalDamage *= rand.nextDouble(1 - floatRange, 1 + floatRange);

        damageInnerAttr.setDamage(finalDamage);

        /* 中间属性生效 */
        List<BuffPDC> be = ac.getBetweenList(BuffActiveCondition.ATTACKER);
        be.addAll(bc.getBetweenList(BuffActiveCondition.DEFENDER));
        Collections.sort(be);
        for (BuffPDC pdc : be) {
            Object answer = pdc.execute(damageInnerAttr);
            /* 可能被更改 */
            finalDamage = damageInnerAttr.getDamage();
            if (answer.equals(false)) return finalDamage;
        }

        /* 最终伤害计算 */
        finalDamage *= (1 - bAttrMap.get(AttributeType.PRE_ARMOR_REDUCTION));
        finalDamage -= bAttrMap.get(AttributeType.ARMOR) * worldK;
        /* 数值修正 */
        finalDamage = Math.max(0, finalDamage);
        finalDamage *= (1 - bAttrMap.get(AttributeType.AFTER_ARMOR_REDUCTION));

        /* AFTER_DAMAGE 事件计算 */
        damageInnerAttr.setDamage(finalDamage);

        List<BuffPDC> ad = ac.getAfterList(BuffActiveCondition.ATTACKER);
        ad.addAll(bc.getAfterList(BuffActiveCondition.DEFENDER));
        Collections.sort(ad);
        for (BuffPDC pdc : ad) {
            Object answer = pdc.execute(damageInnerAttr);
            /* 可能被更改 */
            finalDamage = damageInnerAttr.getDamage();
            if (answer.equals(false)) return finalDamage;
        }

        /* a 给 b 增加 buff */
        TwItemManager.getBuffManager().doAttackBuffs(attacker, defender);
        /* b 给 a 增加 buff */
        TwItemManager.getBuffManager().doDefenceBuffs(attacker, defender);

        /* TIMER 不处理，而是在增加buff的时候处理 */
        return finalDamage;
    }
}
