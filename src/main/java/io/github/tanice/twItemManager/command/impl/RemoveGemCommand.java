package io.github.tanice.twItemManager.command.impl;

import io.github.tanice.twItemManager.TwItemManager;
import io.github.tanice.twItemManager.command.SubCommand;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class RemoveGemCommand extends SubCommand {
    @Override
    public String getName() {
        return "remove";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)){
            sender.sendMessage("§c请务必在游戏中执行该命令");
            return true;
        }
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType().isAir() || TwItemManager.getItemManager().isNotItemClassInTwItem(item)) {
            sender.sendMessage("§c请手持TwItemManager插件物品执行此命令");
            return true;
        }
        ItemStack gem = TwItemManager.getItemManager().removeGem(player,item);
        player.getInventory().addItem(gem);
        return true;
    }
}
