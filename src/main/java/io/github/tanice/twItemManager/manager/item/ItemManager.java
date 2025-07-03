package io.github.tanice.twItemManager.manager.item;

import io.github.tanice.twItemManager.event.TwItemUpdateEvent;
import io.github.tanice.twItemManager.infrastructure.PDCAPI;
import io.github.tanice.twItemManager.manager.item.base.BaseItem;
import io.github.tanice.twItemManager.manager.item.base.impl.Consumable;
import io.github.tanice.twItemManager.manager.item.base.impl.Gem;
import io.github.tanice.twItemManager.manager.item.base.impl.Item;
import io.github.tanice.twItemManager.manager.item.base.impl.Material;
import io.github.tanice.twItemManager.manager.item.level.LevelTemplate;
import io.github.tanice.twItemManager.manager.item.lore.LoreTemplate;
import io.github.tanice.twItemManager.manager.item.quality.QualityGroup;
import io.github.tanice.twItemManager.manager.pdc.CalculablePDC;
import io.github.tanice.twItemManager.manager.pdc.impl.AttributePDC;
import io.github.tanice.twItemManager.manager.pdc.EntityBuffPDC;
import io.github.tanice.twItemManager.manager.pdc.impl.ItemPDC;
import io.github.tanice.twItemManager.util.MiniMessageUtil;
import org.bukkit.Bukkit;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.*;

import static io.github.tanice.twItemManager.config.Config.loadCustomConfigs;
import static io.github.tanice.twItemManager.constance.key.ConfigKey.*;
import static io.github.tanice.twItemManager.infrastructure.PDCAPI.*;
import static io.github.tanice.twItemManager.manager.item.quality.QualityGroup.multiplyRandomChoice;
import static io.github.tanice.twItemManager.util.Logger.logWarning;

/**
 * 将 base lore quality skill 集合成最终的TwItem实例
 */
public class ItemManager {
    private final JavaPlugin plugin;
    /** 主目录文件 */
    private final File rootDir;

    /** key 物品内部名  value 基础物品类(包含物品的基础信息) 通过调用generate函数获取物品实例 */
    private final Map<String, BaseItem> itemMap;

    /** key 组名  value 品质组Map(key 品质名 value 品质类) */
    private final Map<String, QualityGroup> qualityGroupMap;
    /** 全局可用品质(便于检索和切换) */
    private final Map<String, AttributePDC> qualityMap;

    /** 物品描述模板 */
    private final Map<String, LoreTemplate> loreTemplateMap;

    /** key 技能名 value 技能类 */
    // private final Map<String, SkillManager> skillManagerMap;

    /** key 升级模板名 value 升级模板类 */
    private final Map<String, LevelTemplate> levelTemplateMap;

    public ItemManager(@NotNull JavaPlugin plugin) {
        this.plugin = plugin;
        rootDir = plugin.getDataFolder();
        itemMap = new LinkedHashMap<>();
        qualityGroupMap = new HashMap<>();
        qualityMap = new HashMap<>();
        // skillManagerMap = new HashMap<>();
        levelTemplateMap = new HashMap<>();
        loreTemplateMap = new HashMap<>();
        this.loadConfigs();
        /* 初始化全局qualityMap */
        this.loadQualityMap();
    }

    public void onReload() {
        itemMap.clear();
        qualityGroupMap.clear();
        qualityMap.clear();
        loreTemplateMap.clear();
        // skillManagerMap.clear();
        levelTemplateMap.clear();
        this.loadConfigs();
        /* 初始化全局qualityMap */
        this.loadQualityMap();
    }

    /**
     * 根据物品 item 获取内部的持久类
     */
    public @Nullable BaseItem getBaseItem(@Nullable ItemStack item){
        if (item == null) return null;
        return itemMap.get(getInnerName(item));
    }

    /**
     * 根据物品 innerName 获取内部的持久类
     */
    public @Nullable BaseItem getBaseItem(@NotNull String innerName){
        return itemMap.get(innerName);
    }

    public Set<String> getItemNameList() {
        return itemMap.keySet();
    }

    /**
     * 获取全局品质列表的具体属性
     */
    public @Nullable AttributePDC getQualityPDC(@NotNull String qualityName) {
        return qualityMap.get(qualityName);
    }

    /**
     * 获取等级模板类
     * 便于判定相关属性是否合法
     */
    public @Nullable LevelTemplate getLevelTemplate(@NotNull String levelTemplateName) {
        return levelTemplateMap.get(levelTemplateName);
    }

    /**
     * 获取等级模板对应的模板属性
     */
    public @Nullable AttributePDC getLevelPDC(@NotNull String levelTemplateName) {
        LevelTemplate lt = levelTemplateMap.get(levelTemplateName);
        if (lt == null) return null;
        return lt.getAttributePDC();
    }

    public boolean isNotItem(@Nullable ItemStack item) {
        String in = getInnerName(item);
        if (in == null) return true;
        return isNotItem(in);
    }

    public boolean isNotItem(@NotNull String innerName) {
        return !itemMap.containsKey(innerName);
    }
    
    public String getCalculablePDCAsString(@NotNull ItemStack item) {
        CalculablePDC cPDC = PDCAPI.getCalculablePDC(item);
        if (cPDC == null) return "此物品没有持久化的PDC";
        return cPDC.toString();
    }

    public String getCalculablePDCAsString(@NotNull LivingEntity entity) {
        EntityBuffPDC cPDC = PDCAPI.getCalculablePDC(entity);
        if (cPDC == null) return "此物品没有持久化的PDC";
        return cPDC.toString();
    }

    public @NotNull ItemStack generateItem(@NotNull String innerName){
        /* 获取基础物品的可更改备份 */
        BaseItem bit = itemMap.get(innerName);
        if (bit == null){
            logWarning("物品: " + innerName + "不存在");
            return new ItemStack(org.bukkit.Material.AIR);
        }
        ItemStack item = bit.getItem();
        if (item.getType().isAir()) return item;

        if (bit instanceof Item it ) {
            /* 随机品质 */
            if (!getQualityName(item).isEmpty()) {
                AttributePDC aPDC = randomQuality(it.getQualityGroups());
                if (aPDC != null) setQualityName(item, aPDC.getInnerName());
            }
            /* 等级检测 */
            int l = getLevel(item);
            LevelTemplate lt = getLevelTemplate(it.getLevelTemplateName());
            if (lt != null) {
                if (l < lt.getBegin()) l = lt.getBegin();
                if (l > lt.getMax()) l = lt.getMax();
            }
            PDCAPI.setLevel(item, l);
            // TODO step3 绑定技能
        }
        updateItemDisplayView(item);

        return item;
    }

    /**
     * 物品前缀重铸
     * 返回 null 表示重铸失败
     * 否则返回多余的宝石（没有则返回空）
     */
    public @Nullable String recast(@NotNull Player player, @NotNull ItemStack item) {
        BaseItem bit = itemMap.get(getInnerName(item));
        if (!(bit instanceof Item it)) {
            player.sendMessage("§e此物品不能重铸");
            return null;
        }

        if (item.getMaxStackSize() > 1) {
            player.sendMessage("§e可堆叠物品不应该有品质!");
            return null;
        }

        List<String> qgs = it.getQualityGroups();
        if (qgs.isEmpty()) {
            player.sendMessage("§e此物品不能重铸");
            return null;
        }

        /* 获取随机品质 */
        AttributePDC aPDC = randomQuality(qgs);
        if (aPDC == null) {
            player.sendMessage("§e重铸失败, 请联系管理");
            logWarning("玩家: " + player.getName() + " 重铸武器 " + getInnerName(item) + " 时出现null品质, 请检查配置文件");
            return null;
        }
        if (!setQualityName(item, aPDC.getInnerName())) {
            player.sendMessage("§e重铸失败, 请联系管理");
            logWarning("玩家: " + player.getName() + " 重铸武器 " + getInnerName(item) + " 时品质设置失败, 请联系作者");
            return null;
        }
        updateItemDisplayView(item);
        return "";
    }

    public boolean levelUp(@NotNull Player player, @NotNull ItemStack item, @Nullable ItemStack levelUpNeed, boolean check) {
        BaseItem bit = itemMap.get(getInnerName(item));
        if (!(bit instanceof Item it)) {
            player.sendMessage("§e此物品没有等级");
            return false;
        }

        String lvlName = it.getLevelTemplateName();
        if (lvlName.isEmpty()) {
            player.sendMessage("§e此物品没有等级");
            return false;
        }
        LevelTemplate lt = levelTemplateMap.get(lvlName);
        if (lt == null) {
            player.sendMessage("§e此物品没有等级");
            logWarning("玩家: " + player.getName() + " 升级武器 " + getInnerName(item) + " 时, 对应模板 " + lvlName + " 没有内容, 请检查配置文件");
            return false;
        }

        int lvl = getLevel(item);
        if (lvl < lt.getBegin() || lvl > lt.getMax()) {
            player.sendMessage("§e物品已达到最大等级");
            logWarning("非法等级: " + lvl + " in: " + getInnerName(item) + ". Player: " + player.getName());
            return false;
        }
        if (lvl == lt.getMax()) {
            player.sendMessage("§e物品已经达到最大等级");
            return false;
        }
        // 判断升级物品是否正确
        if (check && (isNotItem(levelUpNeed) || !lt.getLevelUpNeed().equals(getInnerName(levelUpNeed)))) return false;

        if (Math.random() < lt.getChance()){
            PDCAPI.levelUp(item);
            player.sendMessage("§a强化成功!");
            return true;
        }
        String s = "§c强化失败";
        if (lt.isLevelDownWhenFailed() && getLevel(item) > lt.getBegin()) {
            PDCAPI.levelDown(item);
            s += " 武器降级";
        }
        player.sendMessage(s);
        updateItemDisplayView(item);
        return true;
    }

    public void levelDown(@NotNull Player player, @NotNull ItemStack item) {
        BaseItem bit = itemMap.get(getInnerName(item));
        if (!(bit instanceof Item it)) {
            player.sendMessage("§e此物品没有等级");
            return;
        }

        String lvlName = it.getLevelTemplateName();
        if (lvlName.isEmpty()) {
            player.sendMessage("§e物品无法降级");
            return;
        }

        LevelTemplate lt = levelTemplateMap.get(lvlName);
        if (lt == null) {
            player.sendMessage("§e物品无法降级");
            logWarning("无效的升级模板名称: " + lvlName + " in: " + getInnerName(item));
            return;
        }
        int lvl = getLevel(item);
        if (lvl < lt.getBegin() || lvl > lt.getMax()) {
            logWarning("非法等级: " + lvl + " in: " + getInnerName(item) + ". Player: " + player.getName());
            return;
        }
        if (lvl == lt.getBegin()) {
            player.sendMessage("§e物品已经达到最低等级");
            return;
        }
        PDCAPI.levelDown(item);
        player.sendMessage("§a降级成功!");
        updateItemDisplayView(item);
    }

    
    public void levelSet(@NotNull Player player, @NotNull ItemStack item, int level) {
        BaseItem bit = itemMap.get(getInnerName(item));
        if (!(bit instanceof Item it)) {
            player.sendMessage("§e此物品没有等级");
            return;
        }
        String lvlName = it.getLevelTemplateName();
        if (lvlName.isEmpty()) {
            player.sendMessage("§e此物品没有等级");
            return;
        }
        LevelTemplate lt = levelTemplateMap.get(lvlName);
        if (lt == null) {
            player.sendMessage("§e物品等级模板不存在");
            return;
        }
        if (level < lt.getBegin() || level > lt.getMax()) {
            player.sendMessage("§e非法等级");
            logWarning("§e非法等级: " + level + " in: " + getInnerName(item) + ". Player: " + player.getName());
            return;
        }
        PDCAPI.setLevel(item, level);
        player.sendMessage("§a等级设置成功");
        updateItemDisplayView(item);
    }

    public boolean insertGem(@NotNull Player player, @NotNull ItemStack item, @NotNull ItemStack gem) {
        CalculablePDC cPDC1 = getCalculablePDC(item);
        BaseItem bitGem = itemMap.get(PDCAPI.getInnerName(gem));
        /* 不满足条件不插入宝石 */
        if (!(cPDC1 instanceof ItemPDC iPDC) || !(bitGem instanceof Gem g)) return false;

        boolean f = iPDC.hasEmptyGemSlot();
        if (!f) {
            player.sendMessage("§e槽位已满");
            return false;
        }

        if (Math.random() < g.getChance()){
            f = iPDC.addGem(getInnerName(gem));
            if (f) {
                player.sendMessage("§a宝石镶嵌成功!");
                setCalculablePDC(item, iPDC);
                return true;
            } else {
                player.sendMessage("§e宝石有问题，请询问管理员");
                logWarning("玩家: " + player.getName() + "的宝石: " + getInnerName(gem) + "有问题，应该镶嵌成功但是失败了，宝石未消失");
                return false;
            }
        }
        String s = "§c镶嵌失败";
        if (g.isLossWhenFailed()) {
            s += " 宝石消失";
            player.sendMessage(s);
            return true;
        }
        player.sendMessage(s);
        updateItemDisplayView(item);
        return false;
    }

    
    public ItemStack removeGem(@NotNull Player player, @NotNull ItemStack item) {
        ItemStack gem = new ItemStack(org.bukkit.Material.AIR);

        CalculablePDC cPDC = getCalculablePDC(item);
        if (!(cPDC instanceof ItemPDC iPDC)) {
            player.sendMessage("§e此物品没有宝石");
            return gem;
        }

        String gn = iPDC.removeRandomGem();
        if (gn.isEmpty()) {
            player.sendMessage("§e此物品没有宝石");
            return gem;
        }
        player.sendMessage("§a宝石拆卸成功!");
        setCalculablePDC(item, iPDC);

        gem = generateItem(gn);
        if (gem.getType().isAir()) {
            player.sendMessage("§e此宝石已经不存在，无法获取");
            logWarning("玩家: " + player.getName() + "所拆卸下的宝石: " + gn +"不存在");
        }
        updateItemDisplayView(item);
        return gem;
    }

    public List<String> updateItem(@NotNull Player player, @NotNull ItemStack item){
        BaseItem bit = itemMap.get(getInnerName(item));
        if (bit == null) return List.of();

        TwItemUpdateEvent updateEvent = new TwItemUpdateEvent (plugin, player, item);
        Bukkit.getPluginManager().callEvent(updateEvent);

        if (updateEvent.isCancelled()) return List.of();

        List<String> res = bit.selfUpdate(item);
        updateItemDisplayView(item);
        return res;
    }

    /**
     * 根据物品Meta和PDC修改物品显示
     */
    public void updateItemDisplayView(@NotNull ItemStack item) {
        BaseItem baseItem = getBaseItem(item);
        /* 本质上也判断了是否为插件物品 */
        if (baseItem == null) return;

        CalculablePDC cPDC = getCalculablePDC(item);
        /* 原版属性绑定 (只有ItemPDC有原版属性) */
        if (cPDC instanceof ItemPDC iPDC) iPDC.attachOriAttrsTo(item);

        LoreTemplate loreTemplate = loreTemplateMap.get(baseItem.getLoreTemplateName());
        /* TODO 这一块的逻辑很乱 */
        /* 没有模板则直接将内部的lore直接赋值 */
        if (loreTemplate == null) {
            ItemMeta meta = item.getItemMeta();
            meta.lore(baseItem.getItemLore().stream().map(MiniMessageUtil::serialize).toList());
            item.setItemMeta(meta);
            return;
        }
        loreTemplate.AttachLoreToItem(item);
    }

    /**
     * 将物品的品质剥离
     * TODO 指令执行
     */
    public void toPaleItem(@NotNull ItemStack item) {
        setQualityName(item, "");
        updateItemDisplayView(item);
    }

    /**
     * 灵魂绑定
     */
    public void doSoulBind(@NotNull ItemStack item, @NotNull Player player) {
        setOwner(item, player.getUniqueId().toString());
    }

    /** 加载所有客制化物品 */
    private void loadConfigs() {
        if (!rootDir.exists()) {
            logWarning("Main directory not found. Please set generate-examples to true and reload the plugin.");
            return;
        }
        this.loadLevelFiles();
        this.loadQualityFiles();
        this.loadItemFiles();
        this.loadLoreTemplateFiles();
        this.loadSkillFiles();
    }

    /**
     * 根据 QualityGroups 中的可用品质随机选择一个
     * @param qualityGroupNames 可选的品质组名列表
     * @return 选中的品质类实例
     */
    private @Nullable AttributePDC randomQuality(@Nullable List<String> qualityGroupNames) {
        List<QualityGroup> qgs = new ArrayList<>();
        if (qualityGroupNames == null) return null;
        QualityGroup qg;
        for (String qualityGroupName : qualityGroupNames) {
            qg = qualityGroupMap.get(qualityGroupName);
            if (qg == null) {
                logWarning("品质组: " + qualityGroupName + " 不存在");
                continue;
            }
            qgs.add(qg);
        }
        return multiplyRandomChoice(qgs);
    }

    private void loadQualityMap() {
        for(QualityGroup qgm: qualityGroupMap.values()) {
            for(int i = 0; i < qgm.getLen(); i++) {
                qualityMap.put(qgm.getQualityNames().get(i), qgm.getQualities().get(i));
            }
        }
    }

    private void loadItemFiles() {
        Map<String, ConfigurationSection> cfg = new HashMap<>();
        loadCustomConfigs(new File(rootDir, "items"), cfg, "");
        ConfigurationSection sec;
        for (ConfigurationSection c : cfg.values()) {
            for (String key : c.getKeys(false)) {
                if (itemMap.containsKey(key)) {
                    logWarning("物品内部名重复: " + key + " !");
                    continue;
                }
                sec = c.getConfigurationSection(key);
                if (sec == null) {
                    logWarning("ITEM: " + key + " 的具体配置为空");
                    return;
                }

                String type = sec.getString(TYPE, TYPE_ITEM);
                switch (type.toLowerCase()) {
                    case TYPE_ITEM -> itemMap.put(key, new Item(key, sec));
                    case TYPE_GEM -> itemMap.put(key, new Gem(key, sec));
                    case TYPE_MATERIAL -> itemMap.put(key, new Material(key, sec));
                    case TYPE_CONSUMABLE -> itemMap.put(key, new Consumable(key, sec));
                }
            }
        }
    }

    private void loadQualityFiles() {
        Map<String, ConfigurationSection> cfg = new HashMap<>();
        loadCustomConfigs(new File(rootDir, "qualities"), cfg, "");
        ConfigurationSection sec;
        for (ConfigurationSection c : cfg.values()) {
            for (String key : c.getKeys(false)) {
                if (qualityGroupMap.containsKey(key)) {
                    logWarning("重复的品质名: " + key + " !");
                    continue;
                }
                sec = c.getConfigurationSection(key);
                if (sec == null) {
                    logWarning("QUALITY: " + key + " 的具体配置为空");
                    return;
                }
                qualityGroupMap.put(key, new QualityGroup(key, sec));
            }
        }
    }

    private void loadLevelFiles() {
        Map<String, ConfigurationSection> cfg = new HashMap<>();
        loadCustomConfigs(new File(rootDir, "levels"), cfg, "");
        ConfigurationSection sec;
        for (ConfigurationSection c : cfg.values()) {
            for (String key : c.getKeys(false)) {
                if (levelTemplateMap.containsKey(key)) {
                    logWarning("重复的等级模板: " + key + " !");
                    continue;
                }
                sec = c.getConfigurationSection(key);
                if (sec == null) {
                    logWarning("LEVEL: " + key + " 的具体配置为空");
                    continue;
                }
                levelTemplateMap.put(key, new LevelTemplate(key, sec));
            }
        }
    }

    private void loadLoreTemplateFiles() {
        Map<String, ConfigurationSection> cfg = new HashMap<>();
        loadCustomConfigs(new File(rootDir, "lore"), cfg, "");
        for (ConfigurationSection c : cfg.values()) {
            for (String key : c.getKeys(false)) {
                if (loreTemplateMap.containsKey(key)) {
                    logWarning("重复的 LORE 模板: " + key + " !");
                    continue;
                }
                List<String> lt = c.getStringList(key);
                if (lt.isEmpty()) {
                    logWarning("LORE: " + key + " 的具体配置为空");
                    continue;
                }
                loreTemplateMap.put(key, new LoreTemplate(key, lt));
            }
        }
    }

    private void loadSkillFiles() {
        // Map<String, ConfigurationSection> cfg = loadSubFiles("skills");
    }
}
