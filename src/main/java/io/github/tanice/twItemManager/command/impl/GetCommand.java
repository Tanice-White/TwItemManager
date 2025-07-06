package io.github.tanice.twItemManager.command.impl;

import io.github.tanice.twItemManager.TwItemManager;
import io.github.tanice.twItemManager.command.SubCommand;
import lombok.NoArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 物品获取指令
 * <pre>
 *  <code>/twi get</code> - 查看所有物品列表
 *  <code>/twi get Def</code> - 搜索包含`Def`的物品列表
 *  <code>/twi get Default-1</code> - 给与自己1个Default-1物品
 *  <code>/twi get Default-1 player 10</code> - 给与player 10个Default-1物品
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
        return "获取TwItems自定义物品";
    }

    @Override
    public String getUsage() {
        return "[物品名称] [玩家] [数量]";
    }

    @Override
    public boolean execute(CommandSender sender, String @NotNull [] args) {
        // 验证执行环境
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§c请在游戏中执行该命令");
            return true;
        }

        // 显示所有物品列表
        if (args.length == 0) {
            showItemList(player, "");
            return true;
        }

        // 处理物品名称
        String itemName = args[0];
        List<String> matches = getMatchingItems(itemName);

        if (matches.isEmpty()) {
            player.sendMessage("§c没有找到匹配 '" + itemName + "' 的物品");
            return true;
        }

        if (matches.size() > 1) {
            showItemList(player, itemName);
            return true;
        }

        String targetName = sender.getName();
        int amount = 1;
        int paramIndex = 1;

        if (args.length > paramIndex) {
            Player targetPlayer = Bukkit.getPlayer(args[paramIndex]);
            if (targetPlayer != null) {
                targetName = targetPlayer.getName();
                paramIndex++;
            }
        }

        if (args.length > paramIndex) {
            try {
                amount = Integer.parseInt(args[paramIndex]);
            } catch (NumberFormatException ignored) {
            }
        }

        giveItem(sender, itemName, targetName, amount);
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String @NotNull [] args) {
        if (args.length == 1) {
            return getMatchingItems(args[0]);
        }

        if (args.length == 2) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.startsWith(args[1]))
                    .collect(Collectors.toList());
        }

        return Collections.emptyList();
    }

    /**
     * 给予物品核心逻辑
     */
    private void giveItem(CommandSender sender, String itemName, String targetName, int amount) {
        if (!TwItemManager.getItemManager().isTwItem(itemName)) {
            sender.sendMessage("§c物品 " + itemName + " 不存在！");
            return;
        }

        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            sender.sendMessage("§c玩家 " + targetName + " 不存在或不在线！");
            return;
        }
        ItemStack item = TwItemManager.getItemManager().generateItem(itemName);
        if (item == null) {
            sender.sendMessage("§c无法生成物品: " + itemName);
            return;
        }

        item.setAmount(amount);
        target.getInventory().addItem(item);
        sender.sendMessage("§a成功给予 " + target.getName() + " §e" + amount + "个§a物品: §b" + itemName);
    }

    /**
     * 显示物品列表
     */
    private void showItemList(CommandSender sender, String filter) {
        List<String> items = getMatchingItems(filter);

        if (items.isEmpty()) {
            sender.sendMessage(filter.isEmpty() ? "§c当前没有可用物品" : "§c没有匹配 '" + filter + "' 的物品");
            return;
        }

        String title = filter.isEmpty() ?
                "所有物品 (" + items.size() + " 个)" :
                "匹配 '" + filter + "' 的物品 (" + items.size() + " 个)";

        sender.sendMessage("§6§l=== " + title + " §6§l===");
        items.forEach(item -> sender.sendMessage("§7- §e" + item));
    }

    /**
     * 获取匹配的物品列表
     */
    private List<String> getMatchingItems(@NotNull String filter) {
        String lowerFilter = filter.toLowerCase();
        return TwItemManager.getItemManager().getItemNameList().stream()
                .filter(name -> name.toLowerCase().contains(lowerFilter))
                .sorted()
                .collect(Collectors.toList());
    }
}