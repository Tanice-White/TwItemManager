package io.github.tanice.twItemManager.command;

import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

public class MainCommand implements CommandExecutor, TabCompleter {
    private final JavaPlugin plugin;
    private final Map<String, SubCommand> subCommands = new HashMap<>();

    public MainCommand(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void onEnable() {
        PluginCommand command = plugin.getCommand("twitemmanager");
        command.setExecutor(this);
        command.setTabCompleter(this);
        subCommands.values().stream().filter(subCommand -> subCommand instanceof Listener).forEach(subCommand -> Bukkit.getPluginManager().registerEvents((Listener) subCommand, plugin));
        subCommands.values().forEach(SubCommand::onEnable);
        plugin.getLogger().info("Load " + subCommands.size() + " Commands");
    }

    public void onReload() {
        subCommands.values().forEach(SubCommand::onReload);
    }

    public void onDisable() {
        subCommands.values().forEach(SubCommand::onDisable);
    }

    public void register(SubCommand command) {
        subCommands.put(command.getName().toLowerCase(), command);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String @NotNull [] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        SubCommand subCommand = subCommands.get(args[0].toLowerCase());
        if (subCommand == null) {
            // 显示错误信息后列出可用命令
            sender.sendMessage("§c未知的子命令！可用命令：");

            // 获取有权限的子命令列表
            List<String> availableCommands = subCommands.values().stream()
                    .filter(c -> c.hasPermission(sender)) // 过滤有权限的命令
                    .map(SubCommand::getName) // 获取命令名称
                    .sorted() // 按字母排序
                    .collect(Collectors.toList());

            if (availableCommands.isEmpty()) {
                sender.sendMessage("§c你没有可用的子命令权限");
            } else {
                sender.sendMessage("§a" + String.join("§7, §a", availableCommands));
            }
            return true;
        }

        if (!subCommand.hasPermission(sender)) {
            sender.sendMessage("§c你没有执行该指令的权限！");
            sender.sendMessage("§c所需权限：§e" + subCommand.getPermission());
            return true;
        }

        // 移除子命令参数
        String[] subArgs = Arrays.copyOfRange(args, 1, args.length);
        return subCommand.execute(sender, subArgs);
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        if (args.length == 1) { // 主命令后的第一个参数
            // 只补全子命令名称
            return subCommands.values().stream()
                    .filter(c -> c.hasPermission(sender))
                    .map(SubCommand::getName)
                    .filter(name -> name.startsWith(args[0].toLowerCase()))
                    .sorted()
                    .collect(Collectors.toList());
        }

        SubCommand subCommand = subCommands.get(args[0].toLowerCase());
        if (subCommand != null) {
            // 注意这里要传递正确的参数索引
            String[] subArgs = Arrays.copyOfRange(args, 1, args.length);
            return subCommand.tabComplete(sender, subArgs);
        }
        return Collections.emptyList();
    }

    private void sendHelp(@NotNull CommandSender sender) {
        sender.sendMessage("§6=== TwItems Help ===");
        subCommands.values().forEach(cmd ->
                sender.sendMessage(String.format("§7/%s §b%s §f- %s", cmd.getName(), cmd.getUsage(), cmd.getDescription()))
        );
    }
}