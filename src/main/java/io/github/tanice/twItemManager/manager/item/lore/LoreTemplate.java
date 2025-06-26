package io.github.tanice.twItemManager.manager.item.lore;

import io.github.tanice.twItemManager.TwItemManager;
import io.github.tanice.twItemManager.config.Config;
import io.github.tanice.twItemManager.infrastructure.PDCAPI;
import io.github.tanice.twItemManager.manager.item.base.BaseItem;
import io.github.tanice.twItemManager.manager.item.base.impl.Consumable;
import io.github.tanice.twItemManager.manager.item.base.impl.Gem;
import io.github.tanice.twItemManager.manager.item.base.impl.Item;
import io.github.tanice.twItemManager.manager.item.base.impl.Material;
import io.github.tanice.twItemManager.manager.pdc.CalculablePDC;
import io.github.tanice.twItemManager.manager.pdc.ConsumablePDC;
import io.github.tanice.twItemManager.manager.pdc.impl.AttributePDC;
import io.github.tanice.twItemManager.manager.pdc.impl.ItemPDC;
import io.github.tanice.twItemManager.manager.pdc.type.AttributeCalculateSection;
import io.github.tanice.twItemManager.util.MiniMessageUtil;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.github.tanice.twItemManager.util.Logger.logWarning;

/**
 * 服务于 BaseItem 的 loreTemplateName 字段
 */
@Getter
public class LoreTemplate {
    /* 技能描述 */
    private final String SKILL_KEY = "skill";
    /* 品质描述 */
    private final String QUALITY_KEY = "quality";
    /* 显示宝石的名称 */
    private final String GEM_KEY = "gem";
    /* item 中原本的lore */
    private final String ORI_KEY = "ori";

    private final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{(\\??)([^}]+)}");

    private final String templateName;
    private final List<String> templates;

    public LoreTemplate(String templateName, @NotNull List<String> template) {
        this.templateName = templateName;
        this.templates = template;
    }

    public void AttachLoreToItem(@NotNull ItemStack itemStack) {
        BaseItem baseItem = TwItemManager.getItemManager().getBaseItem(itemStack);
        ItemMeta meta = itemStack.getItemMeta();
        if (baseItem == null || meta == null) {
            if (Config.debug) logWarning("[generateAndAttachLoreToItem] 物品底层或 META 不存在");
            return;
        }

        /* Consumable */
        if (baseItem instanceof Consumable consumable) {
            ConsumablePDC csPDC = PDCAPI.getConsumablePDC(itemStack);
            if (csPDC == null) {
                if (Config.debug) logWarning("[generateAndAttachLoreToItem] 物品PDC不存在");
                return;
            }
            meta.lore(generateLoreComponents(consumable, csPDC));
        /* Gem Item Material(无PDC) */
        } else meta.lore(generateLoreComponents(baseItem, PDCAPI.getCalculablePDC(itemStack)));

        itemStack.setItemMeta(meta);
    }

    /**
     * 为 BaseItem 生成 lore
     * Material 类没有 PDC
     */
    private @NotNull List<Component> generateLoreComponents(@NotNull BaseItem baseItem, @Nullable CalculablePDC cPDC) {
        // Material 类
        if (cPDC == null) return baseItem.getItemLore().stream().map(MiniMessageUtil::serialize).toList();

        boolean numberDisplay = cPDC.getAttributeCalculateSection() == AttributeCalculateSection.BASE;
        Map<String, Double> attrLore =  cPDC.getAttrLore();
        List<Component> res = new ArrayList<>();

        for (String template : templates) {
            switch (template) {
                case ORI_KEY -> res.addAll(baseItem.getItemLore().stream().map(MiniMessageUtil::serialize).toList());
                case SKILL_KEY -> res.add(MiniMessageUtil.serialize("这是技能测试，用于占位 \")"));
                case QUALITY_KEY -> {
                    if (!(cPDC instanceof ItemPDC itemPDC)) break;
                    AttributePDC qPDC = TwItemManager.getItemManager().getQualityPDC(itemPDC.getQualityName());
                    if (qPDC == null) break;
                    // TODO qPDC.getDisplayName(); 更新
                    // TODO lore更新
                }
                case GEM_KEY -> {
                    if (!(cPDC instanceof ItemPDC itemPDC)) break;
                    String[] gems = itemPDC.getGems();
                    // TODO 更新槽位显示
                }
                default -> res.add(MiniMessageUtil.serialize(matchAndReplace(template, attrLore, numberDisplay)));
            }
        }
        return res;
    }

    /**
     * 为 Consumable 生成 lore
     */
    private @NotNull List<Component> generateLoreComponents(@NotNull Consumable consumable, @NotNull ConsumablePDC cPDC) {
        Map<String, List<String>> attrLore =  cPDC.getContentLore();
        List<Component> res = new ArrayList<>();
        for (String template : templates) {
            if (template.equals(ORI_KEY)) {
                res.addAll(consumable.getItemLore().stream().map(MiniMessageUtil::serialize).toList());
            } else res.add(MiniMessageUtil.serialize(matchAndReplace(template, attrLore)));
        }
        return res;
    }

    /**
     * 匹配 {} 内的关键词，没有则不改变直接输出
     */
    private @NotNull String matchAndReplace(@NotNull String loreTemplateString, @NotNull Map<String, Double> PDCAttrs, boolean numberDisplay) {
        StringBuilder sb = new StringBuilder();
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(loreTemplateString);
        String k;
        Double v;
        boolean isConditional;
        // 替换所有符合条件的部分
        while (matcher.find()) {
            isConditional = "?".equals(matcher.group(1));
            k = matcher.group(2).toLowerCase();
            v = PDCAttrs.get(k);
            if (v == null || v == 0.0) {
                v = 0.0;
                if (isConditional) continue;
            }
            String formatted = numberDisplay ? String.format("%.1f", v) : String.format("%.1f%%", v * 100);
            matcher.appendReplacement(sb, Matcher.quoteReplacement(formatted));
        }
        matcher.appendTail(sb);
        if (!matcher.find()) sb.append(loreTemplateString);
        return sb.toString();
    }

    private @NotNull String matchAndReplace(@NotNull String loreTemplateString, @NotNull Map<String, List<String>> PDCAttrs) {
        StringBuilder sb = new StringBuilder();
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(loreTemplateString);
        String k;
        List<String> vList;
        String v = "";
        boolean isConditional;
        // 替换所有符合条件的部分
        while (matcher.find()) {
            isConditional = "?".equals(matcher.group(1));
            k = matcher.group(2).toLowerCase();
            vList = PDCAttrs.get(k);
            if (vList == null) {
                v = "";
                if (isConditional) continue;
            } else {
                for (String vs : vList) v += vs;
                v = v.substring(1, v.length() - 1);
            }
            matcher.appendReplacement(sb, Matcher.quoteReplacement(v));
        }
        matcher.appendTail(sb);
        if (!matcher.find()) sb.append(loreTemplateString);
        return sb.toString();
    }
}
