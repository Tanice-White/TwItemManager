package io.github.tanice.twItemManager.listener;

import io.github.tanice.twItemManager.config.Config;
import io.github.tanice.twItemManager.event.TwEntityDamageByEntityEvent;
import io.github.tanice.twItemManager.event.TwEntityDamageEvent;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;

import java.util.Random;

public class DamageListener implements Listener {
    private final JavaPlugin plugin;

    /* 随机数 */
    private final Random random = new Random();

    /* 指示器配置 */
    private boolean damageIndicatorEnabled;

    private double radiusX, radiusY, radiusZ;
    private double externalHeight;

    private String defaultPrefix;
    private String criticalPrefix;

    private float criticalLargeScale;
    private float viewRange;

    private int delay;
    private int duration;

    public DamageListener(JavaPlugin plugin) {
        this.plugin = plugin;

        /* 指示器配置 */
        damageIndicatorEnabled = Config.generateDamageIndicator;

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
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void onReload() {
        /* 指示器配置 */
        damageIndicatorEnabled = Config.generateDamageIndicator;

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

        plugin.getLogger().info("DamageEventListener reloaded");
    }

    /**
     * 实体受伤检测
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(@NotNull EntityDamageEvent event) {
        if (!(event.getEntity() instanceof LivingEntity entity)) return;
        if (event.getFinalDamage() <= 0) return;

        if (event instanceof EntityDamageByEntityEvent) return;
//        Bukkit.getPluginManager().callEvent(new TwEntityDamageEvent(entity, event.getDamage()));
    }

    /**
     * 伤害计算
     * 原版的弓箭和附魔不纳入计算
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onEntityDamageByEntityEvent(@NotNull EntityDamageByEntityEvent event) {
        Entity damager = event.getDamager();
        Entity target = event.getEntity();
        if (damager instanceof Projectile projectile) {
            ProjectileSource source = projectile.getShooter();
            if (source instanceof Entity sourceEntity) damager = sourceEntity;
        }
        if (!(damager instanceof LivingEntity livingD) || !(target instanceof LivingEntity livingT)) return;

        /* 荆棘伤害取消计算 */
        if (event.getDamageSource().getDamageType() == org.bukkit.damage.DamageType.THORNS) return;

        TwEntityDamageByEntityEvent twEntityDamageByEntityEvent = new TwEntityDamageByEntityEvent(livingD, livingT, event.getDamage(), event.isCritical());
        Bukkit.getPluginManager().callEvent(twEntityDamageByEntityEvent);
        /* 取消原本的事件 */
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTwDamage(@NotNull TwEntityDamageEvent event) {
        double finalDamage = event.getFinalDamage();
        event.getDefender().damage(finalDamage);

        if (damageIndicatorEnabled) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> generateIndicator(event.getDefender(), event.isCritical(), finalDamage), 1L);
        }
        // TODO 设置死亡原因
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
            e.setViewRange(viewRange);
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
