package io.github.tanice.twItemManager.manager.item.lore;

import io.github.tanice.twItemManager.TwItemManager;
import io.github.tanice.twItemManager.config.Config;
import io.github.tanice.twItemManager.constance.key.AttributeKey;
import io.github.tanice.twItemManager.infrastructure.PDCAPI;
import io.github.tanice.twItemManager.manager.item.ItemManager;
import io.github.tanice.twItemManager.manager.item.base.BaseItem;
import io.github.tanice.twItemManager.manager.item.base.impl.Consumable;
import io.github.tanice.twItemManager.manager.item.base.impl.Item;
import io.github.tanice.twItemManager.pdc.CalculablePDC;
import io.github.tanice.twItemManager.pdc.impl.AttributePDC;
import io.github.tanice.twItemManager.pdc.impl.ItemPDC;
import io.github.tanice.twItemManager.pdc.type.AttributeCalculateSection;
import io.github.tanice.twItemManager.util.MiniMessageUtil;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.github.tanice.twItemManager.pdc.impl.ItemPDC.EMPTY_GEM;
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

    private final String OWNER_KEY = "owner";
    private final String DURABILITY_KEY = "durability";

    private final String INVALID_GEM_LORE = "<gray>Invalid gem</gray>";
    private final String EMPTY_GEM_LORE = "<gray>空置宝石槽</gray>";

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{(\\??)([^}]+)}");

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

        CalculablePDC cPDC = PDCAPI.getCalculablePDC(itemStack);

        /* Consumable */
        if (baseItem instanceof Consumable consumable) meta.lore(generateLoreComponents(consumable));
        /* Gem Item Material(无PDC) */
        else meta.lore(generateLoreComponents(itemStack, baseItem, cPDC));

        if (cPDC instanceof ItemPDC iPDC) {
            String name = baseItem.getDisplayName();
            AttributePDC a = TwItemManager.getItemManager().getQualityPDC(iPDC.getQualityName());
            if (a != null) name = a.getDisplayName() + "  " + name;
            int level = iPDC.getLevel();
            if (level > 0) name += "  <yellow>Lv.<gray>" + level;
            meta.displayName(MiniMessageUtil.serialize(name));
        }

        itemStack.setItemMeta(meta);
    }

    /**
     * 为 BaseItem 生成 lore
     * Material 类没有 PDC
     */
    private @NotNull List<Component> generateLoreComponents(@NotNull ItemStack itemStack, @NotNull BaseItem baseItem, @Nullable CalculablePDC cPDC) {
        // Material 类
        if (cPDC == null) return baseItem.getItemLore().stream().map(MiniMessageUtil::serialize).toList();

        ItemManager itemManager = TwItemManager.getItemManager();
        if (cPDC instanceof ItemPDC) ((ItemPDC) cPDC).selfCalculate();
        Map<String, Double> attrLore =  cPDC.getAttrMap();
        List<Component> res = new ArrayList<>(templates.size());

        for (String template : templates) {
            switch (template) {
                case ORI_KEY -> res.addAll(baseItem.getItemLore().stream().map(MiniMessageUtil::serialize).toList());
                case OWNER_KEY -> {
                    if (!(baseItem instanceof Item item)) continue;
                    if (!item.isSoulBind()) continue;

                    String uuid = PDCAPI.getOwner(itemStack);
                    if (uuid == null || uuid.isEmpty()) {
                        res.add(MiniMessageUtil.serialize("所有者: <gray>无</gray>"));
                        continue;
                    }
                    OfflinePlayer owner = Bukkit.getOfflinePlayer(UUID.fromString(uuid));
                    res.add(MiniMessageUtil.serialize("所有者: " + owner.getName()));
                }
                case DURABILITY_KEY -> {
                    if (!(cPDC instanceof ItemPDC itemPDC)) continue;
                    Integer md = PDCAPI.getMaxDamage(itemStack);
                    Integer cd = PDCAPI.getCurrentDamage(itemStack);

                    if (md == null || cd == null) continue;

                    if (md == -1) {
                        res.add(MiniMessageUtil.serialize("耐久: <blue>无法破坏</blue>"));
                        continue;
                    }
                    res.add(MiniMessageUtil.serialize("耐久: " + cd + "/" + md));
                }
                case SKILL_KEY -> {
                    if (!(baseItem instanceof Item item)) continue;
                    res.add(MiniMessageUtil.serialize("这是技能测试，用于占位\")"));
                }
                case QUALITY_KEY -> {
                    if (!(cPDC instanceof ItemPDC itemPDC)) continue;
                    AttributePDC aPDC = itemManager.getQualityPDC(itemPDC.getQualityName());
                    if (aPDC == null) continue;

                    LoreTemplate qlt = TwItemManager.getItemManager().getLoreTemplate(aPDC.getLoreTemplateName());
                    if (qlt == null) continue;

                    Map<String, Double> qAttrs = aPDC.getAttrMap();
                    boolean f = aPDC.getAttributeCalculateSection() != AttributeCalculateSection.BASE;
                    String v;

                    for (String l : qlt.templates) {
                        v = matchAndReplace(l, qAttrs, f);
                        if (!v.isEmpty()) res.add(MiniMessageUtil.serialize(v));
                    }
                }
                case GEM_KEY -> {
                    if (!(cPDC instanceof ItemPDC itemPDC)) continue;
                    String[] gems = itemPDC.getGems();
                    BaseItem bit;
                    for (String gem : gems) {
                        if (gem.equals(EMPTY_GEM)) {
                            res.add(MiniMessageUtil.serialize(EMPTY_GEM_LORE));
                            continue;
                        }
                        bit = itemManager.getBaseItem(gem);
                        res.add(MiniMessageUtil.serialize(bit == null ? INVALID_GEM_LORE : bit.getDisplayName()));
                    }
                }
                default -> {
                    String l = matchAndReplace(template, attrLore, cPDC.getAttributeCalculateSection() != AttributeCalculateSection.BASE);
                    if (!l.isEmpty()) res.add(MiniMessageUtil.serialize(l));
                }
            }
        }
        return res;
    }

    /**
     * 为 Consumable 生成 lore
     * 原版 药水效果 和 自定义buff不计入
     */
    private @NotNull List<Component> generateLoreComponents(@NotNull Consumable consumable) {
        Map<String, Double> attrLore =  consumable.getContentLore();
        List<Component> res = new ArrayList<>();
        for (String template : templates) {
            if (template.equals(ORI_KEY)) {
                res.addAll(consumable.getItemLore().stream().map(MiniMessageUtil::serialize).toList());
            } else res.add(MiniMessageUtil.serialize(matchAndReplace(template, attrLore, false)));
        }
        return res;
    }

    /**
     * 匹配 {} 内的关键词，没有则不改变直接输出
     */
    private @NotNull String matchAndReplace(@NotNull String loreTemplateString, @NotNull Map<String, Double> attrMap, boolean percentageDisplay) {
        StringBuilder sb = new StringBuilder();
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(loreTemplateString);
        String k;
        Double v;
        boolean isConditional;
        boolean found = false;
        // 替换所有符合条件的部分
        while (matcher.find()) {
            found = true;
            isConditional = "?".equals(matcher.group(1));
            k = matcher.group(2).toLowerCase();
            v = attrMap.get(k);

            if (v == null) v = 0.0;
            if (isConditional && v == 0.0) continue;

            String formatted = percentageDisplay || displayAsPercentage(k) ? String.format("%.1f%%", v * 100) : String.format("%.1f", v);
            if (v > 0) formatted = "+" + formatted;
            matcher.appendReplacement(sb, Matcher.quoteReplacement(formatted));
            /* 这样写 如果lore带问号但是值为0则不会显示 */
            matcher.appendTail(sb);
        }

        if (!found) sb.append(loreTemplateString);
        return sb.toString();
    }

    /**
     * 必须显示为百分数的属性值
     */
    private boolean displayAsPercentage(@NotNull String attrName) {
        return attrName.equalsIgnoreCase(AttributeKey.CRITICAL_STRIKE_CHANCE) ||
                attrName.equalsIgnoreCase(AttributeKey.CRITICAL_STRIKE_DAMAGE) ||
                attrName.equalsIgnoreCase(AttributeKey.MANA_COST) ||
                attrName.equalsIgnoreCase(AttributeKey.SKILL_COOLDOWN);
    }
}
