package io.github.tanice.twItemManager.pdc.type;

import java.io.Serializable;

/**
 * 属性计算分区
 */
public enum AttributeCalculateSection implements Serializable {
    /* 其他（用于用户自定义或者bug） */
    OTHER,
    /* 每秒激活 */
    TIMER,
    /* 伤害计算前 */
    BEFORE_DAMAGE,  // 例：闪避


    /* 伤害计算内 */
    BASE,           // 白值计算区
    /**
     * 某一DamageType的伤害提升 = (加算区同类百分比之和) * (1 + 乘算区同类百分比之和)
     * ADD 和 MULTIPLY 本质上一样，只是计算顺序不同
     */
    ADD,        // 加算区（同AttributeType类相加） -- 常用
    MULTIPLY,   // 乘算区（同AttributeType类相加） -- 稀少
    FIX,        // 修正区（按照MULTIPLY的方法计算）  例：某些attr的最终增伤可以在这里


    /* 攻击伤害计算完成后，防御计算前 */
    BETWEEN_DAMAGE_ADN_DEFENCE,   // 例：真伤
    /* 伤害计算后 */
    AFTER_DAMAGE,  // 例：吸血
}
