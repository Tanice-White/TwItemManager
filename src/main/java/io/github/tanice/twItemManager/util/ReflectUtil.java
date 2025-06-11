package io.github.tanice.twItemManager.util;

import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static io.github.tanice.twItemManager.util.Logger.logWarning;

public class ReflectUtil {
    public static void setCriticalTrue(EntityDamageByEntityEvent event) {
        try {
            Field critical = EntityDamageByEntityEvent.class.getDeclaredField("critical");
            critical.setAccessible(true);
            critical.set(event, true);
        } catch (NoSuchFieldException | IllegalAccessException ignored) {
            logWarning("twi 反射critical错误");
        }
    }

    public static void setCriticalFalse(EntityDamageByEntityEvent event) {
        try {
            Field critical = EntityDamageByEntityEvent.class.getDeclaredField("critical");
            critical.setAccessible(true);
            critical.set(event, false);
        } catch (NoSuchFieldException | IllegalAccessException ignored) {
            logWarning("twi 反射critical错误");
        }
    }
}
