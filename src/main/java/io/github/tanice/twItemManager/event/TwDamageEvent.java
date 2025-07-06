package io.github.tanice.twItemManager.event;

import io.github.tanice.twItemManager.TwItemManager;
import io.github.tanice.twItemManager.config.Config;
import io.github.tanice.twItemManager.manager.calculator.CombatEntityCalculator;
import io.github.tanice.twItemManager.manager.item.base.BaseItem;
import io.github.tanice.twItemManager.manager.item.base.impl.Item;
import io.github.tanice.twItemManager.manager.pdc.impl.BuffPDC;
import io.github.tanice.twItemManager.manager.pdc.type.AttributeType;
import io.github.tanice.twItemManager.manager.pdc.type.BuffActiveCondition;
import io.github.tanice.twItemManager.manager.pdc.type.DamageType;
import io.github.tanice.twItemManager.manager.buff.DamageInnerAttr;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Random;

import static io.github.tanice.twItemManager.util.Logger.logInfo;
import static io.github.tanice.twItemManager.util.Tool.enumMapToString;

// TODO 检擦物品耐久问题

public class TwDamageEvent extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();
    private boolean cancelled;

    private final Random rand = new Random();
    /** 攻击方 */
    @Getter
    private LivingEntity attacker;
    /** 防御方 */
    @Getter
    private LivingEntity defender;
    /** 是否为原版跳劈 */
    @Getter
    private boolean critical;
    /** 初始伤害 */
    @Getter
    @Setter
    private double damage;

    /** TODO 伤害比例(技能专用)  skillModifier */

    private boolean damageFloat;
    private double floatRange;
    private double worldK;

    public TwDamageEvent(@NotNull LivingEntity attacker, @NotNull LivingEntity defender, double damage, boolean critical) {
        this.attacker = attacker;
        this.defender = defender;
        this.damage = damage;
        this.critical = critical;
        this.initFormConfig();
    }

    public TwDamageEvent(@NotNull LivingEntity attacker, @NotNull LivingEntity defender, boolean critical) {
        this.attacker = attacker;
        this.defender = defender;
        this.damage = 1;
        this.critical = critical;
        this.initFormConfig();
    }

    public static HandlerList getHandlerList() {return handlers;}
    
    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean b) {
        cancelled = b;
    }
    
    public double getFinalDamage() {
        CombatEntityCalculator ac = new CombatEntityCalculator(attacker);
        CombatEntityCalculator bc = new CombatEntityCalculator(defender);
        EnumMap<AttributeType, Double> aAttrMap = ac.getAttrsValue();
        EnumMap<AttributeType, Double> bAttrMap = bc.getAttrsValue();

        if (Config.debug) {
            logInfo("[EntityDamageByEntityEvent] attacker attribute map: " + enumMapToString(aAttrMap));
            logInfo("[EntityDamageByEntityEvent] attacker damage type map: " + enumMapToString(ac.getDamageTypeMap()));
        }
        /* 计算玩家生效属性 */
        /* 非法属性都在OTHER中 */
        DamageType weaponDamageType = DamageType.OTHER;
        ItemStack itemStack = new ItemStack(Material.AIR);

        EntityEquipment equipment = attacker.getEquipment();
        if (equipment != null) {
            itemStack = equipment.getItemInMainHand();  // 单独写出来用于后续判断
            BaseItem bit = TwItemManager.getItemManager().getBaseItem(itemStack);
            if (bit instanceof Item it) weaponDamageType = it.getDamageType();
            /* 否则武器类型保持OTHER不变 */
        }

        double finalDamage = 0;

        DamageInnerAttr damageInnerAttr = new DamageInnerAttr(attacker, defender, 0, aAttrMap, bAttrMap, weaponDamageType);
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
        /* 只要是twItemManager的物品，伤害一定是1，否则一定大于1 */
        /* 不影响原版武器伤害的任何计算 */
        /* 不是插件物品 */
        /* 原版弓箭伤害就是会飘 */
        if (TwItemManager.getItemManager().isNotItemClassInTwItem(itemStack)) finalDamage += damage;
            /* 怪物拿起的武器不受这个影响，伤害是满额的 */
            /* 所以玩家才计算比例 */
        else if (attacker instanceof Player p) {
            // 不确定玩家的cooldown会不会受到武器影响，应该要受到武器的影响
            finalDamage *= 0.15 + p.getAttackCooldown() * 0.85;
        }

        finalDamage *=  (1 + ac.getDamageTypeMap().getOrDefault(weaponDamageType, 0D));
        if (critical) finalDamage *= 1 + Config.originalCriticalStrikeAddition;

        /* 暴击 */
        critical = false;
        if (rand.nextDouble() < aAttrMap.get(AttributeType.CRITICAL_STRIKE_CHANCE)){
            critical = true;
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

    private void initFormConfig() {
        /* 伤害计算配置 */
        worldK = Config.worldK;
        damageFloat = Config.damageFloat;
        floatRange = Config.damageFloatRange;
    }
}
