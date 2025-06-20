package io.github.tanice.twItemManager.manager.item.base.impl;

import io.github.tanice.twItemManager.manager.item.base.BaseItem;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

/**
 * 右键使用可产生效果的物品
 */
public class Consumable extends BaseItem {

    public Consumable(@NotNull String innerName, @NotNull ConfigurationSection cfg) {
        super(innerName, cfg);
    }

    @Override
    protected void loadClassValues() {

    }

    @Override
    protected void loadBase(@NotNull ItemMeta meta) {

    }

    @Override
    protected void loadPDCs(@NotNull ItemMeta meta) {

    }
}
