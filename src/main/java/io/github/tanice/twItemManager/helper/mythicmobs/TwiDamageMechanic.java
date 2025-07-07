package io.github.tanice.twItemManager.helper.mythicmobs;

import io.github.tanice.twItemManager.event.TwDamageEvent;
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
            name = "damage",
            aliases = {"d"},
            defValue = "0",
            description = "技能直接造成伤害，不依赖武器伤害"
    )
    protected PlaceholderDouble damage;

    @MythicField(
            name = "k",
            aliases = {"k"},
            defValue = "1",
            description = "武器伤害计算完成后将乘算的值"
    )
    protected PlaceholderDouble k;

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
        this.k = mlc.getPlaceholderDouble(new String[]{"k"}, 1D);
    }

    @Override
    public SkillResult castAtEntity(SkillMetadata data, @NotNull AbstractEntity target) {
        if (target.isDead() || !target.isLiving() || target.getHealth() <= 0.0) return SkillResult.INVALID_TARGET;

        Entity attacker = data.getCaster().getEntity().getBukkitEntity();
        Entity defender = target.getBukkitEntity();

        if (attacker instanceof LivingEntity livingD && defender instanceof LivingEntity livingT) {
            TwDamageEvent twDamageEvent = new TwSkillDamageEvent(livingD, livingT, new SkillDamageData("1"));
            Bukkit.getPluginManager().callEvent(twDamageEvent);
        }
        return SkillResult.SUCCESS;
    }
}