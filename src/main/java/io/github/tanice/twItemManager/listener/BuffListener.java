package io.github.tanice.twItemManager.listener;

import io.github.tanice.twItemManager.TwItemManager;
import io.github.tanice.twItemManager.manager.item.base.impl.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class BuffListener implements Listener {

    private final List<Player> checkPlayers = new ArrayList<>();

    /**
     * 创建holdBuff检测，防止刷出无限buff
     */
    public BuffListener() {

    }

    @EventHandler
    void onPlayerJoin(@NotNull PlayerJoinEvent event) {
        checkPlayers.add(event.getPlayer());
    }
    /**
     * 手持 buff 生效
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onItemHeld(@NotNull PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        ItemStack previousItem = player.getInventory().getItem(event.getPreviousSlot());
        ItemStack newItem = player.getInventory().getItem(event.getNewSlot());
        /* 让buff生效 */
        changeBuff(player, previousItem, newItem);
    }

    /**
     * 玩家物品的 HoldBuff 生效
     */
    private void changeBuff(@NotNull Player p, @Nullable ItemStack pre, @Nullable ItemStack current) {
        Item i = TwItemManager.getItemManager().getItemByItemStack(pre);
        if (i != null && isValidItem(pre)) TwItemManager.getBuffManager().deactivateBuff(p, i.getHoldBuffs());

        i = TwItemManager.getItemManager().getItemByItemStack(current);
        if (i != null && isValidItem(current)) TwItemManager.getBuffManager().activeBuff(p, i.getHoldBuffs());
    }

    /**
     * 检查物品耐久等方面确保可使用
     */
    public boolean isValidItem(@Nullable ItemStack itemStack) {
        if (itemStack == null) return false;
        return true;
    }
}
