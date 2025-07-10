package io.github.tanice.twItemManager.helper.mythicmobs;

import io.github.tanice.twItemManager.event.TwEntityDamageByEntityEvent;
import io.github.tanice.twItemManager.event.TwSkillDamageEvent;
import io.github.tanice.twItemManager.manager.skill.SkillDamageData;
import io.lumine.mythic.api.adapters.AbstractEntity;
import io.lumine.mythic.api.config.MythicLineConfig;
import io.lumine.mythic.api.skills.ITargetedEntitySkill;
import io.lumine.mythic.api.skills.SkillMetadata;
import io.lumine.mythic.api.skills.SkillResult;
import io.lumine.mythic.api.skills.placeholders.PlaceholderBoolean;
import io.lumine.mythic.api.skills.placeholders.PlaceholderDouble;
import io.lumine.mythic.core.utils.annotations.MythicField;
import io.lumine.mythic.core.utils.annotations.MythicMechanic;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

@MythicMechanic(
        author = "TaniceWhite",
        name = "twDamage",
        aliases = {"twd"},
        description = "Deals damage to the target in TwItemManager"
)
public class TwiDamageMechanic implements ITargetedEntitySkill {
    @MythicField(
            name = "damageK",
            aliases = {"dk"},
            defValue = "1",
            description = "武器伤害计算完成后将乘算的值"
    )
    protected PlaceholderDouble damageK;

    @MythicField(
            name = "damage",
            aliases = {"d"},
            defValue = "0",
            description = "技能直接造成伤害，不依赖武器伤害"
    )
    protected PlaceholderDouble damage;

    @MythicField(
            name = "powerByDamageType",
            aliases = {"p"},
            defValue = "true",
            description = "技能伤害是否受到伤害类型增伤属性影响"
    )
    protected PlaceholderBoolean powerByDamageType;

    @MythicField(
            name = "critical",
            aliases = {"c"},
            defValue = "true",
            description = "技能能否暴击"
    )
    protected PlaceholderBoolean canCritical;

    @MythicField(
            name = "criticalK",
            aliases = {"ck"},
            defValue = "1",
            description = "技能暴击概率是武器的多少倍"
    )
    protected PlaceholderDouble criticalK;

    @MythicField(
            name = "criticalChance",
            aliases = {"cc"},
            defValue = "0",
            description = "自定义此次伤害的暴击概率"
    )
    protected PlaceholderDouble criticalChance;

    @MythicField(
            name = "ignoreArmor",
            aliases = {"ia"},
            defValue = "false"
    )
    protected PlaceholderBoolean ignoreArmor;

    @MythicField(
            name = "preventKnockback",
            aliases = {"pk"},
            defValue = "false"
    )
    protected PlaceholderBoolean preventKnockback;

    @MythicField(
            name = "preventImmunity",
            aliases = {"pi"},
            defValue = "false"
    )
    protected PlaceholderBoolean preventImmunity;

    @MythicField(
            name = "ignoreInvulnerability",
            aliases = {"ii"},
            defValue = "false"
    )
    protected PlaceholderBoolean ignoreInvulnerability;

    public TwiDamageMechanic(@NotNull MythicLineConfig mlc) {
        this.damageK = mlc.getPlaceholderDouble(new String[]{"damageK", "k"}, 1D);
    }

    @Override
    public SkillResult castAtEntity(SkillMetadata data, @NotNull AbstractEntity target) {
        if (target.isDead() || !target.isLiving() || target.getHealth() <= 0.0) return SkillResult.INVALID_TARGET;

        Entity attacker = data.getCaster().getEntity().getBukkitEntity();
        Entity defender = target.getBukkitEntity();

        if (attacker instanceof LivingEntity livingD && defender instanceof LivingEntity livingT) {
            SkillDamageData skillDamageData = new SkillDamageData(
                    damageK.get(data, target),
                    damage.get(data, target),
                    powerByDamageType.get(data, target),
                    canCritical.get(data, target),
                    criticalK.get(data, target),
                    criticalChance.get(data, target),
                    ignoreArmor.get(data, target),
                    preventKnockback.get(data, target),
                    preventImmunity.get(data, target),
                    ignoreInvulnerability.get(data, target)
            );
            TwEntityDamageByEntityEvent twEntityDamageByEntityEvent = new TwSkillDamageEvent(livingD, livingT, skillDamageData);
            Bukkit.getPluginManager().callEvent(twEntityDamageByEntityEvent);
        }
        return SkillResult.SUCCESS;
    }
}