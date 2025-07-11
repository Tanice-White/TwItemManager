package io.github.tanice.twItemManager.manager.global;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SkillDamageData {
    /*
     * 如果技能的伤害是 固定值(damage!=0)，则根据 是否受到武器类型增伤的影响(powerByDamageType) 计算得到最终的伤害值
     * 如果 技能伤害不是固定值 (damage==0)，则根据 技能伤害相较于面板伤害的比例(damageK) 和 是否受到武器类型增伤的影响(powerByDamageType) 计算得到最终的伤害值
     */
    /** 技能伤害 比例 */
    private double damageK;
    /** 技能单独的伤害 */
    private double damage;
    /** 是否受到武器类型增伤的影响 */
    private boolean powerByDamageType;

    /*
     * 判断技能是否能够暴击，不能则跳过
     * 否则根据 criticalChance 是否为 0 决定使用哪一个值 优先使用criticalChance
     */
    /** 技能是否能暴击 */
    private boolean canCritical;
    /** 技能暴击率是原技能暴击的占比 */
    private double criticalK;
    /** 技能暴击概率 */
    private double criticalChance;

    /** 其他附加属性 */
    protected boolean ignoreArmor;  /* 无视护甲 */
    protected boolean preventKnockback;  /* 不击退 */
    protected boolean preventImmunity;  /* 不造成无敌帧 */
    protected boolean ignoreInvulnerability;  /* 无视无敌帧 */
}
