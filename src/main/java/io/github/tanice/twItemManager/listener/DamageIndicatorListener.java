package io.github.tanice.twItemManager.listener;

import io.github.tanice.twItemManager.config.Config;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * 伤害指示器
 * TODO 配置文件化
 */
public class DamageIndicatorListener implements Listener {

    private JavaPlugin plugin;
    private double radiusX, radiusY, radiusZ, yOffset;
    private String defaultPrefix, criticalPrefix;
    private int delay;
    private boolean enabled;

    public DamageIndicatorListener(JavaPlugin plugin) {
        this.plugin = plugin;
        radiusX = 0.8;
        radiusY = 0.6;
        radiusZ = 0.8;
        yOffset = 1.2;
        defaultPrefix = "§6";
        criticalPrefix = "§4";
        delay = 30;
        reload();
        Bukkit.getPluginManager().registerEvents(this, plugin);
        plugin.getLogger().info("DamageIndicatorListener Registered");
    }

    /**
     * 最后生成伤害指示器配置 如果事件取消，则不再调用
     * 抛射物需要单独判断
     */
    @EventHandler(priority = EventPriority.HIGHEST,ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if(!enabled) return;
        if (event.getFinalDamage() <= 0) return;

        boolean f = event.getDamager() instanceof Player;
        // 判断是否为投掷物
        if (!f || event.getDamager() instanceof Projectile) {
            ProjectileSource source = ((Projectile)event.getDamager()).getShooter();
            if (source instanceof Player) f = true;
        }

        if (f) generateIndicator(event.getEntity(),event.isCritical(),event.getFinalDamage());
    }

    public void reload(){
        this.enabled = Config.generateDamageIndicator;
    }

    /**
     * 生成伤害指示器
     */
    private void generateIndicator(Entity b,boolean isCritical, double finalDamage){
        String pf;
        if(isCritical && !criticalPrefix.isEmpty()) pf = criticalPrefix;
        else pf = defaultPrefix;

        // TODO MiniMessage 组件
        TextDisplay td = b.getWorld().spawn(randomOffsetLocation(b.getLocation()), TextDisplay.class, e ->{
            e.text(Component.text(pf + String.format("%.1f", finalDamage)));
            e.setBillboard(TextDisplay.Billboard.CENTER);
            e.setSeeThrough(true);
            e.setBackgroundColor(org.bukkit.Color.fromARGB(0, 0, 0, 0));
            e.setShadowed(false);
        });

        // 自动销毁
        new BukkitRunnable() {public void run() {if (td != null && td.isValid()) td.remove();}}.runTaskLater(plugin, delay);
    }

    /**
     *  [-radius, +radius]
     */
    private Location randomOffsetLocation(Location center) {
        double ox = (Math.random() * 2 - 1) * radiusX;
        double oy = (Math.random() * 2 - 1) * radiusY;
        double oz = (Math.random() * 2 - 1) * radiusZ;
        return center.clone().add(ox, oy + yOffset, oz);
    }

}
