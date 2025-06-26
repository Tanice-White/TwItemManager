package io.github.tanice.twItemManager.manager.pdc.impl;

import io.github.tanice.twItemManager.TwItemManager;
import io.github.tanice.twItemManager.infrastructure.PDCAPI;
import io.github.tanice.twItemManager.manager.item.ItemManager;
import io.github.tanice.twItemManager.manager.item.base.BaseItem;
import io.github.tanice.twItemManager.manager.item.base.impl.Gem;
import io.github.tanice.twItemManager.manager.item.base.impl.Item;
import io.github.tanice.twItemManager.manager.item.level.LevelTemplate;
import io.github.tanice.twItemManager.manager.pdc.CalculablePDC;
import io.github.tanice.twItemManager.manager.pdc.type.AttributeCalculateSection;
import io.github.tanice.twItemManager.manager.pdc.type.AttributeType;
import io.github.tanice.twItemManager.manager.pdc.type.DamageType;
import io.github.tanice.twItemManager.util.MiniMessageUtil;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;
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
import static io.github.tanice.twItemManager.util.SlotUtil.slotJudge;
import static io.github.tanice.twItemManager.util.Logger.logWarning;
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

    /* ITEM类兼容原版属性字符串 */
    /* 0-白值增加 1-百分比增加 */
    protected Map<String, double[]> oriAttrs;

    /**
     * 序列化使用，主动调用会引发bug
     */
    public ItemPDC() {
        super();
        qualityName = "";
        gems = new String[0];
        level = 0;
        oriAttrs = new HashMap<>();
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
     * 随机移除宝石
     */
    public @NotNull String removeRandomGem() {
        if (gems.length == 0) return "";
        String s;
        for (int i = 0; i < gems.length; i++) {
            if (!gems[i].equals(EMPTY_GEM)) {
                s = gems[i];
                gems[i] = EMPTY_GEM;
                return s;
            }
        }
        return "";
    }

    /**
     * 获取所有镶嵌上去的宝石，为空则跳过
     * @return 宝石组
     */
    public @NotNull List<String> getFilledGemNames() {
        if (gems.length == 0) return List.of();
        List<String> res = new ArrayList<>();
        for (String gem : gems) {
            if (!gem.equals(EMPTY_GEM)) res.add(gem);
        }
        return res;
    }

    public boolean emptyGems() {
        if (gems.length == 0) return true;
        Arrays.fill(gems, EMPTY_GEM);
        return true;
    }

    /**
     * 判断宝石槽位是否有空否为空
     * @return 是否有空
     */
    public boolean hasEmptyGemSlot() {
        for (String gem : gems) if (gem.equals(EMPTY_GEM)) return true;
        return false;
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
    public @NotNull String toString() {
        return "ItemPDC{" +
                "priority=" + priority + ", " +
                "itemInnerName=" + innerName + ", " +
                "qualityName=" + qualityName + ", " +
                "level=" + level + ", " +
                "gem=" + Arrays.toString(gems) + ", " +
                "oriAttrs=" + mapToString(oriAttrs) + ", " +
                "attributeCalculateSection=" + attributeCalculateSection + ", " +
                "attribute-addition=" + enumMapToString(vMap) + ", " +
                "type-addition=" + enumMapToString(tMap) +
                "}";
    }

    /**
     * 将影响自身属性的额外属性计算进本体属性中
     */
    public void selfCalculate() {
        ItemManager im = TwItemManager.getItemManager();

        BaseItem bit;
        CalculablePDC cPDC;

        /* 品质 */
        /* 品质若为BASE，则是加算，否则乘算 */
        AttributePDC aPDC = im.getQualityPDC(qualityName);
        if (aPDC != null) selfMerge(aPDC, 1);
        /* 宝石 */
        for (String gn : gems) {
            if (gn.equals(EMPTY_GEM)) continue;
            bit = im.getBaseItem(gn);
            if (!(bit instanceof Gem gem)) continue;
            cPDC = PDCAPI.getCalculablePDC(gem.getItem());
            if (cPDC == null) continue;
            selfMerge(cPDC.toAttributePDC(), 1);
        }

        /* 等级 */
        bit = im.getBaseItem(innerName);
        if (!(bit instanceof Item it)) {
            logWarning("[selfCalculate]: 物品 " + innerName + " 不存在");
            return;
        }
        /* 合法检测 */
        LevelTemplate lt = im.getLevelTemplate(it.getLevelTemplateName());
        int l;
        if (lt != null) {
            l = Math.max(level, lt.getBegin());
            l = Math.min(level, lt.getMax());
            if (l != level) logWarning("警告: 某玩家拥有非法等级武器，属性已自动修正(等级属性不变)");
            selfMerge(im.getLevelPDC(lt.getInnerName()), l);
        }
    }

    /**
     * 将quality计算进入属性
     * 这会改变当前的相关值，计算完成后请勿写回
     */
    @Deprecated
    public void selfQualityCalculate() {
        ItemManager im = TwItemManager.getItemManager();
        /* 品质若为BASE，则是加算，否则乘算 */
        AttributePDC aPDC = im.getQualityPDC(qualityName);
        if (aPDC != null) selfMerge(aPDC, 1);
    }

    /**
     * 将宝石计算进入属性
     * 这会改变当前的相关值，计算完成后请勿写回
     */
    @Deprecated
    public void selfGemCalculate() {
        ItemManager im = TwItemManager.getItemManager();
        BaseItem bit;
        CalculablePDC cPDC;
        /* 宝石 */
        for (String gn : gems) {
            if (gn.equals(EMPTY_GEM)) continue;
            bit = im.getBaseItem(gn);
            if (!(bit instanceof Gem gem)) continue;
            cPDC = PDCAPI.getCalculablePDC(gem.getItem());
            if (cPDC == null) continue;
            selfMerge(cPDC.toAttributePDC(), 1);
        }
    }
    /**
     * 将等级计算进入属性
     * 这会改变当前的相关值，计算完成后请勿写回
     */
    @Deprecated
    public void selfLevelCalculate() {
        ItemManager im = TwItemManager.getItemManager();
        BaseItem bit;
        /* 等级 */
        bit = im.getBaseItem(innerName);
        if (!(bit instanceof Item it)) {
            logWarning("[selfCalculate]: 物品 " + innerName + " 不存在");
            return;
        }
        /* 合法检测 */
        LevelTemplate lt = im.getLevelTemplate(it.getLevelTemplateName());
        int l;
        if (lt != null) {
            l = Math.max(level, lt.getBegin());
            l = Math.min(level, lt.getMax());
            if (l != level) logWarning("警告: 某玩家拥有非法等级武器，属性已自动修正(等级属性不变)");
            selfMerge(im.getLevelPDC(lt.getInnerName()), l);
        }
    }

    /**
     * 继承目标的 ”个性化属性“
     * @param iPDC 被继承的目标
     * @return 返回多余的宝石名
     */
    public @NotNull List<String> inheritanceFrom(@Nullable ItemPDC iPDC) {
        if (iPDC == null) return List.of();

        qualityName = iPDC.getQualityName();
        level = iPDC.getLevel();
        /* 被继承的宝石 */
        List<String> eg = iPDC.getFilledGemNames();
        int copyCount = Math.min(eg.size(), gems.length);
        for (int i = 0; i < copyCount; i++) {
            gems[i] = eg.get(i);
        }

        return eg.subList(copyCount, eg.size());
    }

    /**
     * 将原版属性挂到物品上
     * 若没有原版属性，则强制增加一个确保实际伤害都为1
     */
    public void attachOriAttrsTo(@NotNull ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        EquipmentSlotGroup es = slotJudge(getSlot(item));
        for (String k : oriAttrs.keySet()) {
            if (oriAttrs.get(k)[0] != 0) setAttr(getOriAttrNamespaceKey(), meta, k, "+", oriAttrs.get(k)[0], es);
            if (oriAttrs.get(k)[1] != 0) setAttr(getOriAttrNamespaceKey(), meta, k, "*", oriAttrs.get(k)[1], es);
        }
        item.setItemMeta(meta);
    }

    /**
     * 将对应的原版属性移除
     */
    public void removeOriAttrsFrom(@NotNull ItemStack item) {
        ItemMeta meta = item.getItemMeta();
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

    /**
     * 品质，等级，宝石既可以加算也可以乘算，但是总体是依附于物品本身的，即对外是一个计算完成的值
     * 则判定他们的计算方式，若是base 则直接加算；否则乘算
     * 所有的乘算都是单独计算，不加算
     */
    private void selfMerge(AttributePDC aPDC, double k){
        if (aPDC == null) return;
        for (AttributeType type : AttributeType.values()) {
            if (aPDC.getAttributeCalculateSection() == AttributeCalculateSection.BASE) vMap.put(type, vMap.getOrDefault(type, 0D) + aPDC.getVMap().getOrDefault(type, 0D) * k);
            else vMap.put(type, vMap.getOrDefault(type, 0D) * (1 + aPDC.getVMap().getOrDefault(type, 0D) * k));
        }
        for (DamageType type : DamageType.values()) {
            tMap.put(type, tMap.getOrDefault(type, 0D) + aPDC.getTMap().getOrDefault(type, 0D) * k);
        }
    }
}
