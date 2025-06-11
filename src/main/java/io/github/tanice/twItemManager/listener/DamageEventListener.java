package io.github.tanice.twItemManager.listener;

import io.github.tanice.twItemManager.TwItemManager;
import io.github.tanice.twItemManager.config.Config;
import io.github.tanice.twItemManager.event.TwDamageEvent;
import io.github.tanice.twItemManager.manager.calculator.CombatEntityCalculator;
import io.github.tanice.twItemManager.manager.item.base.impl.Item;
import io.github.tanice.twItemManager.manager.pdc.impl.BuffPDC;
import io.github.tanice.twItemManager.manager.pdc.type.AttributeType;
import io.github.tanice.twItemManager.manager.pdc.type.DamageType;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.projectiles.ProjectileSource;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Random;

import static io.github.tanice.twItemManager.util.Logger.logWarning;
import static io.github.tanice.twItemManager.util.ReflectUtil.setCriticalFalse;
import static io.github.tanice.twItemManager.util.ReflectUtil.setCriticalTrue;
import static io.github.tanice.twItemManager.util.Tool.enumMapToString;

public class DamageEventListener implements Listener {

    /* 防御抵消的伤害乘数 */
    private final double k;
    private final boolean damageFloat;
    private final double floatRange;

    /* 随机数 */
    private final Random random = new Random();

    public DamageEventListener(JavaPlugin plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
        k = Config.worldK;
        damageFloat = Config.damageFloat;
        floatRange = Config.damageFloatRange;
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
        if (!(damager instanceof LivingEntity) && !(target instanceof LivingEntity)) return;

        CombatEntityCalculator ac = new CombatEntityCalculator(damager);
        CombatEntityCalculator bc = new CombatEntityCalculator(target);
        EnumMap<AttributeType, Double> aAttrMap = ac.getAttrsValue();
        EnumMap<AttributeType, Double> bAttrMap = bc.getAttrsValue();

        logWarning("attack at: " + enumMapToString(aAttrMap));
        logWarning("attack dt: " + enumMapToString(ac.getDamageTypeMap()));

        /* 计算玩家生效属性 */
        /* 非法属性都在OTHER中 */
        DamageType weaponDamageType = DamageType.OTHER;
        ItemStack itemStack = new ItemStack(Material.AIR);
        if (damager instanceof LivingEntity p) {
            EntityEquipment equipment = p.getEquipment();
            if (equipment != null) {
                itemStack = equipment.getItemInMainHand();  // 单独写出来用于后续判断
                Item i = TwItemManager.getItemManager().getItemByItemStack(itemStack);
                if (i != null) weaponDamageType = i.getDamageType();
                /* 否则武器类型保持OTHER不变 */
            }
        }

        /* 武器的对外白值（品质+宝石+白值） */
        double finalDamage = aAttrMap.get(AttributeType.DAMAGE);
        /* 只要是twItemManager的物品，伤害一定是1，否则一定大于1 */
        /* 不是插件物品 */
        /* 原版弓箭伤害就是会飘 */
        if (TwItemManager.getItemManager().isNotTwItem(itemStack)) finalDamage += event.getDamage();
        /* 怪物拿起的武器不受这个影响，伤害是满额的 */
        /* 所以玩家才计算比例 */
        else if (damager instanceof Player) finalDamage *= event.getDamage();

        finalDamage *=  (1 + ac.getDamageTypeMap().getOrDefault(weaponDamageType, 0D));

        TwDamageEvent twDamageEvent = new TwDamageEvent(damager, target, 0, aAttrMap, bAttrMap, weaponDamageType);
        /* BEFORE_DAMAGE 事件计算 */
        List<BuffPDC> bd = ac.getBeforeList();
        bd.addAll(bc.getBeforeList());
        Collections.sort(bd);
        for (BuffPDC pdc : bd) {
            Object answer = pdc.execute(twDamageEvent);
            if (!(answer instanceof Boolean)) logWarning(".js run 函数返回值错误, 继续执行");
            /* false 表示后续不计算 */
            else if (answer.equals(false)) return;
        }


        /* 暴击事件 + 修正 */
        if (random.nextDouble() < aAttrMap.get(AttributeType.CRITICAL_STRIKE_CHANCE)){
            finalDamage *= aAttrMap.get(AttributeType.CRITICAL_STRIKE_DAMAGE) < 1 ? 1: aAttrMap.get(AttributeType.CRITICAL_STRIKE_DAMAGE);
            setCriticalTrue(event);
        }
        else setCriticalFalse(event);

        /* 伤害浮动 */
        if (damageFloat) finalDamage *= random.nextDouble(1 - floatRange, 1 + floatRange);

        twDamageEvent.setDamage(finalDamage);

        /* 中间属性生效 */
        List<BuffPDC> be = ac.getBeforeList();
        be.addAll(bc.getBeforeList());
        Collections.sort(be);
        for (BuffPDC pdc : be) {
            Object answer = pdc.execute(twDamageEvent);
            if (!(answer instanceof Boolean)) logWarning(".js run 函数返回值错误, 继续执行");
            else if (answer.equals(true)) {
                /* 返回 true 则表示后续伤害事件取消 */
                event.setCancelled(true);
                return;
            }
        }

        /* 最终伤害计算 */
        finalDamage *= (1 - bAttrMap.get(AttributeType.PRE_ARMOR_REDUCTION));
        finalDamage -= bAttrMap.get(AttributeType.ARMOR) * k;
        finalDamage *= (1 - bAttrMap.get(AttributeType.AFTER_ARMOR_REDUCTION));

        /* AFTER_DAMAGE 事件计算 */
        twDamageEvent.setDamage(finalDamage);

        List<BuffPDC> ad = ac.getAfterList();
        ad.addAll(bc.getAfterList());
        Collections.sort(ad);
        for (BuffPDC pdc : ad) {
            Object answer = pdc.execute(twDamageEvent);
            if (!(answer instanceof Boolean)) logWarning(".js run 函数返回值错误, 继续执行");
            else if (answer.equals(true)) {
                /* 返回 true 则表示后续伤害事件取消 */
                event.setCancelled(true);
                return;
            }
        }

        /* a 给 b 增加 buff */

        /* TIMER 不处理，而是在增加buff的时候处理 */
        if (finalDamage < 1) finalDamage = 1;
        event.setDamage(finalDamage);
    }
}
