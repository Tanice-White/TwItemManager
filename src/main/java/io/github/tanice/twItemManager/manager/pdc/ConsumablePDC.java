package io.github.tanice.twItemManager.manager.pdc;

import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

import java.io.Serial;
import java.io.Serializable;

/**
 * 可食用物品的PDC
 */
public class ConsumablePDC implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /* 物品内部名称 */
    @Getter
    private String innerName;
    /* 食用cd */
    @Getter
    private int cd;

    /* TODO 食用物品可改变的：最大生命值，血量，mana，最大mana，临时buff，饱食度，指令执行 */
    /* maxHealth health mana maxMana buff food command */

    private ConfigurationSection cfg;

    public ConsumablePDC(@NotNull String innerName, @NotNull ConfigurationSection config) {
        this.innerName = innerName;
        this.cfg = config;
    }
}
