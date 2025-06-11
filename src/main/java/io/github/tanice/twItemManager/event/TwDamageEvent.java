package io.github.tanice.twItemManager.event;

import io.github.tanice.twItemManager.manager.pdc.type.AttributeType;
import io.github.tanice.twItemManager.manager.pdc.type.DamageType;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.Entity;
import org.bukkit.event.Event;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.EnumMap;

public class TwDamageEvent extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();
    private boolean cancelled;

    /** 攻击方 */
    @Getter
    @Setter
    private Entity attacker;
    /** 防御方 */
    @Getter
    @Setter
    private Entity defender;
    /** 伤害 */
    @Getter
    @Setter
    private double damage;
    /** 双方属性 */
    @Getter
    @Setter
    private EnumMap<AttributeType, Double> attackerAttributes;
    @Getter
    @Setter
    private EnumMap<AttributeType, Double> defenderAttributes;
    /** 攻击来源类型 */
    @Getter
    @Setter
    private DamageType damageType;

    public TwDamageEvent(Entity attacker, Entity defender, double damage, EnumMap<AttributeType, Double> attackerAttributes, EnumMap<AttributeType, Double> defenderAttributes, DamageType damageType) {
        this.attacker = attacker;
        this.defender = defender;
        this.damage = damage;
        this.attackerAttributes = attackerAttributes;
        this.defenderAttributes = defenderAttributes;
        this.damageType = damageType;
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
}
