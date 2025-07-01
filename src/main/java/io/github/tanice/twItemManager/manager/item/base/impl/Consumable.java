package io.github.tanice.twItemManager.manager.item.base.impl;

import io.github.tanice.twItemManager.manager.item.base.BaseItem;
import io.github.tanice.twItemManager.util.MiniMessageUtil;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import static io.github.tanice.twItemManager.infrastructure.PDCAPI.updateUpdateCode;

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
        meta.displayName(MiniMessageUtil.serialize(displayName));
    }

    @Override
    protected void loadPDCs(@NotNull ItemMeta meta) {
        updateUpdateCode(meta);
    }
}
