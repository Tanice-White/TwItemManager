package io.github.tanice.twItemManager.manager.player;

import io.github.tanice.twItemManager.config.Config;
import io.github.tanice.twItemManager.infrastructure.AttributeAPI;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

@Getter
public class PlayerData {
    public final String uuid;
    public final Player player;

    /* 生命值 */
    @Setter
    public double health;
    @Setter
    public double maxHealth;
    /* 蓝条 */
    @Setter
    public double mana;
    @Setter
    public double maxMana;
    /* 饥饿度 */
    @Setter
    public double food;
    /* 饱食度 */
    @Setter
    public double saturation;
    /* 等级 */
    @Setter
    public double level;
    /* 允许飞行 */
    @Setter
    public boolean allowFlight;

    /**
     * 数据库初始化使用
     */
    public PlayerData(@NotNull String uuid, double food, double saturation, double level, double health, double maxHealth, boolean allowFlight, double mana, double maxMana) {
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
     * 玩家初始化使用
     */
    public PlayerData(@NotNull Player player) {
        this.player = player;
        this.uuid = player.getUniqueId().toString();

        this.food = player.getFoodLevel();
        this.level = player.getLevel();
        this.saturation = player.getSaturation();

        this.allowFlight = player.getAllowFlight();

        this.health = player.getHealth();
        this.maxHealth = Config.originalMaxHealth;
        this.mana = Config.originalMaxMana;
        this.maxMana = Config.originalMaxMana;
    }

    /**
     * 玩家初始化后调用
     */
    public void setAndScaleHealth() {
        AttributeAPI.setOriBaseAttr(player, Attribute.MAX_HEALTH, maxHealth);
        player.setHealth(health);
        player.setHealthScale(maxHealth);
        player.setHealthScaled(true);
    }

}
