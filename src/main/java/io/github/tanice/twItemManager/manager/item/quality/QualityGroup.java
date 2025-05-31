package io.github.tanice.twItemManager.manager.item.quality;

import io.github.tanice.twItemManager.manager.pdc.impl.AttributePDC;
import io.github.tanice.twItemManager.manager.pdc.type.AttributeAdditionFromType;
import io.github.tanice.twItemManager.util.AlgorithmUtil;
import io.github.tanice.twItemManager.util.MiniMessageUtil;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static io.github.tanice.twItemManager.util.Logger.logWarning;

/**
 * setters and NoArgsConstructor are for inner use
 */
@Getter
@Setter
@NoArgsConstructor
public class QualityGroup {
    private String groupName;
    // 使用前缀和进行随机
    private List<Double> prefixWeights;
    private List<String> qualityNames;
    private List<AttributePDC> qualities;
    private int len;
    private double totalW;

    public QualityGroup(@NotNull String groupName, @Nullable ConfigurationSection config) {
        this.prefixWeights = new ArrayList<>();
        this.qualities = new ArrayList<>();
        this.qualityNames = new ArrayList<>();
        if (config == null) {
            logWarning(groupName + " Empty configuration section");
            return;
        }
        this.groupName = groupName;

        this.loadAll(config);
    }

    /**
     * 多个组构建-使用后只能调用 randomChoice 方法
     */
    public static AttributePDC multiplyRandomChoice (List<QualityGroup> qualityGroups) {
        List<Double> prefixWeights = new ArrayList<>();
        List<AttributePDC> qualities = new ArrayList<>();
        double totalW = 0;
        for (QualityGroup qm : qualityGroups) {
            prefixWeights.addAll(new ArrayList<>(qm.getPrefixWeights()));
            qualities.addAll(new ArrayList<>(qm.getQualities()));
            totalW += qm.getTotalW();
        }
        QualityGroup res = new QualityGroup();
        res.setPrefixWeights(prefixWeights);
        res.setQualities(qualities);
        res.setTotalW(totalW);
        return res.randomChoice();
    }

    /**
     * 随机概率 二分查找 O(log n)
     */
    public AttributePDC randomChoice() {
        double target = Math.random() * totalW;
        int index = AlgorithmUtil.findMaxLessThanOrEqualIndex(prefixWeights, target);
        return qualities.get(index);
    }

    private void loadAll(@NotNull ConfigurationSection config) {
        len = 0;
        double tt = 0;
        ConfigurationSection sc;
        String inner;
        for (String key : config.getKeys(false)) {
            sc = config.getConfigurationSection(key);
            if (!isValid(sc)) continue;
            inner = MiniMessageUtil.stripAllTags(key);
            tt += sc.getDouble("weight");
            this.prefixWeights.add(tt);
            this.qualities.add(new AttributePDC(key , AttributeAdditionFromType.ITEM, sc));
            this.qualityNames.add(inner);
            len++;
        }
        this.totalW = tt;
    }

    private boolean isValid (@Nullable ConfigurationSection cfg) {
        if (cfg == null) {
            logWarning("quality [" + groupName + "] has empty Configuration section");
            return false;
        }
        if (!cfg.contains("weight")) {
            logWarning("quality [" + groupName + "] has empty weight");
            return false;
        }
        if (cfg.getDouble("weight") <= 0) {
            logWarning("quality [" + groupName + "] has invalid weight");
            return false;
        }
        return true;
    }
}
