package io.github.tanice.twItemManager.listener;

import io.github.tanice.twItemManager.TwItemManager;
import io.github.tanice.twItemManager.config.Config;
import io.github.tanice.twItemManager.event.TwDamageEvent;
import io.github.tanice.twItemManager.manager.calculator.CombatEntityCalculator;
import io.github.tanice.twItemManager.manager.item.base.impl.Item;
import io.github.tanice.twItemManager.manager.pdc.impl.BuffPDC;
import io.github.tanice.twItemManager.manager.pdc.type.AttributeType;
import io.github.tanice.twItemManager.manager.pdc.type.BuffActiveCondition;
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
import org.bukkit.event.entity.EntityDamageEvent;
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

import static io.github.tanice.twItemManager.util.Logger.logInfo;
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
    private boolean damageIndicatorEnabled;

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
        /* 伤害计算配置 */
        k = Config.worldK;
        damageFloat = Config.damageFloat;
        floatRange = Config.damageFloatRange;

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
        if (!(event.getEntity() instanceof LivingEntity living)) return;
        boolean c = false;
        if (event instanceof EntityDamageByEntityEvent oe) c = oe.isCritical();

        boolean isC = c;
        Bukkit.getScheduler().runTaskLater(plugin, () -> generateIndicator(living, isC, event.getFinalDamage()), 1L);
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

        /* 荆棘伤害取消计算 */
        if (event.getDamageSource().getDamageType() == org.bukkit.damage.DamageType.THORNS) return;

        CombatEntityCalculator ac = new CombatEntityCalculator(livingD);
        CombatEntityCalculator bc = new CombatEntityCalculator(livingT);
        EnumMap<AttributeType, Double> aAttrMap = ac.getAttrsValue();
        EnumMap<AttributeType, Double> bAttrMap = bc.getAttrsValue();

        if (Config.debug) {
            logInfo("[EntityDamageByEntityEvent] attacker attribute map: " + enumMapToString(aAttrMap));
            logInfo("[EntityDamageByEntityEvent] attacker damage type map: " + enumMapToString(ac.getDamageTypeMap()));
        }
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

        double finalDamage = 0;

        TwDamageEvent twDamageEvent = new TwDamageEvent(livingD, livingT, 0, aAttrMap, bAttrMap, weaponDamageType);
        /* BEFORE_DAMAGE 事件计算 */
        List<BuffPDC> bd = ac.getBeforeList(BuffActiveCondition.ATTACKER);
        bd.addAll(bc.getBeforeList(BuffActiveCondition.DEFENDER));
        Collections.sort(bd);
        for (BuffPDC pdc : bd) {
            Object answer = pdc.execute(twDamageEvent);
            /* 可能被更改 */
            finalDamage = twDamageEvent.getDamage();
            if (answer.equals(false)) {
                // doDamage(event, isCritical, finalDamage);
                event.setDamage(finalDamage);
                return;
            }
        }

        /* 武器的对外白值（品质+宝石+白值） */
        finalDamage = aAttrMap.get(AttributeType.DAMAGE);
        /* 只要是twItemManager的物品，伤害一定是1，否则一定大于1 */
        /* 不影响原版武器伤害的任何计算 */
        /* 不是插件物品 */
        /* 原版弓箭伤害就是会飘 */
        if (TwItemManager.getItemManager().isNotTwItem(itemStack)) finalDamage += event.getDamage();
            /* 怪物拿起的武器不受这个影响，伤害是满额的 */
            /* 所以玩家才计算比例 */
        else if (damager instanceof Player p) {
            // TODO 不确定玩家的cooldown会不会受到武器影响，应该要受到武器的影响
            finalDamage *= 0.15 + p.getAttackCooldown() * 0.85;
        }

        finalDamage *=  (1 + ac.getDamageTypeMap().getOrDefault(weaponDamageType, 0D));
        if (event.isCritical()) finalDamage *= 1 + Config.originalCriticalStrikeAddition;

        /* 暴击事件 + 修正 */
        if (random.nextDouble() < aAttrMap.get(AttributeType.CRITICAL_STRIKE_CHANCE)){
            finalDamage *= aAttrMap.get(AttributeType.CRITICAL_STRIKE_DAMAGE) < 1 ? 1: aAttrMap.get(AttributeType.CRITICAL_STRIKE_DAMAGE);
        }

        /* 伤害浮动 */
        if (damageFloat) finalDamage *= random.nextDouble(1 - floatRange, 1 + floatRange);

        twDamageEvent.setDamage(finalDamage);

        /* 中间属性生效 */
        List<BuffPDC> be = ac.getBetweenList(BuffActiveCondition.ATTACKER);
        be.addAll(bc.getBetweenList(BuffActiveCondition.DEFENDER));
        Collections.sort(be);
        for (BuffPDC pdc : be) {
            Object answer = pdc.execute(twDamageEvent);
            /* 可能被更改 */
            finalDamage = twDamageEvent.getDamage();
            if (answer.equals(false)) {
                // doDamage(event, isCritical, finalDamage);
                event.setDamage(finalDamage);
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

        List<BuffPDC> ad = ac.getAfterList(BuffActiveCondition.ATTACKER);
        ad.addAll(bc.getAfterList(BuffActiveCondition.DEFENDER));
        Collections.sort(ad);
        for (BuffPDC pdc : ad) {
            Object answer = pdc.execute(twDamageEvent);
            /* 可能被更改 */
            finalDamage = twDamageEvent.getDamage();
            if (answer.equals(false)) {
                // doDamage(event, isCritical, finalDamage);
                event.setDamage(finalDamage);
                return;
            }
        }

        /* a 给 b 增加 buff */
        TwItemManager.getBuffManager().doAttackBuffs(livingD, livingT);
        /* b 给 a 增加 buff */
        TwItemManager.getBuffManager().doDefenceBuffs(livingD, livingT);

        /* TIMER 不处理，而是在增加buff的时候处理 */
        // doDamage(event, isCritical, finalDamage);
        event.setDamage(finalDamage);
    }

    /**
     * 对target造成真实伤害
     */
    @Deprecated
    private void doDamage(@NotNull EntityDamageEvent event, boolean isCritical, double damage) {
        if (damage <= 0) {
            event.setDamage(0);
            return;
        }

        LivingEntity le = (LivingEntity) event.getEntity();
        double finalDamage = event.getFinalDamage();
        if (finalDamage > damage) finalDamage = damage;
        double deltaDamage = damage - finalDamage;
        double health = le.getHealth();

        /* 原版伤害致死则不需要补充伤害 */
        if (health > finalDamage) {
            double dd = health - finalDamage;
            le.setHealth(health - Math.min(dd, deltaDamage)); /* 补充伤害 */
        }

        if (damageIndicatorEnabled) generateIndicator(event.getEntity(), isCritical, damage);
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
