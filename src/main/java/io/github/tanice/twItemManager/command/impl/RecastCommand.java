package io.github.tanice.twItemManager.command.impl;

import io.github.tanice.twItemManager.TwItemManager;
import io.github.tanice.twItemManager.command.SubCommand;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class RecastCommand extends SubCommand {
    @Override
    public String getName() {
        return "recast";
    }

    @Override
    public String getDescription() {
        return "将玩家主手中的TwItems重铸";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)){
            sender.sendMessage("§c请务必在游戏中执行该命令");
            return true;
        }
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType().isAir() || TwItemManager.getItemManager().isNotItemClassInTwItem(item)) {
            sender.sendMessage("§c请手持TwItem执行此命令");
            return true;
        }
        TwItemManager.getItemManager().recast(player, item);
        return true;
    }
}
