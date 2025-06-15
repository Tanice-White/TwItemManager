package io.github.tanice.twItemManager.command.impl;

import io.github.tanice.twItemManager.TwItemManager;
import io.github.tanice.twItemManager.command.SubCommand;
import io.github.tanice.twItemManager.event.PluginReloadEvent;
import lombok.NoArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

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
    public boolean execute(CommandSender sender, String[] args) {
        TwItemManager.getInstance().onReload();

        Bukkit.getPluginManager().callEvent(new PluginReloadEvent());
        TwItemManager.getInstance().onReload();
        TwItemManager.getInstance().getLogger().info("[TwItems]重载成功");
        sender.sendMessage("§a[TwItems]重载成功");
        return true;
    }
}
