package io.github.tanice.twItemManager.util;

import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.lang.reflect.Field;

public class ReflectUtil {
    public static void setCriticalTrue(EntityDamageByEntityEvent event) {
        try {
            Field critical = EntityDamageByEntityEvent.class.getDeclaredField("critical");
            critical.setAccessible(true);
            critical.set(event, true);
        } catch (NoSuchFieldException | IllegalAccessException ignored) {}
    }
}
