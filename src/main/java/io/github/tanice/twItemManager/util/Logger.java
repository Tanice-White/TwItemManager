package io.github.tanice.twItemManager.util;

import io.github.tanice.twItemManager.TwItemManager;

public class Logger {
    public static void logInfo(String msg) {
        TwItemManager.getInstance().getLogger().info(msg);
    }

    public static void logWarning(String msg) {
        TwItemManager.getInstance().getLogger().warning(msg);
    }
}
