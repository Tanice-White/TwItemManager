package io.github.tanice.twItemManager.command.impl;

import io.github.tanice.twItemManager.TwItemManager;
import io.github.tanice.twItemManager.command.SubCommand;
import io.github.tanice.twItemManager.manager.item.ItemManager;
import io.github.tanice.twItemManager.manager.item.base.BaseItem;
import io.github.tanice.twItemManager.manager.item.base.impl.Item;
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
        ItemManager im = TwItemManager.getItemManager();

        ItemStack item = player.getInventory().getItemInMainHand();
        BaseItem bit = im.getBaseItem(item);
        if (!item.getType().isAir()) {
            player.sendMessage("§a\n物品[PDC]" + "\n" + im.getCalculablePDCAsString(item));
            if (bit instanceof Item it) {
                player.sendMessage("hold_buff: " + it.getHoldBuffs());
                player.sendMessage("attack_buff: " + it.getAttackBuffs());
                player.sendMessage("defence_buff: " + it.getDefenseBuffs());
            }
        }

        else player.sendMessage("§a\n玩家[PDC]" + "\n" + im.getCalculablePDCAsString(player));

        return true;
    }
}
