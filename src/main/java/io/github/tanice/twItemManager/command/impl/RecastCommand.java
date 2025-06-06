package io.github.tanice.twItemManager.command.impl;

import io.github.tanice.twItemManager.TwItemManager;
import io.github.tanice.twItemManager.command.SubCommand;
import org.bukkit.Material;
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
        if (item.getType() == Material.AIR || TwItemManager.getItemManager().isNotTwItem(item)) {
            sender.sendMessage("§c请手持不可堆叠的TwItem执行此命令");
            return true;
        }
        boolean ok = TwItemManager.getItemManager().recast(item);
        if (ok) sender.sendMessage("§a重铸成功");
        else sender.sendMessage("§c重铸失败");
        return true;
    }
}
