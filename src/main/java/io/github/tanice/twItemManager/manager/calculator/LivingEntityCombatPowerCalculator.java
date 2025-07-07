package io.github.tanice.twItemManager.manager.calculator;

import io.github.tanice.twItemManager.config.Config;
import io.github.tanice.twItemManager.manager.pdc.CalculablePDC;
import io.github.tanice.twItemManager.manager.pdc.type.AttributeCalculateSection;
import io.github.tanice.twItemManager.manager.pdc.type.AttributeType;
import lombok.Getter;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

import java.util.EnumMap;

@Getter
public class LivingEntityCombatPowerCalculator extends Calculator {
    /**
     * 仅在序列化使用
     */
    public LivingEntityCombatPowerCalculator() {
        super();
    }

    public LivingEntityCombatPowerCalculator(@NotNull LivingEntity entity) {
        super(entity);
    }
}
