package io.github.tanice.twItemManager.constance.key;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Set;

/**
 * 此类表示所有可更改的常量，包含需要写入原版中的属性KEY和需要储存在实体PDC中的KEY
 */
public final class AttributeKey {
    /* 原版 */
    public static final String ATTACK_SPEED = "attack-speed";
    public static final String ATTACK_KNOCKBACK = "attack-knockback";
    public static final String KNOCKBACK_RESISTANCE = "knockback-resistance";
    public static final String LUCK = "luck";
    public static final String MAX_HEALTH = "max-health";
    public static final String MOVEMENT_SPEED = "movement-speed";
    public static final String BLOCK_BREAK_SPEED = "block-break-speed";
    public static final String BLOCK_INTERACTION_RANGE = "block-interaction-range";
    public static final String ENTITY_INTERACTION_RANGE = "entity-interaction-range";
    public static final String MINING_EFFICIENCY = "mining-efficiency";
    // 用处很少，用于特殊标记等方式
    public static final String BURNING_TIME = "burning-time";
    public static final String EXPLOSION_KNOCKBACK_RESISTANCE = "explosive-knockback-resistance";
    public static final String FALL_DAMAGE_MULTIPLIER = "fall-damage-multiplier";
    public static final String FLYING_SPEED = "flying-speed";
    public static final String GRAVITY = "gravity";
    public static final String JUMP_STRENGTH = "jump-strength";
    public static final String MAX_ABSORPTION = "max-absorption";
    public static final String MOVEMENT_EFFICIENCY = "movement-efficiency";
    public static final String OXYGEN_BONUS = "oxygen-bonus";
    public static final String SAFE_FALL_DISTANCE = "safe-fall-distance";
    public static final String SCALE = "scale";
    public static final String STEP_HEIGHT = "step-height";
    public static final String WATER_MOVEMENT_EFFICIENCY = "water-movement-efficiency";
    public static final String SNEAKING_SPEED = "sneaking-speed";
    public static final String SUBMERGED_MINING_SPEED = "submerged-mining-speed";
    public static final String SWEEPING_DAMAGE_RATIO = "sweeping-damage-ratio";

    /* PDC */
    public static final String SLOT = "slot";
    public static final String QUALITY = "quality";
    public static final String BASE_DAMAGE = "attack-damage";
    public static final String LEVEL = "level";
    public static final String CRITICAL_STRIKE_CHANCE = "critical-strike-chance";
    public static final String CRITICAL_STRIKE_DAMAGE = "critical-strike-damage";
    public static final String ARMOR = "armor";
    public static final String ARMOR_TOUGHNESS = "armor-toughness";
    public static final String PRE_ARMOR_REDUCTION = "pre-armor-reduction";
    public static final String AFTER_ARMOR_REDUCTION = "after-armor-reduction";

    // TODO 未实现
    public static final String MANA_COST = "mana-cost";
    public static final String SKILL_COOLDOWN = "skill-cooldown";
    public static final String SET_ADDITION = "set-addition";

    public static final Set<String> OriginalKeys = Set.of(
            ATTACK_SPEED,ATTACK_KNOCKBACK,KNOCKBACK_RESISTANCE,LUCK,MAX_HEALTH,MOVEMENT_SPEED,BLOCK_BREAK_SPEED,BLOCK_INTERACTION_RANGE,ENTITY_INTERACTION_RANGE,MINING_EFFICIENCY,
            BURNING_TIME,EXPLOSION_KNOCKBACK_RESISTANCE,FALL_DAMAGE_MULTIPLIER,FLYING_SPEED,GRAVITY,JUMP_STRENGTH,MAX_ABSORPTION,MOVEMENT_EFFICIENCY,OXYGEN_BONUS,SAFE_FALL_DISTANCE,SCALE,STEP_HEIGHT,WATER_MOVEMENT_EFFICIENCY,SNEAKING_SPEED,SUBMERGED_MINING_SPEED,SWEEPING_DAMAGE_RATIO
    );

    public static final Set<String> PDCKeys = Set.of(
            SLOT,QUALITY,BASE_DAMAGE,LEVEL,CRITICAL_STRIKE_CHANCE,CRITICAL_STRIKE_DAMAGE,ARMOR,ARMOR_TOUGHNESS,PRE_ARMOR_REDUCTION,AFTER_ARMOR_REDUCTION,SET_ADDITION
    );

    @Contract(pure = true)
    public static @Unmodifiable Set<String> getOriginalKeys() {
        return OriginalKeys;
    }

    @Contract(pure = true)
    public static @Unmodifiable Set<String> getPDCKeys() {
        return PDCKeys;
    }

    public static boolean isOriginalKey(String key) {
        return OriginalKeys.contains(key);
    }

    public static boolean isPDCKeys(String key) {
        return PDCKeys.contains(key);
    }
}
