package io.github.tanice.twItemManager.constance.key;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Set;

/**
 * 此类表示所有可更改的常量，包含需要写入原版中的属性KEY和需要储存在实体PDC中的KEY
 */
public final class AttributeKey {
    /* 原版 */
    public static final String ATTACK_SPEED = "attack_speed";
    public static final String ATTACK_KNOCKBACK = "attack_knockback";
    public static final String KNOCKBACK_RESISTANCE = "knockback_resistance";
    public static final String LUCK = "luck";
    public static final String MAX_HEALTH = "max_health";
    public static final String MOVEMENT_SPEED = "movement_speed";
    public static final String BLOCK_BREAK_SPEED = "block_break_speed";
    public static final String BLOCK_INTERACTION_RANGE = "block_interaction_range";
    public static final String ENTITY_INTERACTION_RANGE = "entity_interaction_range";
    public static final String MINING_EFFICIENCY = "mining_efficiency";
    // 用处很少，用于特殊标记等方式
    public static final String BURNING_TIME = "burning_time";
    public static final String EXPLOSION_KNOCKBACK_RESISTANCE = "explosion_knockback_resistance";
    public static final String FALL_DAMAGE_MULTIPLIER = "fall_damage_multiplier";
    public static final String FLYING_SPEED = "flying_speed";
    public static final String GRAVITY = "gravity";
    public static final String JUMP_STRENGTH = "jump_strength";
    public static final String MAX_ABSORPTION = "max_absorption";
    public static final String MOVEMENT_EFFICIENCY = "movement_efficiency";
    public static final String OXYGEN_BONUS = "oxygen_bonus";
    public static final String SAFE_FALL_DISTANCE = "safe_fall_distance";
    public static final String SCALE = "scale";
    public static final String STEP_HEIGHT = "step_height";
    public static final String WATER_MOVEMENT_EFFICIENCY = "water_movement_efficiency";
    public static final String SNEAKING_SPEED = "sneaking_speed";
    public static final String SUBMERGED_MINING_SPEED = "submerged_mining_speed";
    public static final String SWEEPING_DAMAGE_RATIO = "sweeping_damage_ratio";

    /* PDC */
    public static final String SLOT = "slot";
    public static final String QUALITY = "quality";
    public static final String BASE_DAMAGE = "attack_damage";
    public static final String LEVEL = "level";
    public static final String CRITICAL_STRIKE_CHANCE = "critical_strike_chance";
    public static final String CRITICAL_STRIKE_DAMAGE = "critical_strike_damage";
    public static final String ARMOR = "armor";
    public static final String ARMOR_TOUGHNESS = "armor_toughness";
    public static final String PRE_ARMOR_REDUCTION = "pre_armor_reduction";
    public static final String AFTER_ARMOR_REDUCTION = "after_armor_reduction";
    public static final String MANA_COST = "skill_mana_cost";
    public static final String SKILL_COOLDOWN = "skill_cooldown";
    public static final String SET_NAME = "set_name";
    public static final String MAX_DURABILITY = "max_durability";

    /* 职业/武器类型 */
    public static final String MELEE = "melee";
    public static final String MAGIC = "magic";
    public static final String RANGED = "ranged";
    public static final String ROUGE = "rouge";
    public static final String SUMMON = "summon";
    public static final String OTHER = "other";

    public static final Set<String> OriginalKeys = Set.of(
            ATTACK_SPEED,ATTACK_KNOCKBACK,KNOCKBACK_RESISTANCE,LUCK,MAX_HEALTH,MOVEMENT_SPEED,BLOCK_BREAK_SPEED,BLOCK_INTERACTION_RANGE,ENTITY_INTERACTION_RANGE,MINING_EFFICIENCY,
            BURNING_TIME,EXPLOSION_KNOCKBACK_RESISTANCE,FALL_DAMAGE_MULTIPLIER,FLYING_SPEED,GRAVITY,JUMP_STRENGTH,MAX_ABSORPTION,MOVEMENT_EFFICIENCY,OXYGEN_BONUS,SAFE_FALL_DISTANCE,SCALE,STEP_HEIGHT,WATER_MOVEMENT_EFFICIENCY,SNEAKING_SPEED,SUBMERGED_MINING_SPEED,SWEEPING_DAMAGE_RATIO
    );

    public static final Set<String> PDCKeys = Set.of(
            SLOT,QUALITY,BASE_DAMAGE,LEVEL,CRITICAL_STRIKE_CHANCE,CRITICAL_STRIKE_DAMAGE,ARMOR,ARMOR_TOUGHNESS,PRE_ARMOR_REDUCTION,AFTER_ARMOR_REDUCTION,MANA_COST,SKILL_COOLDOWN,SET_NAME,MAX_DURABILITY
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
