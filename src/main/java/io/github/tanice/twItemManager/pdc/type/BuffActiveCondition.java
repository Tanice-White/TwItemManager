package io.github.tanice.twItemManager.pdc.type;

public enum BuffActiveCondition {
    ATTACKER,  /* buff持有者作为攻击方激活 = ATTACKER_PHYSICAL + ATTACKER_SKILL */
    ATTACKER_PHYSICAL, /* 非技能类攻击放激活 */
    ATTACKER_SKILL,  /* 技能释放方激活 */

    DEFENDER,  /* buff持有者作为防御方激活 */
    DEFENDER_PHYSICAL,  /* 非技能类被攻击方激活 */
    DEFENDER_SKILL,  /* 被技能攻击时激活 */

    ALL,  /* 均能激活 */
}
