package io.github.tanice.twItemManager.manager.calculator;

import io.github.tanice.twItemManager.manager.pdc.impl.ItemPDC;
import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@Getter
public class CombatPowerCalculator extends Calculator {
    /**
     * 世界配置文件设置的自定义难度系数
     */
    private double difficulty;

    /**
     * 计算出攻击方最终的伤害
     * @param entity 伤害的来源实体
     * @param percentage 传入事件的 getDamage() 即可，因为原版伤害都是1点(必须要绑定一个原版modifier才行，否则是正常的伤害)
     * @return 伤害值
     */
    @Override
    public double getFinalV(@NotNull Entity entity, double percentage) {
        double res = 0;
        List<ItemStack> equip = getEffectiveEquipment(entity);
        if (!equip.isEmpty()){
            ItemStack it;
            ItemPDC itemPDC;
            for (int i = 0; i < 6; i++){
                it = equip.get(i);
                if (it.getType() == Material.AIR) continue;
            }
        }
        return res;
    }
}
