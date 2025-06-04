package io.github.tanice.twItemManager.listener;

import io.github.tanice.twItemManager.manager.calculator.CombatEntityCalculator;
import io.github.tanice.twItemManager.manager.pdc.CalculablePDC;
import io.github.tanice.twItemManager.manager.pdc.impl.BuffPDC;
import io.github.tanice.twItemManager.manager.pdc.type.AttributeCalculateSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

import static io.github.tanice.twItemManager.util.Logger.logWarning;

public class DamageEventListener implements Listener {
    /**
     * 最先判断伤害事件(是否取消伤害)
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onDamage(@NotNull EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof LivingEntity a) || !(event.getEntity() instanceof LivingEntity b)) return;
        CombatEntityCalculator ac = new CombatEntityCalculator(a);
        CombatEntityCalculator bc = new CombatEntityCalculator(b);

        /* BEFORE_DAMAGE 事件计算 */
        List<BuffPDC> bd = ac.getBeforeList();
        bd.addAll(bc.getBeforeList());
        Collections.sort(bd);
        for (BuffPDC pdc : bd) {
            Object answer = pdc.execute(a, b, 0);
            if (!(answer instanceof Boolean)) logWarning(".js run 函数返回值错误, 继续执行");
            else if (answer.equals(true)) {
                /* 返回 true 则表示后续伤害事件取消 */
                event.setCancelled(true);
                return;
            }
        }

        /* 最终伤害计算 */


        /* AFTER_DAMAGE 事件计算 */
        List<BuffPDC> ad = ac.getAfterList();
        ad.addAll(bc.getAfterList());
        Collections.sort(ad);
        for (BuffPDC pdc : ad) {
            Object answer = pdc.execute(a, b, 0);
            if (!(answer instanceof Boolean)) logWarning(".js run 函数返回值错误, 继续执行");
            else if (answer.equals(true)) {
                /* 返回 true 则表示后续伤害事件取消 */
                event.setCancelled(true);
                return;
            }
        }

        /* a 给 b 增加 buff */

        /* TIMER 不处理，而是在增加buff的时候处理 */
    }
}
