package io.github.tanice.twItemManager.manager.item.base.impl;

import io.github.tanice.twItemManager.infrastructure.PDCAPI;
import io.github.tanice.twItemManager.manager.item.base.BaseItem;
import io.github.tanice.twItemManager.manager.pdc.impl.AttributePDC;
import io.github.tanice.twItemManager.manager.pdc.type.AttributeCalculateSection;
import io.github.tanice.twItemManager.util.MiniMessageUtil;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import static io.github.tanice.twItemManager.constance.key.AttributeKey.SLOT;
import static io.github.tanice.twItemManager.constance.key.ConfigKey.*;
import static io.github.tanice.twItemManager.infrastructure.PDCAPI.setItemCalculablePDC;
import static io.github.tanice.twItemManager.infrastructure.PDCAPI.setSlot;
import static io.github.tanice.twItemManager.util.Logger.logWarning;

public class Gem extends BaseItem {
    /**
     * 依据内部名称和对应的config文件创建物品
     */
    public Gem(@NotNull String innerName, @NotNull ConfigurationSection config) {
        super(innerName, config);
    }

    @Override
    protected void loadUnchangeableVar(@NotNull ConfigurationSection config) {

    }

    @Override
    protected void loadBase(@NotNull ItemMeta meta, @NotNull ConfigurationSection config) {
        meta.displayName(MiniMessageUtil.deserialize(config.getString(DISPLAY_NAME,"")));
        meta.setMaxStackSize(config.getInt(MAX_STACK, 1));
        meta.setUnbreakable(true);
    }

    @Override
    protected void loadPDCs(@NotNull ItemMeta meta, @NotNull ConfigurationSection config) {
        if (!PDCAPI.setItemCalculablePDC(meta, new AttributePDC(innerName , AttributeCalculateSection.BASE, config))) {
            logWarning("GemPDC 设置出错");
        }
        // setSlot(meta ,config.getString(SLOT,"ANY"));
    }
}
