package io.github.tanice.twItemManager.command.impl;

import io.github.tanice.twItemManager.TwItemManager;
import io.github.tanice.twItemManager.command.SubCommand;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class CheckCommand extends SubCommand {
    @Override
    public String getName() {
        return "check";
    }
    @Override
    public String getDescription() {
        return "检查玩家主手中物品的itemPDC";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)){
            sender.sendMessage("§c请务必在游戏中执行该命令");
            return true;
        }
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() == Material.AIR || TwItemManager.getItemManager().isNotTwItem(item)) {
            sender.sendMessage("§c请手持TwItem执行此命令");
            return true;
        }
        player.sendMessage("§a[物品itemPDC]" + "\n" + TwItemManager.getItemManager().getItemPDC(item));
        return true;
    }
}
