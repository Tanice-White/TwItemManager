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
import java.util.stream.Stream;

/**
 * 给予物品指令
 * <pre>
 *  <code>/twi give item</code> - 查看物品列表
 *  <code>/twi give gem</code> - 查看宝石列表
 *  <code>/twi give item Def</code> - 搜索包含`Def`的物品列表
 *  <code>/twi give gem 钻石</code> - 搜索包含`钻石`的宝石列表
 *  <code>/twi give item Default-1</code> - 给与 自己 1 个 Default-1 物品
 *  <code>/twi give gem 钻石 10</code> - 给与 自己 10 个钻石宝石
 *  <code>/twi give gem 钻石 player 10</code> - 给与 player 10 个钻石宝石
 * </pre>
 */
@NoArgsConstructor
public class GetCommand extends SubCommand {
    @Override
    public String getName() {
        return "get";
    }

    @Override
    public String getDescription() {
        return "给予玩家TwItems自定义物品或宝石";
    }

    @Override
    public String getUsage() {
        return "<item|gem> <内部名称> [玩家|数量] [数量]";
    }

    @Override
    public boolean execute(CommandSender sender, String @NotNull [] args) {
        if (!(sender instanceof Player player)){
            sender.sendMessage("§c请务必在游戏中执行该命令");
            return true;
        }

        // 至少需要物品类型参数
        if (args.length == 0) {
            player.sendMessage("§c用法: /twi give <item|gem> <内部名称> [玩家|数量] [数量]");
            return true;
        }

        // 检查是否是有效的物品类型
        String itemType = args[0].toLowerCase();
        if (!itemType.equals("item") && !itemType.equals("gem")) {
            player.sendMessage("§c无效的物品类型: " + args[0] + "，请使用 item 或 gem");
            return true;
        }

        // 如果只有物品类型参数，显示该类型的所有物品
        if (args.length == 1) {
            showItemList(player, "", itemType);
            return true;
        }

        // 如果有物品类型和物品名称参数，处理给予物品逻辑
        if (args.length >= 2) {
            String itemName = args[1];
            String playerName = sender.getName(); // 默认给执行命令的玩家
            int amount = 1; // 默认数量为1

            // 处理可选的玩家或数量参数
            if (args.length >= 3) {
                // 尝试解析第三个参数为数量
                try {
                    amount = Math.max(1, Math.min(64, Integer.parseInt(args[2])));
                } catch (NumberFormatException e) {
                    // 如果不是有效数字，则认为是玩家名称
                    playerName = args[2];

                    // 如果有第四个参数，则作为数量
                    if (args.length >= 4) {
                        amount = parseAmount(args[3]);
                    }
                }
            }
            giveItem(player, itemName, playerName, amount, itemType);
        }
        return true;
    }

    public List<String> tabComplete(CommandSender sender, String @NotNull [] args) {
        // 物品类型补全
        if (args.length == 1) {
            return Stream.of("item", "gem")
                    .filter(type -> type.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        String itemType = args[0].toLowerCase();

        return switch (args.length - 1) {
            case 0 -> new ArrayList<>(getNameListByTypeName(itemType));

            case 1 -> getNameListByTypeName(itemType).stream()
                    .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());

            case 2 -> {
                // 尝试判断第三个参数是玩家还是数量
                List<String> completions = new ArrayList<>(Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(name -> name.startsWith(args[2]))
                        .toList());

                // 添加可能的数量补全
                if (args[2].isEmpty() || args[2].matches("\\d+")) {
                    completions.add("1-64");
                }

                yield completions;
            }

            case 3 -> {
                // 如果第三个参数是玩家，则第四个参数应为数量
                List<String> completions = new ArrayList<>();
                if (args[3].isEmpty() || args[3].matches("\\d+")) {
                    completions.add("1-64");
                }
                yield completions;
            }

            default -> Collections.emptyList();
        };
    }

    private void giveItem(CommandSender sender, String innerName, String playerName, int amount, String itemType) {
        // 验证物品存在性
        if (isNotValidItem(innerName, itemType)) {
            sender.sendMessage("§c" + itemType + " " + innerName + " 不存在！");
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
        if (itemType.equals("gem")) item = TwItemManager.getItemManager().generateGemItem(innerName);
        else item = TwItemManager.getItemManager().generateItem(innerName);


        if (item == null) {
            sender.sendMessage("§c无法生成 " + itemType + ": " + innerName);
            return;
        }

        item.setAmount(amount);
        target.getInventory().addItem(item);
        sender.sendMessage("§a成功给予 " + target.getName() + " " + amount + " 个 " + itemType + " " + innerName);
    }

    private int parseAmount(String input) {
        try {
            return Math.max(1, Math.min(64, Integer.parseInt(input)));
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    private void showItemList(CommandSender sender, String filter, String itemType) {
        List<String> items = filterItems(filter, itemType);
        if (items.isEmpty()) {
            sender.sendMessage("§c没有找到匹配的" + itemType);
            return;
        }

        sender.sendMessage("§6=== 找到 " + items.size() + " 个" + itemType + " ===");
        items.forEach(item -> sender.sendMessage("§7- " + item));
    }

    private List<String> filterItems(String input, String itemType) {
        return getNameListByTypeName(itemType).stream()
                .filter(name -> name.toLowerCase().contains(input.toLowerCase()))
                .collect(Collectors.toList());
    }

    private List<String> getNameListByTypeName(@NotNull String type) {
        if (type.equals("gem")) return TwItemManager.getItemManager().getGemNameList().stream().toList();
        else return TwItemManager.getItemManager().getItemNameList().stream().toList();
    }

    private boolean isNotValidItem(String innerName, @NotNull String itemType) {
        if (itemType.equals("gem")) {
            return TwItemManager.getItemManager().isNotGem(innerName);
        } else {
            return TwItemManager.getItemManager().isNotTwItem(innerName);
        }
    }
}
