package io.github.tanice.twItemManager.util;

import io.github.tanice.twItemManager.TwItemManager;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SlotUtil {
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

    /**
     * 判断槽位的有效（slot对应）
     */
    public static boolean mainHandJudge(@Nullable String slot) {
        return slot != null && (slot.equalsIgnoreCase("hand") || slot.equalsIgnoreCase("any") || slot.equalsIgnoreCase("mainHand"));
    }

    public static boolean offHandJudge(@Nullable String slot) {
        return slot != null && (slot.equalsIgnoreCase("hand") || slot.equalsIgnoreCase("any") || slot.equalsIgnoreCase("offHand"));
    }

    public static boolean helmetJudge(@Nullable String slot) {
        return slot != null && (slot.equalsIgnoreCase("head") || slot.equalsIgnoreCase("any") || slot.equalsIgnoreCase("helmet"));
    }

    public static boolean chestJudge(@Nullable String slot) {
        return slot != null && (slot.equalsIgnoreCase("chest") || slot.equalsIgnoreCase("any") || slot.equalsIgnoreCase("chestPlate"));
    }

    public static boolean legsJudge(@Nullable String slot) {
        return slot != null && (slot.equalsIgnoreCase("legs") || slot.equalsIgnoreCase("any") || slot.equalsIgnoreCase("leggings"));
    }

    public static boolean bootsJudge(@Nullable String slot) {
        return slot != null && (slot.equalsIgnoreCase("boots") || slot.equalsIgnoreCase("any") || slot.equalsIgnoreCase("feet"));
    }
}
