package io.github.tanice.twItemManager.manager.calculator;

import io.github.tanice.twItemManager.manager.item.ItemManager;
import io.github.tanice.twItemManager.manager.pdc.impl.ItemPDC;
import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@Getter
public class CombatPowerCalculator extends Calculator {
    /**
     * 世界配置文件设置的自定义难度系数
     */
    private double difficulty;

    public CombatPowerCalculator(@NotNull ItemManager im, @NotNull LivingEntity living) {
        super(im, living);
    }

    /**
     * 计算出攻击方最终的伤害
     * @return 伤害值
     */
    @Override
    public double getFinalV(@NotNull LivingEntity entity) {
        double res = 0;
        List<ItemPDC> equipPDCs = getEffectiveEquipmentPDC(entity);
        if (!equipPDCs.isEmpty()){

        }
        return res;
    }
}
