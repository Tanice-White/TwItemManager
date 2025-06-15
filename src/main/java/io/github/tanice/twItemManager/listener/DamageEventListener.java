package io.github.tanice.twItemManager.listener;

import io.github.tanice.twItemManager.TwItemManager;
import io.github.tanice.twItemManager.config.Config;
import io.github.tanice.twItemManager.event.TwDamageEvent;
import io.github.tanice.twItemManager.manager.calculator.CombatEntityCalculator;
import io.github.tanice.twItemManager.manager.item.base.impl.Item;
import io.github.tanice.twItemManager.manager.pdc.impl.BuffPDC;
import io.github.tanice.twItemManager.manager.pdc.type.AttributeType;
import io.github.tanice.twItemManager.manager.pdc.type.DamageType;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Random;

import static io.github.tanice.twItemManager.util.Logger.logWarning;
import static io.github.tanice.twItemManager.util.Tool.enumMapToString;

public class DamageEventListener implements Listener {
    private final JavaPlugin plugin;

    /* 防御抵消的伤害乘数 */
    private double k;
    private boolean damageFloat;
    private double floatRange;

    /* 随机数 */
    private final Random random = new Random();

    /* 指示器配置 */
    private boolean enabled;

    private double radiusX, radiusY, radiusZ;
    private double externalHeight;

    private String defaultPrefix;
    private String criticalPrefix;

    private float criticalLargeScale;
    private float viewRange;

    private int delay;
    private int duration;

    public DamageEventListener(JavaPlugin plugin) {
        this.plugin = plugin;

        /* 伤害计算配置 */
        k = Config.worldK;
        damageFloat = Config.damageFloat;
        floatRange = Config.damageFloatRange;

        /* 指示器配置 */
        enabled = Config.generateDamageIndicator;

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
        /* 伤害计算配置 */
        k = Config.worldK;
        damageFloat = Config.damageFloat;
        floatRange = Config.damageFloatRange;

        /* 指示器配置 */
        enabled = Config.generateDamageIndicator;

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
     * 伤害计算
     * 原版的弓箭和附魔不纳入计算
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onEntityDamageByEntityEvent(@NotNull EntityDamageByEntityEvent event) {
        // TODO 检查物品耐久问题
        Entity damager = event.getDamager();
        Entity target = event.getEntity();
        if (damager instanceof Projectile projectile) {
            ProjectileSource source = projectile.getShooter();
            if (source instanceof Entity sourceEntity) damager = sourceEntity;
        }
        if (!(damager instanceof LivingEntity livingD) || !(target instanceof LivingEntity livingT)) return;

        CombatEntityCalculator ac = new CombatEntityCalculator(livingD);
        CombatEntityCalculator bc = new CombatEntityCalculator(livingT);
        EnumMap<AttributeType, Double> aAttrMap = ac.getAttrsValue();
        EnumMap<AttributeType, Double> bAttrMap = bc.getAttrsValue();

        logWarning("attack at: " + enumMapToString(aAttrMap));
        logWarning("attack dt: " + enumMapToString(ac.getDamageTypeMap()));

        /* 计算玩家生效属性 */
        /* 非法属性都在OTHER中 */
        DamageType weaponDamageType = DamageType.OTHER;
        ItemStack itemStack = new ItemStack(Material.AIR);

        EntityEquipment equipment = livingD.getEquipment();
        if (equipment != null) {
            itemStack = equipment.getItemInMainHand();  // 单独写出来用于后续判断
            Item i = TwItemManager.getItemManager().getItemByItemStack(itemStack);
            if (i != null) weaponDamageType = i.getDamageType();
            /* 否则武器类型保持OTHER不变 */
        }

        double eventOriDamage = event.getDamage();
        event.setDamage(0);
        double finalDamage = 0;
        boolean isCritical = false;

        TwDamageEvent twDamageEvent = new TwDamageEvent(livingD, livingT, 0, aAttrMap, bAttrMap, weaponDamageType);
        /* BEFORE_DAMAGE 事件计算 */
        List<BuffPDC> bd = ac.getBeforeList();
        bd.addAll(bc.getBeforeList());
        Collections.sort(bd);
        for (BuffPDC pdc : bd) {
            Object answer = pdc.execute(twDamageEvent);
            if (!(answer instanceof Boolean)) logWarning(".js run 函数返回值错误, 继续执行");
            /* false 表示后续不计算 */
            else if (answer.equals(false)) {
                doDamage(livingT, isCritical, finalDamage);
                return;
            }
        }

        /* 武器的对外白值（品质+宝石+白值） */
        finalDamage = aAttrMap.get(AttributeType.DAMAGE);
        /* 只要是twItemManager的物品，伤害一定是1，否则一定大于1 */
        /* 不是插件物品 */
        /* 原版弓箭伤害就是会飘 */
        if (TwItemManager.getItemManager().isNotTwItem(itemStack)) finalDamage += eventOriDamage;
            /* 怪物拿起的武器不受这个影响，伤害是满额的 */
            /* 所以玩家才计算比例 */
        else if (damager instanceof Player) {
            double ck = 1;
            /* 处理跳劈伤害 */
            if (event.isCritical()) ck = 1.5;
            finalDamage *= eventOriDamage / ck;
        }

        finalDamage *=  (1 + ac.getDamageTypeMap().getOrDefault(weaponDamageType, 0D));


        /* 暴击事件 + 修正 */
        if (random.nextDouble() < aAttrMap.get(AttributeType.CRITICAL_STRIKE_CHANCE)){
            finalDamage *= aAttrMap.get(AttributeType.CRITICAL_STRIKE_DAMAGE) < 1 ? 1: aAttrMap.get(AttributeType.CRITICAL_STRIKE_DAMAGE);
            isCritical = true;
        }

        /* 伤害浮动 */
        if (damageFloat) finalDamage *= random.nextDouble(1 - floatRange, 1 + floatRange);

        twDamageEvent.setDamage(finalDamage);

        /* 中间属性生效 */
        List<BuffPDC> be = ac.getBeforeList();
        be.addAll(bc.getBeforeList());
        Collections.sort(be);
        for (BuffPDC pdc : be) {
            Object answer = pdc.execute(twDamageEvent);
            /* 可能被更改 */
            finalDamage = twDamageEvent.getDamage();
            if (!(answer instanceof Boolean)) logWarning(".js run 函数返回值错误, 继续执行");
            else if (answer.equals(false)) {
                doDamage(livingT, isCritical, finalDamage);
                return;
            }
        }

        /* 最终伤害计算 */
        finalDamage *= (1 - bAttrMap.get(AttributeType.PRE_ARMOR_REDUCTION));
        finalDamage -= bAttrMap.get(AttributeType.ARMOR) * k;
        /* 数值修正 */
        finalDamage = Math.max(0, finalDamage);
        finalDamage *= (1 - bAttrMap.get(AttributeType.AFTER_ARMOR_REDUCTION));

        /* AFTER_DAMAGE 事件计算 */
        twDamageEvent.setDamage(finalDamage);

        List<BuffPDC> ad = ac.getAfterList();
        ad.addAll(bc.getAfterList());
        Collections.sort(ad);
        for (BuffPDC pdc : ad) {
            Object answer = pdc.execute(twDamageEvent);
            /* 可能被更改 */
            finalDamage = twDamageEvent.getDamage();
            if (!(answer instanceof Boolean)) logWarning(".js run 函数返回值错误, 继续执行");
            else if (answer.equals(false)) {
                doDamage(livingT, isCritical, finalDamage);
                return;
            }
        }

        /* a 给 b 增加 buff */

        /* TIMER 不处理，而是在增加buff的时候处理 */
        doDamage(livingT, isCritical, finalDamage);
    }

    /**
     * 对target造成真实伤害
     */
    private void doDamage(@NotNull LivingEntity target, boolean isCritical, double damage) {
        target.damage(damage);
        if (damage > 0) generateIndicator(target, isCritical, damage);
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
