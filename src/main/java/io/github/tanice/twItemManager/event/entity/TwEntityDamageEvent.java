package io.github.tanice.twItemManager.event.entity;

import io.github.tanice.twItemManager.TwItemManager;
import io.github.tanice.twItemManager.config.Config;
import io.github.tanice.twItemManager.manager.global.DamageAttributes;
import io.github.tanice.twItemManager.calculator.LivingEntityCombatPowerCalculator;
import io.github.tanice.twItemManager.manager.item.base.impl.Item;
import io.github.tanice.twItemManager.pdc.impl.BuffPDC;
import io.github.tanice.twItemManager.pdc.type.AttributeType;
import io.github.tanice.twItemManager.pdc.type.BuffActiveCondition;
import io.github.tanice.twItemManager.util.EquipmentUtil;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.EnumMap;
import java.util.Random;

import static io.github.tanice.twItemManager.util.Logger.logInfo;
import static io.github.tanice.twItemManager.util.Tool.enumMapToString;

/**
 * 插件伤害事件
 * TODO 原版武器的护甲等也需要参与计算
 */
public class TwEntityDamageEvent extends Event implements Cancellable {
    protected static final HandlerList handlers = new HandlerList();
    protected boolean cancelled;

    protected final Random rand = new Random();
    /** 防御方 */
    @Getter
    @NotNull
    protected LivingEntity defender;
    /** 是否为原版跳劈(暴击) */
    @Getter
    protected boolean critical;
    /** 初始伤害 */
    @Getter
    @Setter
    protected double damage;

    protected boolean damageFloat;
    protected double floatRange;
    protected double worldK;

    public TwEntityDamageEvent(@NotNull LivingEntity defender, double oriDamage) {
        this.defender = defender;
        this.damage = oriDamage;
        critical = false;
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
        LivingEntityCombatPowerCalculator defenderCalculator = (LivingEntityCombatPowerCalculator) TwItemManager.getEntityAttributeManager().getCalculator(defender);
        EnumMap<AttributeType, Double> defenderAttributeTypeModifiers = defenderCalculator.getAttributeTypeModifiers();

        if (Config.debug) {
            logInfo("[TwEntityDamageEvent] defender attribute map: " + enumMapToString(defenderAttributeTypeModifiers));
            logInfo("[TwEntityDamageEvent] defender damage type map: " + enumMapToString(defenderCalculator.getDamageTypeModifiers()));
        }
        double finalDamage = damage;

        DamageAttributes damageAttributes = new DamageAttributes(defender, 0, defenderAttributeTypeModifiers);
        /* BEFORE_DAMAGE 事件计算 */
        for (BuffPDC pdc : defenderCalculator.getOrderedBeforeList(BuffActiveCondition.DEFENDER)) {
            Object answer = pdc.execute(damageAttributes);
            /* 可能被更改 */
            finalDamage = damageAttributes.getDamage();
            if (answer.equals(false)) return finalDamage;
        }
        damageAttributes.setDamage(finalDamage);

        /* 中间属性生效 */
        for (BuffPDC pdc : defenderCalculator.getOrderedBetweenList(BuffActiveCondition.DEFENDER)) {
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

        for (BuffPDC pdc : defenderCalculator.getOrderedAfterList(BuffActiveCondition.DEFENDER)) {
            Object answer = pdc.execute(damageAttributes);
            /* 可能被更改 */
            finalDamage = damageAttributes.getDamage();
            if (answer.equals(false)) return finalDamage;
        }

        /* defender 给 自己 增加 buff */
        boolean f = false;
        for (Item i : EquipmentUtil.getActiveEquipmentItem(defender)){
            f |= TwItemManager.getBuffManager().activateBuffs(defender, i.getDefenseBuffs().getFirst());
        }

        if (f) Bukkit.getPluginManager().callEvent(new EntityAttributeChangeEvent(defender));
        return finalDamage;
    }

    /**
     * 加载全局配置中的信息
     */
    protected void initFormConfig() {
        /* 伤害计算配置 */
        worldK = Config.worldK;
        damageFloat = Config.damageFloat;
        floatRange = Config.damageFloatRange;
    }
}
