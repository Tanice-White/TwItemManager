package io.github.tanice.twItemManager.calculator;

import lombok.Getter;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

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
