package io.github.tanice.twItemManager.command.impl;

import io.github.tanice.twItemManager.TwItemManager;
import io.github.tanice.twItemManager.command.SubCommand;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class LevelCommand extends SubCommand {

    @Override
    public String getName() {
        return "level";
    }

    @Override
    public String getDescription() {
        return "升级或降级玩家主手上的TwItems(如果此物品有等级模板的话)";
    }

    @Override
    public String getUsage() {
        return "<up|down|set [等级]>";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§c请务必在游戏中执行该命令");
            return true;
        }
        ItemStack item = player.getInventory().getItemInMainHand();

        if (TwItemManager.getItemManager().isNotItem(item)) {
            sender.sendMessage("§c请手持TwItem执行此命令");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage("§c用法: /twi" + getName() + " " + getUsage());
            return true;
        }

        String operation = args[0].toLowerCase();
        switch (operation) {
            case "up":
                TwItemManager.getItemManager().levelUp(player, item, null, false);
                break;
            case "down":
                TwItemManager.getItemManager().levelDown(player, item);
                break;
            case "set":
                String l = args[1];
                if (l == null || l.isEmpty()) {
                    player.sendMessage("§c 请在后方输入具体等级");
                    return true;
                }
                TwItemManager.getItemManager().levelSet(player, item, Integer.parseInt(args[1]));
                break;
            default:
                sender.sendMessage("§c无效操作，请使用:" + getUsage());
                break;
        }
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String @NotNull [] args) {
        if (args.length == 1) {
            List<String> operations = List.of("up", "down", "set");
            return operations.stream().filter(op -> op.startsWith(args[0].toLowerCase())).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
