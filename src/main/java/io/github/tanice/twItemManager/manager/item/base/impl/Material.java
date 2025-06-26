package io.github.tanice.twItemManager.manager.item.base.impl;

import io.github.tanice.twItemManager.manager.item.base.BaseItem;
import io.github.tanice.twItemManager.util.MiniMessageUtil;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import static io.github.tanice.twItemManager.constance.key.ConfigKey.*;
import static io.github.tanice.twItemManager.infrastructure.PDCAPI.updateUpdateCode;

/**
 * 普通的材料
 */
public class Material extends BaseItem {
    public Material(@NotNull String innerName, @NotNull ConfigurationSection config) {
        super(innerName, config);
    }

    @Override
    protected void loadClassValues() {

    }

    @Override
    protected void loadBase(@NotNull ItemMeta meta) {
        meta.displayName(MiniMessageUtil.serialize(cfg.getString(DISPLAY_NAME,"")));
        meta.setMaxStackSize(cfg.getInt(MAX_STACK, 1));
        meta.setUnbreakable(true);
        int cmd = cfg.getInt(CUSTOM_MODEL_DATA);
        if (cmd != 0) meta.setCustomModelData(cmd);
        for (String flagName : cfg.getStringList(HIDE_FLAGS)) meta.addItemFlags(ItemFlag.valueOf("HIDE_" + flagName.toUpperCase()));
    }

    @Override
    protected void loadPDCs(@NotNull ItemMeta meta) {
        updateUpdateCode(meta);
    }

    /**
     * 材料可堆叠，不能使用PDC中的时间戳
     */
    @Override
    public @NotNull ItemStack getItem() {
        return item.clone();
    }
}
