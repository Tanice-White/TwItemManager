package io.github.tanice.twItemManager.manager.item.level;

import io.github.tanice.twItemManager.manager.pdc.impl.AttributePDC;
import io.github.tanice.twItemManager.manager.pdc.type.AttributeAdditionFromType;
import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

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
        begin = cfg.getInt("begin", 0);
        max = cfg.getInt("max", 100);
        chance = cfg.getDouble("chance", 1);
        /* items inner name */
        levelUpNeed = cfg.getString("level-up-need");
        levelDownWhenFailed = cfg.getBoolean("level-down-when-failed", false);
        attributePDC = new AttributePDC(innerName, AttributeAdditionFromType.LEVEL, cfg);
    }
}
