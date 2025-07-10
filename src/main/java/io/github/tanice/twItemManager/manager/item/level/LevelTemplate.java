package io.github.tanice.twItemManager.manager.item.level;

import io.github.tanice.twItemManager.pdc.impl.AttributePDC;
import io.github.tanice.twItemManager.pdc.type.AttributeCalculateSection;
import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

import static io.github.tanice.twItemManager.constance.key.ConfigKey.*;

@Getter
public class LevelTemplate {
    private final String innerName;
    private final int begin;
    private final int max;
    private final double chance;
    /* 升级所需物品的内部名 */
    private final String levelUpNeed;
    private final boolean levelDownWhenFailed;
    private final AttributePDC attributePDC;

    public LevelTemplate(@NotNull String innerName, @NotNull ConfigurationSection cfg) {
        this.innerName = innerName;
        begin = cfg.getInt(BEGIN, 0);
        max = cfg.getInt(MAX, 100);
        chance = cfg.getDouble(CHANCE, 1);
        /* items inner name */
        levelUpNeed = cfg.getString(LEVEL_UP_NEED, "");
        levelDownWhenFailed = cfg.getBoolean(LEVEL_DOWN_WHEN_FAILED, false);
        attributePDC = new AttributePDC(innerName, AttributeCalculateSection.valueOf(cfg.getString(ACS, "BASE").toUpperCase()), cfg);
    }
}
