package io.github.tanice.twItemManager.command;

import org.bukkit.command.CommandSender;

import java.util.Collections;
import java.util.List;

public abstract class SubCommand {

    // 命令基本信息
    public abstract String getName();
    public String getDescription() { return ""; }
    public String getUsage() { return ""; }
    public String getPermission() { return "twitemmanager.command." + getName().toLowerCase(); }

    // 执行命令
    public abstract boolean execute(CommandSender sender, String[] args);

    // TAB补全
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }

    public void onEnable(){}

    public void onDisable(){}

    public void onReload(){}

    protected boolean hasPermission(CommandSender sender) {
        return sender.hasPermission(getPermission());
    }
}