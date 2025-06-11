package io.github.tanice.twItemManager.command.impl;

import io.github.tanice.twItemManager.TwItemManager;
import io.github.tanice.twItemManager.command.SubCommand;
import io.github.tanice.twItemManager.manager.item.ItemManager;
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
        ItemManager im = TwItemManager.getItemManager();
        if (!im.isNotTwItem(item)) player.sendMessage("§a\n[物品itemPDC]" + "\n" + im.getItemStringItemPDC(item));
        else if (!im.isNotGem(item)) player.sendMessage("§a\n[物品AttributePDC]" + "\n" + im.getItemStringAttributePDC(item));
        else sender.sendMessage("§c请手持TwItem执行此命令");
        return true;
    }
}
