package io.github.tanice.twItemManager.listener;

import io.github.tanice.twItemManager.TwItemManager;
import io.github.tanice.twItemManager.helper.mythicmobs.TwiDamageMechanic;
import io.lumine.mythic.bukkit.events.MythicMechanicLoadEvent;
import io.lumine.mythic.bukkit.events.MythicSkillEvent;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public class MMSkillListener implements Listener {
    private final JavaPlugin plugin;

    public MMSkillListener(@NotNull JavaPlugin plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    /* 注册自己的解释器 */
    @EventHandler
    public void OnMythicMechanicLoad(@NotNull MythicMechanicLoadEvent event) {
        if (event.getMechanicName().equalsIgnoreCase("twDamage") || event.getEventName().equalsIgnoreCase("twd")) {
            event.register(new TwiDamageMechanic(event.getConfig()));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCastSkill(@NotNull MythicSkillEvent event) {
        event.getSkill().getInternalName();
        // TODO 管理技能cd和mana
        // TODO 需要获取玩家的cd和mana减免属性才能计算最终结果
    }

    // MythicDamageEvent 目前不需要监听, 可能包含其他情况

    @EventHandler
    public void onPlayerJoin(@NotNull PlayerJoinEvent event) {
        TwItemManager.getSkillManager().addPlayerData(event.getPlayer().getUniqueId().toString());
    }

    @EventHandler
    public void onPlayerQuit(@NotNull PlayerQuitEvent event) {
        TwItemManager.getSkillManager().clearPlayerData(event.getPlayer().getUniqueId().toString());
    }
}
