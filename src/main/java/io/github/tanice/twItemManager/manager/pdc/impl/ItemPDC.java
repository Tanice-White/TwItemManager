package io.github.tanice.twItemManager.manager.pdc.impl;

import io.github.tanice.twItemManager.TwItemManager;
import io.github.tanice.twItemManager.manager.item.ItemManager;
import io.github.tanice.twItemManager.manager.pdc.CalculablePDC;
import io.github.tanice.twItemManager.manager.pdc.type.AttributeCalculateSection;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serial;
import java.util.*;

import static io.github.tanice.twItemManager.constance.key.AttributeKey.*;
import static io.github.tanice.twItemManager.constance.key.ConfigKey.*;
import static io.github.tanice.twItemManager.infrastructure.AttributeAPI.*;
import static io.github.tanice.twItemManager.infrastructure.PDCAPI.getSlot;
import static io.github.tanice.twItemManager.util.EquipmentSlotGroupUtil.slotJudge;
import static io.github.tanice.twItemManager.util.Tool.*;

/**
 * 物品持有的属性
 * 武器的属性和宝石作为武器的基础面板计算
 */
@Getter
public class ItemPDC extends CalculablePDC {
    @Serial
    private static final long serialVersionUID = 1L;

    private static final String EMPTY_GEM = "$";
    /* 影响属性的值 */
    @Setter
    private String qualityName;
    private final String[] gems;
    private int level;
    private final String levelTemplateName;

    /* ITEM类兼容原版属性字符串 */
    /* 0-白值增加 1-百分比增加 */
    protected Map<String, double[]> oriAttrs;

    public ItemPDC() {
        super();
        qualityName = "";
        gems = new String[0];
        level = 0;
        oriAttrs = new HashMap<>();
        levelTemplateName = "";
    }

    public ItemPDC(@NotNull String innerName, @NotNull AttributeCalculateSection acs, @NotNull ConfigurationSection cfg) {
        super(innerName, acs, cfg.getConfigurationSection(ATTR_SECTION_KEY));
        qualityName = cfg.getString(QUALITY, "");
        int l = cfg.getInt(GEM_STACK_NUMBER, 0);
        if (l > 0) {
            gems = new String[l];
            Arrays.fill(gems, EMPTY_GEM);
        }
        else gems = new String[0];
        level = cfg.getInt(LEVEL, 0);
        levelTemplateName = cfg.getString(LEVEL_TEMPLATE_NAME, "");
        oriAttrs = new HashMap<>();
        this.loadOriAttrs(cfg.getConfigurationSection(ATTR_SECTION_KEY));
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

    @Override
    public String toString() {
        return "CalculablePDC{" +
                "priority=" + priority + ", " +
                "itemInnerName=" + innerName + ", " +
                "qualityName=" + qualityName + ", " +
                "level=" + level + ", " +
                "gem=" + Arrays.toString(gems) + ", " +
                "oriAttrs=" + mapToString(oriAttrs) + ", " +
                "attributeCalculateSection=" + attributeCalculateSection + ", " +
                "attribute-addition=" + enumMapToString(vMap) +
                "type-addition=" + enumMapToString(tMap) +
                "}";
    }

    /**
     * 将影响自身属性的额外属性计算进本体属性中
     */
    public void selfCalculate() {
        ItemManager im = TwItemManager.getItemManager();

        merge(im.getQualityPDC(qualityName));

        merge(im.getLevelPDC(levelTemplateName, level));

        AttributePDC aPDC;
        for (String gem : gems) {
            if (gem.equals(EMPTY_GEM)) continue;
            aPDC = im.getGemAttributePDC(gem);
            if (aPDC != null) merge(aPDC);
        }
    }

    /**
     * 将原版属性挂到物品上
     * 宝石属性会在被生成的时候绑定到实物上
     */
    public void attachOriAttrsTo(@NotNull ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        EquipmentSlotGroup es = slotJudge(getSlot(item));
        for (String k : oriAttrs.keySet()) {
            if (oriAttrs.get(k)[0] != 0) setAttr(getOriAttrNamespaceKey(), meta, k, "+", oriAttrs.get(k)[0], es);
            if (oriAttrs.get(k)[1] != 0)  setAttr(getOriAttrNamespaceKey(), meta, k, "*", oriAttrs.get(k)[1], es);
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

    private double getCfgValue(@NotNull String v) {
        double res = Double.parseDouble(v.replaceAll("%", ""));
        if (v.endsWith("%")) res /= 100;
        return res;
    }
}
