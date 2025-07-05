package io.github.tanice.twItemManager.manager.player;

import io.github.tanice.twItemManager.config.Config;
import io.github.tanice.twItemManager.infrastructure.AttributeAPI;
import io.github.tanice.twItemManager.infrastructure.PDCAPI;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

import static io.github.tanice.twItemManager.constance.key.ConsumableAttributeKey.*;
import static io.github.tanice.twItemManager.util.Logger.logWarning;

@Getter
public class PlayerData {
    private final String uuid;
    private Player player;

    /* 生命值 */
    @Setter
    private double health;
    @Setter
    private double maxHealth;
    /* 蓝条 */
    @Setter
    private double mana;
    @Setter
    private double maxMana;
    /* 饥饿度 */
    @Setter
    private int food;
    /* 饱食度 */
    @Setter
    private float saturation;
    /* 等级 */
    @Setter
    private int level;
    /* 允许飞行 */
    @Setter
    private boolean allowFlight;

    /**
     * 数据库初始化使用，其余情况请勿使用
     */
    public PlayerData(@NotNull String uuid, int food, float saturation, int level, double health, double maxHealth, boolean allowFlight, double mana, double maxMana) {
        this.player = null;
        this.uuid = uuid;

        this.food = food;
        this.level = level;
        this.saturation = saturation;

        this.health = health;
        this.maxHealth = maxHealth;

        this.allowFlight = allowFlight;

        this.mana = mana;
        this.maxMana = maxMana;
    }
    /**
     * 创建用于玩家初始化的PlayerData对象
     */
    @Contract("_ -> new")
    public static @NotNull PlayerData initPlayerData(@NotNull Player player) {
        return new PlayerData(player, true);
    }

    /**
     * 创建用于储存玩家数据的PlayerData对象
     */
    @Contract("_ -> new")
    public static @NotNull PlayerData readPlayerData(@NotNull Player player) {
        return new PlayerData(player, false);
    }

    /**
     * 玩家初始化后调用
     * tips: 先修改最大值
     */
    public void selfActivate() {
        if (player == null) {
            player = Bukkit.getPlayer(UUID.fromString(uuid));
            if (player == null) {
                logWarning("Player " + uuid + " 不存在, 取消 PlayerData 同步");
                return;
            }
        }
        AttributeAPI.setOriBaseAttr(player, Attribute.MAX_HEALTH, maxHealth);
        player.setHealth(Math.min(health, maxHealth));

        if (!player.isHealthScaled()) {
            player.setHealthScale(20);
            player.setHealthScaled(true);
        }

        if (food < 0) food = 0;
        if (food > 20) food = 20;
        player.setFoodLevel(food);

        if (saturation < 0) saturation = 0;
        if (saturation > 20) saturation = 20;
        player.setSaturation(saturation);

        if (level < 0) level = 0;
        player.setLevel(level);

        player.setAllowFlight(allowFlight);

        if (maxMana < 0) maxMana = 0;
        if (mana < 0) mana = 0;
        if (mana > maxMana) mana = maxMana;
        PDCAPI.setMaxMana(player, maxMana);
        PDCAPI.setCurrentMana(player, mana);
        /* TODO 蓝条显示 */

    }

    /**
     * Consumable 专用
     */
    @Contract("_ -> new")
    public static @NotNull PlayerData newFromConsumable(@NotNull ConfigurationSection cfg) {
        return new PlayerData(
                cfg.getDouble(HEALTH, 0D),
                cfg.getDouble(MAX_HEALTH, 0D),
                cfg.getDouble(MANA, 0D),
                cfg.getDouble(MAX_MANA, 0D),
                cfg.getInt(FOOD, 0),
                (float) cfg.getDouble(SATURATION, 0D),
                cfg.getInt(LEVEL, 0)
        );
    }

    /**
     * 合并两个PlayerData，即属性改变
     */
    public void mergeAndActive(@NotNull PlayerData playerData) {
        this.health += playerData.health;
        this.maxHealth += playerData.maxHealth;
        this.mana += playerData.mana;
        this.maxMana += playerData.maxMana;
        this.food += playerData.food;
        this.saturation += playerData.saturation;
        this.level += playerData.level;

        this.selfActivate();
    }

    private PlayerData(@NotNull Player player, boolean isInitialization) {
        this.player = player;
        this.uuid = player.getUniqueId().toString();

        this.food = player.getFoodLevel();
        this.level = player.getLevel();
        this.saturation = player.getSaturation();

        this.allowFlight = player.getAllowFlight();

        this.health = player.getHealth();

        if (isInitialization) {
            this.maxHealth = Config.originalMaxHealth;
            this.mana = Config.originalMaxMana;
            this.maxMana = Config.originalMaxMana;
        } else {
            // PDC 内部信息
            this.maxHealth = AttributeAPI.getOriBaseAttr(player, Attribute.MAX_HEALTH);
            this.mana = PDCAPI.getCurrentMana(player);
            this.maxMana = PDCAPI.getMaxMana(player);
        }
    }

    /**
     * consumable 专用
     */
    private PlayerData(double health, double maxHealth, double mana, double maxMana, int food, float saturation, int level) {
        uuid = null;
        player = null;
        this.health = health;
        this.maxHealth = maxHealth;
        this.mana = mana;
        this.maxMana = maxMana;
        this.food = food;
        this.saturation = saturation;
        this.level = level;
    }
}
