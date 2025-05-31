package io.github.tanice.twItemManager.manager.pdc.impl;

import io.github.tanice.twItemManager.TwItemManager;
import io.github.tanice.twItemManager.manager.pdc.CalculablePDC;
import io.github.tanice.twItemManager.manager.pdc.type.AttributeAdditionFromType;
import io.github.tanice.twItemManager.manager.pdc.type.AttributeCalculateType;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serial;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static io.github.tanice.twItemManager.constance.key.AttributeKey.*;
import static io.github.tanice.twItemManager.constance.key.ConfigKey.*;
import static io.github.tanice.twItemManager.infrastructure.AttributeAPI.*;
import static io.github.tanice.twItemManager.infrastructure.PDCAPI.getSlot;
import static io.github.tanice.twItemManager.util.EquipmentSlotGroupUtil.slotJudge;

/**
 * 物品持有的属性
 */
@Getter
public class ItemPDC extends CalculablePDC {
    @Serial
    private static final long serialVersionUID = 1L;

    private static final String EMPTY_GEM = "$";
    private static final String ATTR_SECTION_KEY = "attrs";
    /* 影响属性的值 */
    @Setter
    private String qualityName;
    private final String[] gems;
    private int level;

    /* ITEM类兼容原版属性字符串 */
    /* 0-白值增加 1-百分比增加 */
    protected Map<String, double[]> oriAttrs;

    public ItemPDC() {
        super();
        qualityName = "";
        gems = new String[0];
        level = 0;
        oriAttrs = new HashMap<>();
    }

    public ItemPDC(@NotNull String innerName, @NotNull AttributeAdditionFromType fromType, int gemStack) {
        super(innerName, fromType);
        qualityName = "";
        if (gemStack > 0) gems = new String[gemStack];
        else gems = new String[0];
        level = 0;
        oriAttrs = new HashMap<>();
    }

    public ItemPDC(@NotNull String innerName, @NotNull AttributeAdditionFromType fromType, @NotNull ConfigurationSection cfg) {
        super(innerName, fromType, cfg.getConfigurationSection(ATTR_SECTION_KEY));
        qualityName = cfg.getString(QUALITY, "");
        int l = cfg.getInt(GEM_STACK_NUMBER, 0);
        if (l > 0) {
            gems = new String[l];
            Arrays.fill(gems, EMPTY_GEM);
        }
        else gems = new String[0];
        level = cfg.getInt(LEVEL, 0);
        oriAttrs = new HashMap<>();
        this.loadOriAttrs(cfg.getConfigurationSection(ATTR_SECTION_KEY));
    }

    @Override
    public void selfCalculate() {
        // TODO 将宝石和品质属性进行计算
        // addOriAttrs(quality的PDC和gems的PDC);
        return;
    }

    @Override
    public void merge(CalculablePDC... o) {
        // TODO 同类(AttributeAdditionFromType)物品数值合并
        return;
    }

    /**
     * 添加宝石
     * @param GemInnerName 宝石内部名
     * @return 是否成功
     */
    public boolean addGem(String GemInnerName) {
        if (gems.length == 0) return false;
        for (int i = 0; i < gems.length; i++) {
            if (gems[i].equals(EMPTY_GEM)) {
                gems[i] = GemInnerName;
                return true;
            }
        }
        return false;
    }

    /**
     * 移除宝石, 若原本没有这个宝石，使用移除会返回false
     * @param GemInnerName 宝石内部名
     * @return 是否成功
     */
    public boolean removeGem(String GemInnerName) {
        if (gems.length == 0) return false;
        for (int i = 0; i < gems.length; i++) {
            if (gems[i].equals(GemInnerName)) {
                gems[i] = EMPTY_GEM;
                return true;
            }
        }
        return false;
    }

    /**
     * 获取所有宝石
     * @return 宝石组
     */
    public @Nullable String[] getGems() {
        if (gems.length == 0) return null;
        return Arrays.copyOf(gems, gems.length);
    }

    public boolean emptyGems() {
        if (gems.length == 0) return true;
        Arrays.fill(gems, EMPTY_GEM);
        return true;
    }

    /**
     * 判断宝石是否为空
     * @param GemInnerName 宝石内部名
     * @return 是否为空
     */
    public boolean isEmptyGem(@NotNull String GemInnerName) {
        return GemInnerName.equals(EMPTY_GEM);
    }

    /**
     * 不检测合法性，在属性计算时统一计算
     */
    public void levelUp() {
        level ++;
    }
    /**
     * 不检测合法性，在属性计算时统一计算
     */
    public void levelDown() {
        if (level > 0) level --;
    }

    public void setLevel(int level) {
        if (level >= 0) this.level = level;
    }

    /**
     * 将原版属性挂到物品上
     * 宝石属性会在被生成的时候绑定到实物上
     */
    public void attachOriAttrsTo(@NotNull ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        EquipmentSlotGroup es = slotJudge(getSlot(item));
        for (String k : oriAttrs.keySet()) {
            if (oriAttrs.get(k)[0] != 0) setAttr(getOriAttrNamespaceKey(), meta, k, AttributeCalculateType.ADD, oriAttrs.get(k)[0], es);
            if (oriAttrs.get(k)[1] != 0)  setAttr(getOriAttrNamespaceKey(), meta, k, AttributeCalculateType.MULTIPLY, oriAttrs.get(k)[0], es);
        }
        item.setItemMeta(meta);
    }

    /**
     * 将对应的原版属性移除(不提供移除)
     */
    @Deprecated
    public void removeOriAttrsFrom(@NotNull ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        // 需要精确remove
        for (String k : oriAttrs.keySet()) removeAddAttrByKey(getOriAttrNamespaceKey(), meta, k);
    }

    @Override
    public String toString() {
        return "CalculablePDC{" +
                "fromType=" + fromType + ", " +
                "itemInnerName=" + innerName + ", " +
                "qualityName=" + qualityName + ", " +
                "level=" + level + ", " +
                "gem=" + Arrays.toString(gems) + ", " +
                "damage=" + Arrays.toString(damage) + ", " +
                "criticalStrikeChance=" + criticalStrikeChance + ", " +
                "criticalStrikeDamage=" + criticalStrikeDamage + ", " +
                "armor=" + armor + ", " +
                "armorToughness=" + armorToughness + ", " +
                "preArmorReduction=" + preArmorReduction + ", " +
                "afterArmorReduction=" + afterArmorReduction + ", " +
                "manaCost=" + Arrays.toString(manaCost) + ", " +
                "skillCoolDown=" + Arrays.toString(skillCoolDown) +
                "oriAttrs=" + mapToString(oriAttrs) +
                "}";
    }

    /**
     * 生成原版属性绑定到物品中的 namespace key
     */
    private @NotNull String getOriAttrNamespaceKey() {
        return TwItemManager.getInstance().getName().toLowerCase();
    }

    /**
     * 设置原版属性
     */
    private void loadOriAttrs(@Nullable ConfigurationSection cfg) {
        if (cfg == null) return;
        String v;
        for (String str : cfg.getKeys(false)) {
            if (!isOriginalKey(str)) continue;
            v = cfg.getString(str);
            if (v == null) continue;
            /* 有百分号表示是乘积计算 */
            if (v.endsWith("%")) addOriAttrs(str, new double[]{0D, getCfgValue(v)});
            else addOriAttrs(str, new double[]{getCfgValue(v), 0D});
        }
    }

    /**
     * 增加原版属性
     */
    private void addOriAttrs(@NotNull String key, double @NotNull [] values) {
        if (values.length != 2) return;
        oriAttrs.put(key, new double[]{values[0], values[1]});
    }

    private @NotNull String mapToString(@NotNull Map<String, double[]> map) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        for (Map.Entry<String, double[]> entry : map.entrySet()) {
            sb.append(entry.getKey());
            sb.append("=");
            sb.append(Arrays.toString(entry.getValue()));
            sb.append("; ");
        }
        sb.append("}");
        return sb.toString();
    }
}
