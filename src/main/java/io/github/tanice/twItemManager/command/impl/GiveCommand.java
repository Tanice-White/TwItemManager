package io.github.tanice.twItemManager.command.impl;

import io.github.tanice.twItemManager.TwItemManager;
import io.github.tanice.twItemManager.command.SubCommand;
import lombok.NoArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 给予物品指令
 * <pre>
 *  <code>/twi give</code> - 查看物品列表
 *  <code>/twi give Def</code> - 搜索包含`Def`的物品列表
 *  <code>/twi give Default-1</code> - 给与 自己 1 个 Default-1 物品
 *  <code>/twi give Default-1 player 5</code> - 给与 player 5 个 Default-1 物品
 * </pre>
 */
@NoArgsConstructor
public class GiveCommand extends SubCommand {
    @Override
    public String getName() {
        return "give";
    }

    @Override
    public String getDescription() {
        return "给予玩家TwItems自定义物品";
    }

    @Override
    public String getUsage() {
        return "<物品ID> [玩家] [数量]";
    }

    @Override
    public boolean execute(CommandSender sender, String @NotNull [] args) {
        // 无参数时显示所有物品
        if (args.length == 0) {
            showItemList(sender, "");
            return true;
        }
        if (args.length == 1) {
            // 检查是否是有效的物品名称
            if (TwItemManager.getItemManager().getItemNameList().contains(args[0])){
                giveItem(sender, args[0], sender.getName(), 1);
                return true;
            } else showItemList(sender, args[0]);
            return true;
        }

        if (args.length >= 2) {
            String itemName = args[0];
            String playerName = args[1];
            int amount = args.length >= 3 ? parseAmount(args[2]) : 1;
            giveItem(sender, itemName, playerName, amount);
        }
        return true;
    }

    public List<String> tabComplete(CommandSender sender, String @NotNull [] args) {
        return switch (args.length) {
            case 0 -> new ArrayList<>(TwItemManager.getItemManager().getItemNameList());

            case 1 -> TwItemManager.getItemManager().getItemNameList().stream()
                    .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());

            case 2 -> Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.startsWith(args[1]))
                    .collect(Collectors.toList());

            case 3 -> Collections.singletonList("1-64");

            default -> Collections.emptyList();
        };
    }

    private void giveItem(CommandSender sender, String itemName, String playerName, int amount) {
        // 验证物品存在性
        if (TwItemManager.getItemManager().isNotTwItem(itemName)) {
            sender.sendMessage("§c物品 " + itemName + " 不存在！");
            return;
        }

        // 获取目标玩家
        Player target = Bukkit.getPlayer(playerName);
        if (target == null) {
            sender.sendMessage("§c玩家 " + playerName + " 不存在或不在线！");
            return;
        }

        // 生成并给予物品
        ItemStack item;
        item = TwItemManager.getItemManager().generateItem(itemName);
        item.setAmount(amount);
        target.getInventory().addItem(item);
        sender.sendMessage("§a成功给予 " + target.getName() + " " + amount + " 个 " + itemName);
    }

    private int parseAmount(String input) {
        try {
            return Math.max(1, Math.min(64, Integer.parseInt(input)));
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    private void showItemList(CommandSender sender, String filter) {
        List<String> items = filterItems(filter);
        if (items.isEmpty()) {
            sender.sendMessage("§c没有找到匹配的物品");
            return;
        }

        sender.sendMessage("§6=== 找到 " + items.size() + " 个物品 ===");
        items.forEach(item -> sender.sendMessage("§7- " + item));
    }

    private List<String> filterItems(String input) {
        return TwItemManager.getItemManager().getItemNameList().stream()
                .filter(name -> name.toLowerCase().contains(input.toLowerCase()))
                .collect(Collectors.toList());
    }
}
