package io.github.tanice.twItemManager.manager.item;

import io.github.tanice.twItemManager.TwItemManager;
import io.github.tanice.twItemManager.event.TwItemUpdateEvent;
import io.github.tanice.twItemManager.infrastructure.PDCAPI;
import io.github.tanice.twItemManager.manager.item.base.impl.Gem;
import io.github.tanice.twItemManager.manager.item.base.impl.Item;
import io.github.tanice.twItemManager.manager.item.level.LevelTemplate;
import io.github.tanice.twItemManager.manager.item.quality.QualityGroup;
import io.github.tanice.twItemManager.manager.pdc.CalculablePDC;
import io.github.tanice.twItemManager.manager.pdc.impl.AttributePDC;
import io.github.tanice.twItemManager.manager.pdc.impl.ItemPDC;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.*;

import static io.github.tanice.twItemManager.config.Config.loadCustomConfigs;
import static io.github.tanice.twItemManager.infrastructure.PDCAPI.*;
import static io.github.tanice.twItemManager.manager.item.quality.QualityGroup.multiplyRandomChoice;
import static io.github.tanice.twItemManager.util.Logger.logWarning;

/**
 * 将 base lore quality skill 集合成最终的TwItem实例
 */
public class ItemManager implements Manager {
    private final JavaPlugin plugin;
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
    /** 物品描述模板 */
    // private final LoreManager loreManager;
    /** key 技能名 value 技能类 */
    // private final Map<String, SkillManager> skillManagerMap;
    /** key 升级模板名 value 升级模板类 */
    private final Map<String, LevelTemplate> levelTemplateMap;

    public ItemManager(@NotNull JavaPlugin plugin) {
        this.plugin = plugin;
        rootDir = plugin.getDataFolder();
        itemMap = new HashMap<>();
        gemMap = new HashMap<>();
        qualityGroupManagerMap = new HashMap<>();
        qualityMap = new HashMap<>();
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
        // buffSectionMap.clear();
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
    public @Nullable Item getItemByItemStack(@Nullable ItemStack item){
        if (item == null) return null;
        return itemMap.get(getInnerName(item));
    }

    public @Nullable Item getItemByInnerName(@NotNull String innerName){
        return itemMap.get(innerName);
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
        return (AttributePDC) cPDC;
    }

    /**
     * 获取全局宝石名称列表
     */
    public Set<String> getGemNameList() {
        return gemMap.keySet();
    }

    /**
     * 获取全局品质列表的具体属性
     */
    public @Nullable AttributePDC getQualityPDC(@NotNull String qualityName) {
        return qualityMap.get(qualityName);
    }

    /**
     * 获取等级对应的属性
     * 传入 level 便于扩展等级属性
     * 如 不同等级不同属性，可累加，可选择等
     */
    public @Nullable AttributePDC getLevelPDC(@NotNull String levelTemplateName) {
        LevelTemplate lt = levelTemplateMap.get(levelTemplateName);
        if (lt == null) return null;
        return lt.getAttributePDC();
    }

    /**
     * 获取等级模板属性
     * 便于判定相关属性是否合法
     */
    public @Nullable LevelTemplate getLevelTemplate(@NotNull String levelTemplateName) {
        return levelTemplateMap.get(levelTemplateName);
    }

    /**
     * 判断
     */
    public boolean isNotTwItem(@NotNull String innerName) {
        return !itemMap.containsKey(innerName);
    }

    public boolean isNotTwItem(@Nullable ItemStack item) {
        String in = getInnerName(item);
        if (in == null) return true;
        return isNotTwItem(in);
    }

    public boolean isNotGem(@NotNull String innerName) {
        return !gemMap.containsKey(innerName);
    }

    public boolean isNotGem(@NotNull ItemStack item) {
        String in = getInnerName(item);
        if (in == null) return true;
        return isNotGem(in);
    }

    /**
     * 获取物品的 ItemPDC
     */
    public String getItemStringItemPDC(@NotNull ItemStack item) {
        ItemPDC iPDC = (ItemPDC) getItemCalculablePDC(item);
        if (iPDC == null) return "此物品没有持久化的PDC";
        return iPDC.toString();
    }

    /**
     * 获取物品的AttributePDC
     */
    public String getItemStringAttributePDC(@NotNull ItemStack item) {
        AttributePDC iPDC = (AttributePDC) getItemCalculablePDC(item);
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
        Item it = itemMap.get(innerName);
        ItemStack item = it.getItem();
        if (item.getType() == Material.AIR) return item;

        /* 获取随机品质 */
        if (getQualityName(item).isEmpty()) {
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
        completeItem(item);
        return item;
    }

    @Override
    public @NotNull ItemStack generateGemItem(@NotNull String innerName) {
        if (!gemMap.containsKey(innerName)){
            logWarning("宝石: " + innerName + "不存在");
            return new ItemStack(Material.AIR);
        }
        Gem baseGem = gemMap.get(innerName);
        ItemStack item = baseGem.getItem();
        completeItem(item);
        return item;
    }

    @Override
    public void completeItem(@NotNull ItemStack item) {
        CalculablePDC cPDC = getItemCalculablePDC(item);
        if (cPDC == null) return;

        /* lore 更新 */

        if (cPDC instanceof ItemPDC iPDC) {
            /* 原版属性绑定 */
            iPDC.attachOriAttrsTo(item);
            /* 品质显示 */

            /* 等级显示 */

        }
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
        completeItem(item);
        return true;
    }

    @Override
    public boolean levelUp(@NotNull Player player, @NotNull ItemStack item, @Nullable ItemStack levelUpNeed, boolean check) {
        if (isNotTwItem(item)) return false;
        String innerName = getInnerName(item);
        String lvlName = itemMap.get(innerName).getLevelTemplateName();
        if (lvlName.isEmpty()) return false;
        LevelTemplate lt = levelTemplateMap.get(lvlName);
        if (lt == null) return false;

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
    public void levelDown(@NotNull Player player, @NotNull ItemStack item) {
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

    @Override
    public void levelSet(@NotNull Player player, @NotNull ItemStack item, int level) {
        if (isNotTwItem(item)) {
            player.sendMessage("§e物品无等级");
            return;
        }
        String innerName = getInnerName(item);
        String lvlName = itemMap.get(innerName).getLevelTemplateName();
        if (lvlName.isEmpty()) {
            player.sendMessage("§e物品无等级");
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
    }

    @Override
    public boolean insertGem(Player player, ItemStack item, ItemStack gem) {
        if (isNotTwItem(item) || isNotGem(gem)) return false;

        ItemPDC iPDC = (ItemPDC) getItemCalculablePDC(item);
        Gem g = getGem(gem);
        if (iPDC == null || g == null) return false;

        boolean f = iPDC.hasEmptyGemSlot();
        if (!f) {
            player.sendMessage("§e槽位已满");
            return false;
        }

        if (Math.random() < g.getChance()){
            f = iPDC.addGem(getInnerName(gem));
            if (f) {
                player.sendMessage("§a宝石镶嵌成功!");
                setItemCalculablePDC(item, iPDC);
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
        return false;
    }

    @Override
    public ItemStack removeGem(Player player, ItemStack item) {
        ItemStack gem = new ItemStack(Material.AIR);
        if (isNotTwItem(item)) return gem;

        ItemPDC iPDC = (ItemPDC) getItemCalculablePDC(item);
        if (iPDC == null) {
            player.sendMessage("§e获取物品出错");
            return gem;
        }

        String gn = iPDC.removeRandomGem();
        if (gn.isEmpty()) {
            player.sendMessage("§e此物品没有宝石");
            return gem;
        }
        player.sendMessage("§a宝石拆卸成功!");
        setItemCalculablePDC(item, iPDC);

        gem = generateGemItem(gn);
        if (gem.getType().isAir()) {
            player.sendMessage("§e此宝石已经不存在，无法获取");
            logWarning("玩家: " + player.getName() + "所拆卸下的宝石: " + gn +"不存在");
        }
        return gem;
    }

    @Override
    public List<String> updateItem(@NotNull Player player, @NotNull ItemStack item){
        String inn = PDCAPI.getInnerName(item);
        if (inn == null) return new ArrayList<>(0);

        CalculablePDC prePDC = PDCAPI.getItemCalculablePDC(item);
        Item it = TwItemManager.getItemManager().getItemByItemStack(item);
        if (prePDC instanceof ItemPDC iPDC && it != null) {
            /* 获取底层抽象 */
            ItemPDC newPDC = (ItemPDC) PDCAPI.getItemCalculablePDC(it.getItem());
            if (newPDC == null) {
                logWarning("物品更新后找不到对应的底层: " + inn);
                return new ArrayList<>(0);
            }
            List<String> externalGems = newPDC.extendFrom(iPDC);

            TwItemUpdateEvent event = new TwItemUpdateEvent (plugin, player, iPDC, newPDC);
            Bukkit.getPluginManager().callEvent(event);
            if (event.isCancelled()) return new ArrayList<>(0);
            /* 原版属性操作 */
            iPDC.removeOriAttrsFrom(item);
            newPDC.attachOriAttrsTo(item);
            PDCAPI.setItemCalculablePDC(item, newPDC);
            /* 多余的宝石给予玩家 */
            return externalGems;
        }
        return new ArrayList<>(0);
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
        completeItem(item);
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
}
