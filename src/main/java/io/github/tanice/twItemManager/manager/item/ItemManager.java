package io.github.tanice.twItemManager.manager.item;

import io.github.tanice.twItemManager.infrastructure.PDCAPI;
import io.github.tanice.twItemManager.manager.item.base.impl.Gem;
import io.github.tanice.twItemManager.manager.item.base.impl.Item;
import io.github.tanice.twItemManager.manager.item.level.LevelTemplate;
import io.github.tanice.twItemManager.manager.item.quality.QualityGroup;
import io.github.tanice.twItemManager.manager.pdc.CalculablePDC;
import io.github.tanice.twItemManager.manager.pdc.impl.AttributePDC;
import io.github.tanice.twItemManager.manager.pdc.impl.BuffPDC;
import io.github.tanice.twItemManager.manager.pdc.impl.ItemPDC;
import io.github.tanice.twItemManager.manager.pdc.type.AttributeCalculateSection;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

import static io.github.tanice.twItemManager.config.Config.loadCustomConfigs;
import static io.github.tanice.twItemManager.infrastructure.PDCAPI.*;
import static io.github.tanice.twItemManager.manager.item.quality.QualityGroup.multiplyRandomChoice;
import static io.github.tanice.twItemManager.util.Logger.logWarning;

/**
 * 将 base lore quality skill 集合成最终的TwItem实例
 */
public class ItemManager implements Manager {
    /** 主目录文件 */
    private final File rootDir;
    /** key 物品内部名  value 基础物品类(包含物品的基础信息) 通过调用generate函数获取物品实例 */
    private final Map<String, Item> itemMap;
    /** 宝石 */
    private final Map<String, Gem> gemMap;
    /** key 组名  value 品质组Map(key 品质名 value 品质类) */
    private final Map<String, QualityGroup> qualityGroupManagerMap;
    /** 全局可用品质(便于检索和切换) */
    private final Map<String, AttributePDC> qualityMap;
    /** 全局可用的buff属性列表 */
    private final EnumMap<AttributeCalculateSection, List<BuffPDC>> buffSectionMap;
    /** 全局可用Buff(便于检索和切换) */
    private final Map<String, BuffPDC> buffMap;
    /** 物品描述模板 */
    // private final LoreManager loreManager;
    /** key 技能名 value 技能类 */
    // private final Map<String, SkillManager> skillManagerMap;
    /** key 升级模板名 value 升级模板类 */
    private final Map<String, LevelTemplate> levelTemplateMap;

    public ItemManager(@NotNull JavaPlugin plugin) {
        rootDir = plugin.getDataFolder();
        itemMap = new HashMap<>();
        gemMap = new HashMap<>();
        qualityGroupManagerMap = new HashMap<>();
        qualityMap = new HashMap<>();
        buffSectionMap = new EnumMap<>(AttributeCalculateSection.class);
        buffMap = new HashMap<>();
        // skillManagerMap = new HashMap<>();
        levelTemplateMap = new HashMap<>();
        // loreManager = new LoreManager();
        this.loadConfigs();
        /* 初始化全局qualityMap */
        this.loadQualityMap();
    }

    public void reload() {
        itemMap.clear();
        gemMap.clear();
        qualityGroupManagerMap.clear();
        qualityMap.clear();
        buffSectionMap.clear();
        // skillManagerMap.clear();
        levelTemplateMap.clear();
        // loreManager 在 loadConfigs 中初始化
        loadConfigs();
        /* 初始化全局qualityMap */
        loadQualityMap();
    }

    /**
     * 根据物品 item 获取内部的持久类
     */
    public Item getItem(@NotNull ItemStack item){
        return itemMap.get(getInnerName(item));
    }
    public Set<String> getItemNameList() {
        return itemMap.keySet();
    }

    /**
     * 根据宝石物品获取内部持久类
     */
    public Gem getGem(@NotNull ItemStack item) {
        return gemMap.get(getInnerName(item));
    }

    /**
     * 根据宝石内部名称获取其对应的属性抽象
     */
    public @Nullable AttributePDC getGemAttributePDC(@NotNull String gemName) {
        CalculablePDC cPDC = getItemCalculablePDC(gemMap.get(gemName).getItem());
        if (cPDC == null) return null;
        return cPDC.toAttributePDC();
    }

    /**
     * 获取全局宝石名称列表
     * @return
     */
    public Set<String> getGemNameList() {
        return gemMap.keySet();
    }

    /**
     * 获取全局buff列表的具体属性
     */
    public @Nullable BuffPDC getBuffPDC(@NotNull String buffName) {
        return buffMap.get(buffName);
    }

    /**
     * 获取全局品质列表的具体属性
     */
    public AttributePDC getQualityPDC(@NotNull String qualityName) {
        return qualityMap.get(qualityName);
    }

    /**
     * 获取等级对应的属性
     * 传入 level 便于扩展等级属性
     * 如 不同等级不同属性，可累加，可选择等
     */
    public AttributePDC getLevelPDC(@NotNull String levelTemplateName, int level) {
        return levelTemplateMap.get(levelTemplateName).getAttributePDC();
    }

    /**
     * 获取等级模板属性
     * 便于判定相关属性是否合法
     */
    public LevelTemplate getLevelTemplate(@NotNull String levelTemplateName) {
        return levelTemplateMap.get(levelTemplateName);
    }

    public boolean isNotTwItem(@NotNull String innerName) {
        return !itemMap.containsKey(innerName);
    }

    public boolean isNotTwItem(@NotNull ItemStack item) {
        String in = getInnerName(item);
        if (in == null) return true;
        return !itemMap.containsKey(in);
    }

    public String getItemPDC(@NotNull ItemStack item) {
        ItemPDC iPDC = (ItemPDC) getItemCalculablePDC(item);
        if (iPDC == null) return "此物品没有持久化的PDC";
        return iPDC.toString();
    }

    /**
     * 接口方法实现
     */

    @Override
    public ItemStack generateItem(@NotNull String innerName){
        if (!itemMap.containsKey(innerName)){
            logWarning("物品: " + innerName + "不存在");
            return new ItemStack(Material.AIR);
        }
        /* 获取基础物品的可更改备份 */
        Item baseItem = itemMap.get(innerName);
        ItemStack item = baseItem.getItem();
        if (item.getType() == Material.AIR) return item;

        /* 获取随机品质 */
        AttributePDC aPDC = randomQuality(baseItem.getQualityGroups());
        if (aPDC != null) setQualityName(item, aPDC.getInnerName());

        // TODO step3 绑定技能
        updateItem(item);
        return item;
    }

    @Override
    public void updateItem(@NotNull ItemStack item) {
        CalculablePDC cPDC = getItemCalculablePDC(item);
        if (cPDC == null) return;
        /* lore 更新 */
        /* 原版属性绑定 + 等级显示 */
        ((ItemPDC)cPDC).attachOriAttrsTo(item);
    }

    @Override
    public ItemStack generateGemItem(@NotNull String innerName) {
        if (!gemMap.containsKey(innerName)){
            logWarning("宝石: " + innerName + "不存在");
            return new ItemStack(Material.AIR);
        }
        /* 获取基础物品的可更改备份 */
        return gemMap.get(innerName).getItem();
    }

    @Override
    public boolean recast(@NotNull ItemStack item) {
        if (item.getMaxStackSize() > 1) {
            logWarning("No way for stackable things to recast");
            return false;
        }
        toPaleItem(item);
        /* 获取随机品质 */
        AttributePDC aPDC = randomQuality(itemMap.get(getInnerName(item)).getQualityGroups());
        if (aPDC != null) setQualityName(item, aPDC.getInnerName());
        updateItem(item);
        return true;
    }

    @Override
    public boolean levelUp(Player player, ItemStack item, ItemStack levelUpNeed, boolean check) {
        if (isNotTwItem(item)) return false;
        String innerName = getInnerName(item);
        String lvlName = itemMap.get(innerName).getLevelTemplateName();
        if (lvlName.isEmpty()) {
            if (player.isOp()) logWarning("No level template for: " + innerName);
            return false;
        }
        LevelTemplate lt = levelTemplateMap.get(lvlName);

        if (lt == null) {
            player.sendMessage("§e此物品无法强化");
            logWarning("无效的升级模板名称: " + lvlName + " in: " + innerName);
            return false;
        }
        int lvl = getLevel(item);
        if (lvl < lt.getBegin() || lvl > lt.getMax()) {
            logWarning("非法等级: " + lvl + " in: " + getInnerName(item) + ". Player: " + player.getName());
            return false;
        }
        if (lvl == lt.getMax()) {
            player.sendMessage("§e物品已经达到最大等级");
            return false;
        }
        // 判断升级物品是否正确
        if (check && (isNotTwItem(levelUpNeed) || !lt.getLevelUpNeed().equals(getInnerName(levelUpNeed)))) return false;

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
        return true;
    }

    @Override
    public void levelDown(Player player, ItemStack item) {
        if (isNotTwItem(item)) return;
        String innerName = getInnerName(item);
        String lvlName = itemMap.get(innerName).getLevelTemplateName();
        if (lvlName.isEmpty()) {
            player.sendMessage("§e物品无法降级");
            logWarning("此物品无等级: " + innerName);
            return;
        }

        LevelTemplate lt = levelTemplateMap.get(lvlName);
        if (lt == null) {
            player.sendMessage("§e物品无法降级");
            logWarning("无效的升级模板名称: " + lvlName + " in: " + innerName);
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
    }


    /** 加载所有客制化物品 */
    private void loadConfigs() {
        if (!rootDir.exists()) {
            logWarning("Main directory not found. Please set generate-examples to true and reload the plugin.");
            return;
        }
        this.loadLoreTemplateFiles();
        this.loadLevelFiles();
        this.loadGemFiles();
        this.loadQualityFiles();
        this.loadItemFiles();
        this.loadSkillFiles();
        this.loadBuffFilesAndBuffMap();
    }

    /**
     * 根据 QualityGroups 中的可用品质随机选择一个
     * @param QualityGroupNames 可选的品质组名列表
     * @return 选中的品质类实例
     */
    private @Nullable AttributePDC randomQuality(@NotNull List<String> QualityGroupNames) {
        if (QualityGroupNames.isEmpty()) return null;

        List<QualityGroup> qgs = new ArrayList<>();
        QualityGroup qg;
        for (String qualityGroupName : QualityGroupNames) {
            qg = qualityGroupManagerMap.get(qualityGroupName);
            if (qg == null) {
                logWarning("No quality group found with name: " + qualityGroupName);
                continue;
            }
            qgs.add(qg);
        }
        return multiplyRandomChoice(qgs);
    }

    /**
     * 将物品的品质剥离
     */
    private void toPaleItem(@NotNull ItemStack item) {
        setQualityName(item, "");
        updateItem(item);
    }

    private void loadQualityMap() {
        for(QualityGroup qgm: qualityGroupManagerMap.values()) {
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
                itemMap.put(key, new Item(key, sec));
            }
        }
    }

    private void loadSkillFiles() {
        // Map<String, ConfigurationSection> cfg = loadSubFiles("skills");
    }

    private void loadQualityFiles() {
        Map<String, ConfigurationSection> cfg = new HashMap<>();
        loadCustomConfigs(new File(rootDir, "qualities"), cfg, "");
        ConfigurationSection sec;
        for (ConfigurationSection c : cfg.values()) {
            for (String key : c.getKeys(false)) {
                if (qualityGroupManagerMap.containsKey(key)) {
                    logWarning("重复的品质名: " + key + " !");
                    continue;
                }
                sec = c.getConfigurationSection(key);
                if (sec == null) {
                    logWarning("QUALITY: " + key + " 的具体配置为空");
                    return;
                }
                qualityGroupManagerMap.put(key, new QualityGroup(key, sec));
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

    private void loadGemFiles() {
        Map<String, ConfigurationSection> cfg = new HashMap<>();
        loadCustomConfigs(new File(rootDir, "gems"), cfg, "");
        ConfigurationSection sec;
        for (ConfigurationSection c : cfg.values()) {
            for (String key : c.getKeys(false)) {
                if (gemMap.containsKey(key)) {
                    logWarning("宝石名重复: " + key + " !");
                    continue;
                }
                sec = c.getConfigurationSection(key);
                if (sec == null) {
                    logWarning("GEM: " + key + " 的具体配置为空");
                    return;
                }
                gemMap.put(key, new Gem(key, sec));
            }
        }
    }

    private void loadLoreTemplateFiles() {
        // Map<String, ConfigurationSection> cfg = loadSubFiles("lore");
    }

    private void loadBuffFilesAndBuffMap(){
        Path buffDir = rootDir.toPath().resolve("buff"); ;
        if (!Files.exists(buffDir) || !Files.isDirectory(buffDir)) return;
        try (Stream<Path> files = Files.list(buffDir)) {
            files.forEach(file -> {
                String fileName = file.getFileName().toString();
                String name = fileName.substring(0, fileName.lastIndexOf('.'));
                BuffPDC bPDC;
                ConfigurationSection subsection;
                if (fileName.endsWith(".yml")) {
                    ConfigurationSection section = YamlConfiguration.loadConfiguration(file.toFile());
                    for (String k : section.getKeys(false)) {
                        subsection = section.getConfigurationSection(k);
                        if (subsection == null) continue;
                        bPDC = new BuffPDC(k, section);
                        buffSectionMap.getOrDefault(bPDC.getAttributeCalculateSection(), new ArrayList<>()).add(bPDC);
                        buffMap.put(k, bPDC);
                    }
                }
                else if (fileName.endsWith(".js")) {
                    bPDC = new BuffPDC(name, file);
                    buffSectionMap.getOrDefault(bPDC.getAttributeCalculateSection(), new ArrayList<>()).add(bPDC);
                    buffMap.put(bPDC.getInnerName(), bPDC);
                }
                else logWarning("未知的文件格式: " + fileName);
            });
        } catch (IOException e) {
            logWarning("加载BUFF文件错误");
            logWarning(e.toString());
        }
    }
}
