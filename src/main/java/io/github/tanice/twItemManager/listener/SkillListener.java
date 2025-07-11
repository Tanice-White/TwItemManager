package io.github.tanice.twItemManager.listener;

import io.github.tanice.twItemManager.TwItemManager;
import io.github.tanice.twItemManager.event.entity.PlayerSkillChangeEvent;
import io.github.tanice.twItemManager.helper.mythicmobs.TwiDamageMechanic;
import io.github.tanice.twItemManager.manager.skill.Trigger;
import io.lumine.mythic.bukkit.events.MythicMechanicLoadEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import static io.github.tanice.twItemManager.util.Logger.logInfo;

public class SkillListener implements Listener {
    private final JavaPlugin plugin;

    public SkillListener(@NotNull JavaPlugin plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
        logInfo("SkillListener loaded");
    }

    /**
     * 注册自己的解释器
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onMythicMechanicLoad(@NotNull MythicMechanicLoadEvent event) {
        if (event.getMechanicName().equalsIgnoreCase("twDamage") || event.getEventName().equalsIgnoreCase("twd")) {
            event.register(new TwiDamageMechanic(event.getConfig()));
        }
    }
    // MythicSkillEvent 不能监听，每一个skill都会触发，而一个真正意义上的技能内部会有多个skill，难以判断
    // MythicDamageEvent 目前不需要监听, 可能包含其他情况

    /**
     * 更改玩家可释放的技能
     * PlayerSkillChangeEvent 都会在 EntityAttributeChangeEvent 被提交后被 call
     * 所以本质上, 除了以下三个, 可以什么都不写
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerSkillChange(@NotNull PlayerSkillChangeEvent event) {
        TwItemManager.getSkillManager().submitAsyncTask(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(@NotNull PlayerJoinEvent event) {
        TwItemManager.getSkillManager().addPlayerData(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerQuit(@NotNull PlayerQuitEvent event) {
        TwItemManager.getSkillManager().clearPlayerData(event.getPlayer().getUniqueId());
    }

    /* 玩家释放技能监听 */

    /**
     * 下蹲或起身
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerShift(@NotNull PlayerToggleSneakEvent event) {
        // TODO 释放技能
    }

    /**
     * 右键 / 蹲 + 右键 / 左键 / 蹲 + 左键 / 跳 + 左键 / 跳 + 右键
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerJump(@NotNull PlayerInteractEvent event) {
        Action action = event.getAction();
        Player player = event.getPlayer();
        Trigger trigger;
        if (action.isLeftClick()) {
            if (player.isJumping()) trigger = Trigger.SPACE_LEFT;
            else {
                if (player.isSneaking()) trigger = Trigger.SHIFT_LEFT;
                else trigger = Trigger.LEFT_CLICK;
            }
        } else {
            if (player.isJumping()) trigger = Trigger.SPACE_RIGHT;
            else {
                if (player.isSneaking()) trigger = Trigger.SHIFT_RIGHT;
                else trigger = Trigger.RIGHT_CLICK;
            }
        }
        // TODO 释放技能
    }

    /**
     * 弓箭离弦
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onShoot(@NotNull EntityShootBowEvent event) {
        // TODO 释放技能
    }
}
