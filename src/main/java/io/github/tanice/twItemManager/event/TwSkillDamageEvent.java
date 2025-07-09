package io.github.tanice.twItemManager.event;

import io.github.tanice.twItemManager.TwItemManager;
import io.github.tanice.twItemManager.config.Config;
import io.github.tanice.twItemManager.manager.buff.DamageInnerAttr;
import io.github.tanice.twItemManager.manager.calculator.LivingEntityCombatPowerCalculator;
import io.github.tanice.twItemManager.manager.item.base.BaseItem;
import io.github.tanice.twItemManager.manager.item.base.impl.Item;
import io.github.tanice.twItemManager.manager.pdc.impl.BuffPDC;
import io.github.tanice.twItemManager.manager.pdc.type.AttributeType;
import io.github.tanice.twItemManager.manager.pdc.type.BuffActiveCondition;
import io.github.tanice.twItemManager.manager.pdc.type.DamageType;
import io.github.tanice.twItemManager.manager.skill.SkillDamageData;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;

import static io.github.tanice.twItemManager.util.Logger.logInfo;
import static io.github.tanice.twItemManager.util.Tool.enumMapToString;

public class TwSkillDamageEvent extends TwDamageEvent{
    /** 技能伤害元数据 */
    private final SkillDamageData skillDamageData;

    public TwSkillDamageEvent(@NotNull LivingEntity caster, @NotNull LivingEntity target, @NotNull SkillDamageData skillDamageData) {
        super(caster, target, 0, false);
        this.skillDamageData = skillDamageData;
    }

    @Override
    public double getFinalDamage() {

        double finalDamage = 0;

        if (skillDamageData.getDamage() != 0) finalDamage = skillDamageData.getDamage();
        else {

        }
        /* TIMER 不处理，而是在增加buff的时候处理 */
        return 2.5;
    }
}
