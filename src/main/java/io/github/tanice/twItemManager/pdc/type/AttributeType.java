package io.github.tanice.twItemManager.pdc.type;

import java.io.Serializable;

/**
 * 同类加算
 */
public enum AttributeType implements Serializable {
    ATTACK_DAMAGE,
    ARMOR,
    CRITICAL_STRIKE_CHANCE, // 加算
    CRITICAL_STRIKE_DAMAGE, // 加算
    ARMOR_TOUGHNESS,        // 加算
    PRE_ARMOR_REDUCTION,    // 属性单独提供，加算  很少，对计算影响很大  受平衡影响
    AFTER_ARMOR_REDUCTION,  // 属性单独提供，加算  基本的减伤就是此类型  受平衡影响

    // TODO 这俩右键技能释放的时候需要考虑
    SKILL_MANA_COST,        // 加算
    SKILL_COOLDOWN,         // 加算
}
