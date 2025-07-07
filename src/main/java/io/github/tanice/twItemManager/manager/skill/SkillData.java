package io.github.tanice.twItemManager.manager.skill;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

import static io.github.tanice.twItemManager.constance.key.ConfigKey.CD;
import static io.github.tanice.twItemManager.constance.key.ConfigKey.MANA_COST;

@Getter
public class SkillData {
    /** 技能名 */
    @NotNull
    private final String skillName;
    /** 技能原本 cd (tick) */
    private final int cd;
    /** 技能原本的蓝耗 */
    private final double manaCost;
    @Setter
    private long nextAbleTimeStamp;

    public SkillData(@NotNull String skillName, @NotNull ConfigurationSection cfg) {
        this.skillName = skillName;
        this.cd = cfg.getInt(CD, 0);
        this.manaCost = cfg.getDouble(MANA_COST, 0D);
        nextAbleTimeStamp = 0;
    }
}
