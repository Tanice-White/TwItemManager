package io.github.tanice.twItemManager.manager.pdc;

import io.github.tanice.twItemManager.config.Config;
import io.github.tanice.twItemManager.manager.pdc.impl.BuffPDC;
import io.github.tanice.twItemManager.manager.player.PlayerData;
import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.github.tanice.twItemManager.constance.key.ConsumableAttributeKey.*;

/**
 * 可食用物品的PDC
 */
@Getter
public class ConsumablePDC implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private final double version;

    /* 物品内部名称 */
    private final String innerName;

    /* 食用cd */
    private int cd;

    private final PlayerData changedPlayerData;

    private final List<String> buffNames;

    private final List<String> commands;

    private final List<String> effects;

    public ConsumablePDC(@NotNull String innerName, @NotNull ConfigurationSection cfg) {
        version = Config.version;
        this.innerName = innerName;
        changedPlayerData = PlayerData.newFromConsumable(cfg);
        buffNames = cfg.getStringList(BUFF);
        commands = cfg.getStringList(COMMAND);
        effects = cfg.getStringList(EFFECT);
    }

    /**
     * 可变属性转为MAP
     */
    public @NotNull Map<String, Double> getContentLore() {
        Map<String, Double> contentMap = new HashMap<>();
        // PlayerData
        contentMap.put(HEALTH.toLowerCase(), changedPlayerData.getHealth());
        contentMap.put(MAX_HEALTH.toLowerCase(), changedPlayerData.getMaxHealth());
        contentMap.put(MANA.toLowerCase(), changedPlayerData.getMana());
        contentMap.put(MAX_MANA.toLowerCase(), changedPlayerData.getMaxMana());
        contentMap.put(FOOD.toLowerCase(), changedPlayerData.getFood());
        contentMap.put(SATURATION.toLowerCase(), changedPlayerData.getSaturation());
        contentMap.put(LEVEL.toLowerCase(), changedPlayerData.getLevel());

        contentMap.put(CD.toLowerCase(), (double) cd);

        return contentMap;
    }
}
