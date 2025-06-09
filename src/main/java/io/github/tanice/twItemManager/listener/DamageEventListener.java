package io.github.tanice.twItemManager.listener;

import io.github.tanice.twItemManager.TwItemManager;
import io.github.tanice.twItemManager.event.TwDamageEvent;
import io.github.tanice.twItemManager.manager.calculator.CombatEntityCalculator;
import io.github.tanice.twItemManager.manager.item.base.impl.Item;
import io.github.tanice.twItemManager.manager.pdc.impl.BuffPDC;
import io.github.tanice.twItemManager.manager.pdc.type.AttributeType;
import io.github.tanice.twItemManager.manager.pdc.type.DamageType;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Random;

import static io.github.tanice.twItemManager.util.Logger.logWarning;
import static io.github.tanice.twItemManager.util.ReflectUtil.setCriticalFalse;
import static io.github.tanice.twItemManager.util.ReflectUtil.setCriticalTrue;
import static io.github.tanice.twItemManager.util.Tool.enumMapToString;
import static io.github.tanice.twItemManager.util.Tool.getMax;

public class DamageEventListener implements Listener {

    /* 防御抵消的伤害乘数 */
    private final double k = 1;

    /* 随机数 */
    private final Random random = new Random();

    public DamageEventListener(JavaPlugin plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    /**
     * 最先判断伤害事件(是否取消伤害)
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onEntityDamageByEntityEvent(@NotNull EntityDamageByEntityEvent event) {
        Entity a = event.getDamager(), b = event.getEntity();
        CombatEntityCalculator ac = new CombatEntityCalculator(a), bc = new CombatEntityCalculator(b);
        if (!(a instanceof LivingEntity) && !(b instanceof LivingEntity)) return;

        double finalDamage;
        /* 计算玩家生效属性 */
        DamageType weaponDamageType = DamageType.OTHER;
        if (a instanceof Player p) {
            Item i = TwItemManager.getItemManager().getItem(p.getInventory().getItemInMainHand());
            if (i != null) weaponDamageType = i.getDamageType();
            else weaponDamageType = getMax(ac.getDamageTypeMap());
        }
        EnumMap<AttributeType, Double> aAttrMap = ac.getAttrsValue();
        EnumMap<AttributeType, Double> bAttrMap = bc.getAttrsValue();

        // TODO 带更多的计算属性，比如 aAttrMap 和 bAttrMap
        TwDamageEvent twDamageEvent = new TwDamageEvent(a, b, 0, new Double[]{0D, 0D, 0D}, weaponDamageType);
        /* BEFORE_DAMAGE 事件计算 */
        List<BuffPDC> bd = ac.getBeforeList();
        bd.addAll(bc.getBeforeList());
        Collections.sort(bd);
        for (BuffPDC pdc : bd) {
            Object answer = pdc.execute(twDamageEvent);
            if (!(answer instanceof Boolean)) logWarning(".js run 函数返回值错误, 继续执行");
            else if (answer.equals(true)) {
                /* 返回 true 则表示后续伤害事件取消 */
                event.setCancelled(true);
                return;
            }
        }

        finalDamage = aAttrMap.get(AttributeType.DAMAGE);
        twDamageEvent.setDamage(finalDamage);
        twDamageEvent.setDefenceAttributeList(new Double[]{
                bAttrMap.get(AttributeType.PRE_ARMOR_REDUCTION),
                bAttrMap.get(AttributeType.ARMOR),
                // bAttrMap.get(AttributeType.ARMOR_TOUGHNESS),  // 护甲韧性暂时不参与计算
                bAttrMap.get(AttributeType.AFTER_ARMOR_REDUCTION),
        });

        /* 暴击事件 + 修正 */
        if (random.nextDouble() < aAttrMap.get(AttributeType.CRITICAL_STRIKE_CHANCE)){
            finalDamage *= aAttrMap.get(AttributeType.CRITICAL_STRIKE_DAMAGE) < 1 ? 1: aAttrMap.get(AttributeType.CRITICAL_STRIKE_DAMAGE);
            setCriticalTrue(event);
        }
        else setCriticalFalse(event);

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
        twDamageEvent.setDamage(finalDamage);

        /* AFTER_DAMAGE 事件计算 */
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
        event.setDamage(finalDamage < 1 ? 1 : finalDamage);
    }
}
