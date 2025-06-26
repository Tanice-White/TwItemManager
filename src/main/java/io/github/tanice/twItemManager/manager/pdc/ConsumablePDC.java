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
    public @NotNull Map<String, List<String>> getContentLore() {
        Map<String, List<String>> contentMap = new HashMap<>(); // 保持插入顺序
        // PlayerData
        contentMap.put(HEALTH.toLowerCase(), List.of(String.valueOf(changedPlayerData.getHealth())));
        contentMap.put(MAX_HEALTH.toLowerCase(), List.of(String.valueOf(changedPlayerData.getMaxHealth())));
        contentMap.put(MANA.toLowerCase(), List.of(String.valueOf(changedPlayerData.getMana())));
        contentMap.put(MAX_MANA.toLowerCase(), List.of(String.valueOf(changedPlayerData.getMaxMana())));
        contentMap.put(FOOD.toLowerCase(), List.of(String.valueOf(changedPlayerData.getFood())));
        contentMap.put(SATURATION.toLowerCase(), List.of(String.valueOf(changedPlayerData.getSaturation())));
        contentMap.put(LEVEL.toLowerCase(), List.of(String.valueOf(changedPlayerData.getLevel())));

        contentMap.put(CD.toLowerCase(), List.of(String.valueOf(cd)));
        contentMap.put(BUFF.toLowerCase(), buffNames.isEmpty() ? List.of() : new ArrayList<>(buffNames));
        contentMap.put(COMMAND.toLowerCase(), commands.isEmpty() ? List.of() : new ArrayList<>(commands));
        contentMap.put(EFFECT.toLowerCase(), effects.isEmpty() ? List.of() : new ArrayList<>(effects));

        return contentMap;
    }
}
