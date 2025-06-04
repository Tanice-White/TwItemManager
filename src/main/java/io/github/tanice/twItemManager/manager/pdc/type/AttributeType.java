package io.github.tanice.twItemManager.manager.pdc.type;

import java.io.Serializable;

/**
 * 同类加算
 */
public enum AttributeType implements Serializable {
    DAMAGE,
    ARMOR,
    CRITICAL_STRIKE_CHANCE, // 加算
    CRITICAL_STRIKE_DAMAGE, // 加算
    ARMOR_TOUGHNESS,        // 加算
    PRE_ARMOR_REDUCTION,    // 属性单独提供，加算  很少，对计算影响很大  受平衡影响
    AFTER_ARMOR_REDUCTION,  // 属性单独提供，加算  基本的减伤就是此类型  受平衡影响
    MANA_COST,              // 加算
    SKILL_COOL_DOWN,        // 加算

    // TODO 特定情况下的伤害减免
}
