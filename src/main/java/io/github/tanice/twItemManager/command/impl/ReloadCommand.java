package io.github.tanice.twItemManager.command.impl;

import io.github.tanice.twItemManager.TwItemManager;
import io.github.tanice.twItemManager.command.SubCommand;
import io.github.tanice.twItemManager.event.TwItemManagerReloadEvent;
import lombok.NoArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import static io.github.tanice.twItemManager.util.Logger.logInfo;

/**
 * 插件重载指令
 */
@NoArgsConstructor
public class ReloadCommand extends SubCommand {
    @Override
    public String getName() {
        return "reload";
    }

    @Override
    public String getDescription() {
        return "插件重载";
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, String[] args) {
        Bukkit.getPluginManager().callEvent(new TwItemManagerReloadEvent());

        TwItemManager.getInstance().onReload();
        logInfo("[TwItems]重载成功");
        sender.sendMessage("§a[TwItems]重载成功");
        return true;
    }
}
