package io.github.tanice.twItemManager.util;

import io.github.tanice.twItemManager.TwItemManager;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class EquipmentSlotGroupUtil {
    /**
     * 判定 slot 的值, 传入 空 或者 null 都返回 EquipmentSlotGroup.ANY
     */
    public static @NotNull EquipmentSlotGroup slotJudge(@Nullable String slot) {
        if (slot == null || slot.isEmpty()) return EquipmentSlotGroup.ANY;
        EquipmentSlotGroup sg = EquipmentSlotGroup.getByName(slot);
        if (sg == null) {
            TwItemManager.getInstance().getLogger().warning("Invalid slot : " + slot + ", use ANY as default");
            return EquipmentSlotGroup.ANY;
        }
        return sg;
    }
}
