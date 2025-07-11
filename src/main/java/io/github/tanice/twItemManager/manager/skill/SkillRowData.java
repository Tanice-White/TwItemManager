package io.github.tanice.twItemManager.manager.skill;

import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

import static io.github.tanice.twItemManager.constance.key.ConfigKey.*;

@Getter
public class SkillRowData {
    /** 技能名 */
    @NotNull
    private final String skillName;
    @NotNull
    private final String mythicSkillName;
    /** 技能基础 cd (tick) */
    private final int cd;
    /** 技能基础的蓝耗 */
    private final double manaCost;

    public SkillRowData(@NotNull String skillName, @NotNull ConfigurationSection cfg) {
        this.skillName = skillName;
        String mn = cfg.getString(MYTHIC_SKILL_NAME);
        this.mythicSkillName = mn == null ? skillName : mn;
        this.cd = cfg.getInt(CD, 0);
        this.manaCost = cfg.getDouble(MANA_COST, 0D);
    }
}
