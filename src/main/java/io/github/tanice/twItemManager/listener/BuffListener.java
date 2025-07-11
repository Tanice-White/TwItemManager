package io.github.tanice.twItemManager.listener;

import io.github.tanice.twItemManager.TwItemManager;
import io.github.tanice.twItemManager.event.entity.EntityAttributeChangeEvent;
import io.github.tanice.twItemManager.manager.item.base.BaseItem;
import io.github.tanice.twItemManager.manager.item.base.impl.Item;
import io.github.tanice.twItemManager.util.EquipmentUtil;
import io.papermc.paper.event.entity.EntityEquipmentChangedEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import static io.github.tanice.twItemManager.util.Logger.logInfo;

public class BuffListener implements Listener {
    private final JavaPlugin plugin;
    
    public BuffListener(JavaPlugin plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, TwItemManager.getInstance());
        logInfo("BuffListener loaded");
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerJoin(@NotNull PlayerJoinEvent event) {
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            Player player = event.getPlayer();
            TwItemManager.getBuffManager().loadPlayerBuffs(player);
            Bukkit.getPluginManager().callEvent(new EntityAttributeChangeEvent(player));
        }, 1L);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerQuit(@NotNull PlayerQuitEvent event) {
        Player player = event.getPlayer();
        TwItemManager.getBuffManager().SaveAndClearPlayerBuffs(player);
        Bukkit.getPluginManager().callEvent(new EntityAttributeChangeEvent(player));
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(@NotNull PlayerDeathEvent event) {
        Player player = event.getPlayer();
        TwItemManager.getBuffManager().deactivateEntityBuffs(event.getEntity());
        Bukkit.getPluginManager().callEvent(new EntityAttributeChangeEvent(player));
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerRespawn(@NotNull PlayerRespawnEvent event) {
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            Player player = event.getPlayer();
            /* 激活hold_buff */
            for (Item i : EquipmentUtil.getActiveEquipmentItem(player)){
                TwItemManager.getBuffManager().activateBuffs(player, i.getHoldBuffs(), true);
            }
            Bukkit.getPluginManager().callEvent(new EntityAttributeChangeEvent(player));
        }, 1L);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityEquipmentChange(@NotNull EntityEquipmentChangedEvent event) {
        /* 它的old和new不会换位（不是原物品的引用） */
        if (isTwItemChange(event)) {
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                LivingEntity entity = event.getEntity();
                BaseItem bit;
                for (EntityEquipmentChangedEvent.EquipmentChange equipmentChange : event.getEquipmentChanges().values()) {
                    /* 旧物品 buff 移除 */
                    bit = TwItemManager.getItemManager().getBaseItem(equipmentChange.oldItem());
                    if (bit instanceof Item i) TwItemManager.getBuffManager().deactivateBuffs(entity, i.getHoldBuffs());
                    /* 新物品 buff 增加 */
                    bit = TwItemManager.getItemManager().getBaseItem(equipmentChange.newItem());
                    if (bit instanceof Item i) TwItemManager.getBuffManager().activateBuffs(entity, i.getHoldBuffs(), true);
                }
                Bukkit.getPluginManager().callEvent(new EntityAttributeChangeEvent(entity));
            }, 1L);
        }
    }

    /**
     * 如果是TwItem, 那么耐久度是否还能使用
     */
    private boolean isTwItemChange(@NotNull EntityEquipmentChangedEvent event) {
        for (EntityEquipmentChangedEvent.EquipmentChange c : event.getEquipmentChanges().values()) {
            // TODO 如果是 TwItem，判断耐久度， 否则 return true
            return true;
        }
        return false;
    }
} 