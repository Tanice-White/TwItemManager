package io.github.tanice.twItemManager.listener;

import io.github.tanice.twItemManager.config.Config;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;

import java.util.Random;

/**
 * 伤害指示器
 * TODO 配置文件化
 */
public class DamageIndicatorListener implements Listener {

    private final Random random = new Random();

    private final JavaPlugin plugin;
    private boolean enabled;

    private double radiusX, radiusY, radiusZ;
    private double externalHeight;

    private final String defaultPrefix;
    private final String criticalPrefix;

    private final float criticalLargeScale;
    private final float viewRange;

    private final int delay;
    private final int duration;


    public DamageIndicatorListener(JavaPlugin plugin) {
        this.plugin = plugin;

        radiusX = 0.4;
        radiusY = 0.3;
        radiusZ = 0.4;

        externalHeight = random.nextDouble() * 0.2;

        defaultPrefix = Config.defaultPrefix;
        criticalPrefix = Config.criticalPrefix;

        criticalLargeScale = (float) Config.criticalLargeScale;
        viewRange = (float) Config.viewRange;

        delay = 32;
        duration = 12;

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

        Entity damager = event.getDamager();
        Entity target = event.getEntity();
        if (damager instanceof Projectile projectile) {
            ProjectileSource source = projectile.getShooter();
            if (source instanceof Entity sourceEntity) damager = sourceEntity;
        }
        if (damager instanceof Player || target instanceof Player) generateIndicator(event.getEntity(),event.isCritical(),event.getFinalDamage());
    }

    public void reload(){
        this.enabled = Config.generateDamageIndicator;
    }

    /**
     * 生成伤害指示器
     */
    private void generateIndicator(Entity b, boolean isCritical, double finalDamage) {
        String pf;
        if (isCritical && !criticalPrefix.isEmpty()) pf = criticalPrefix;
        else pf = defaultPrefix;

        Location spawnLocation = randomOffsetLocation(b.getLocation());
        TextDisplay td = b.getWorld().spawn(spawnLocation, TextDisplay.class, e -> {

            e.text(Component.text(pf + String.format("%.1f", finalDamage)));
            e.setBillboard(TextDisplay.Billboard.CENTER);
            e.setSeeThrough(true);
            e.setBackgroundColor(org.bukkit.Color.fromARGB(0, 0, 0, 0));
            e.setShadowed(false);
            e.setViewRange((float) viewRange);
            // 暴击时设置更大的字体
            Matrix4f matrix = new Matrix4f();
            if (isCritical) matrix.scale(criticalLargeScale, criticalLargeScale, 1.5f);
            else matrix.scale(1.5f, 1.5f, 1f);
            e.setTransformationMatrix(matrix);

        });
        double newY = b.getHeight() + externalHeight;
        if (isCritical) newY += externalHeight;

        double copyNewY = newY;

        new BukkitRunnable() {
            final double incrementPerTick = finalDamage / duration;
            double currentDamage = 0D;
            int ticks = 0;

            float progress;

            @Override
            public void run() {
                if (ticks++ >= delay || !td.isValid()) {
                    if (td.isValid()) td.remove();
                    cancel();
                    return;
                }

                if (ticks <= 8) td.teleport(spawnLocation.clone().add(0, copyNewY * ticks / 8, 0));

                if (ticks <= duration) {
                    progress = (float) ticks / duration;
                    currentDamage += incrementPerTick;
                    if (currentDamage > finalDamage) currentDamage = finalDamage;
                    td.text(Component.text(pf + String.format("%.1f", currentDamage)));
                }
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    /**
     *  [0, +radius]
     */
    private @NotNull Location randomOffsetLocation(@NotNull Location center) {
        return center.clone().add(random.nextDouble() * radiusX, random.nextDouble() * radiusY, random.nextDouble() * radiusZ);
    }

}
