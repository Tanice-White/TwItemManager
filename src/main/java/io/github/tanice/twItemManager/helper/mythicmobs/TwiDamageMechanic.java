package io.github.tanice.twItemManager.helper.mythicmobs;

import io.github.tanice.twItemManager.event.TwDamageEvent;
import io.lumine.mythic.api.adapters.AbstractEntity;
import io.lumine.mythic.api.config.MythicLineConfig;
import io.lumine.mythic.api.skills.ITargetedEntitySkill;
import io.lumine.mythic.api.skills.SkillMetadata;
import io.lumine.mythic.api.skills.SkillResult;
import io.lumine.mythic.api.skills.placeholders.PlaceholderDouble;
import io.lumine.mythic.core.utils.annotations.MythicField;
import io.lumine.mythic.core.utils.annotations.MythicMechanic;
import org.bukkit.Bukkit;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

@MythicMechanic(
        author = "TaniceWhite",
        name = "twDamage",
        aliases = {"twd"},
        description = "Deals damage to the target"
)
public class TwiDamageMechanic implements ITargetedEntitySkill {
    @MythicField(
            name = "k",
            aliases = {"k"},
            description = "The amount of damage will multiply",
            defValue = "1"
    )
    protected PlaceholderDouble k;

    public TwiDamageMechanic(@NotNull MythicLineConfig mlc) {
        this.k = mlc.getPlaceholderDouble(new String[]{"k"}, 1D);
    }

    @Override
    public SkillResult castAtEntity(SkillMetadata data, @NotNull AbstractEntity target) {
        if (target.isDead() || !target.isLiving() || target.getHealth() <= 0.0) return SkillResult.INVALID_TARGET;

        Entity damager = data.getCaster().getEntity().getBukkitEntity();
        Entity bukkitTarget = target.getBukkitEntity();

        if (damager instanceof LivingEntity livingD && bukkitTarget instanceof LivingEntity livingT) {
            TwDamageEvent twDamageEvent = new TwDamageEvent(livingD, livingT, false);
            Bukkit.getPluginManager().callEvent(twDamageEvent);
        }
        return SkillResult.SUCCESS;
    }
}