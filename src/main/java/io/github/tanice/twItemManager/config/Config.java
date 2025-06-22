package io.github.tanice.twItemManager.config;

import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Stream;

import static io.github.tanice.twItemManager.util.Logger.logWarning;

/**
 * 插件配置管理类
 * 管理全局配置，同时提供读取文件夹内所有yml文件的方法
 */
@Getter
public class Config {
    public static double version;
    public static boolean debug;

    /* 生成示例文件 */
    public static boolean generateExamples;

    /* 阻止原版的心形粒子和灰尘粒子 */
    public static boolean cancelGenericParticles;

    /* 使用伤害指示器 */
    public static boolean generateDamageIndicator;
    public static String defaultPrefix;
    public static String criticalPrefix;
    public static double criticalLargeScale;
    public static double viewRange;

    /* 防御伤害抵消乘数 */
    public static double worldK;

    /* 伤害浮动机制 */
    public static boolean damageFloat;
    public static double damageFloatRange;

    /* 玩家减伤平衡算法 */
    public static boolean useDamageReductionBalanceForPlayer;

    /* 跳劈伤害加成 */
    public static double originalCriticalStrikeAddition;

    /* 数据库配置 */
    public static boolean use_mysql;
    public static String host;
    public static String port;
    public static String database_name;
    public static String username;
    public static String password;

    /** 激活时 加载全局配置文件 */
    public static void onEnable(@NotNull JavaPlugin plugin) {
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "config.yml"));
        ConfigurationSection sub;

        /* 与版本号相关，如果版本号不一致则会刷新全服务器的实体的 EntityPDC */
        version = cfg.getDouble("VERSION", -1);
        debug = cfg.getBoolean("DEBUG", false);

        generateExamples = cfg.getBoolean("generate_examples", true);
        cancelGenericParticles = cfg.getBoolean("cancel_generic_particles", false);

        /* 使用伤害指示器 */
        generateDamageIndicator = cfg.getBoolean("generate_damage_indicator", false);
        defaultPrefix = cfg.getString("default_prefix", "§6");
        criticalPrefix = cfg.getString("critical_prefix", "§4");
        criticalLargeScale = cfg.getDouble("critical_large_scale", 1D);
        viewRange = cfg.getDouble("view_range", 20D);

        /* 防御伤害抵消乘数 */
        worldK = cfg.getDouble("world_k", 1D);

        /* 伤害浮动机制 */
        damageFloat = cfg.getBoolean("damage_float", false);
        damageFloatRange = cfg.getDouble("damage_float_range", 0D);

        useDamageReductionBalanceForPlayer = cfg.getBoolean("use_damage_reduction_balance_for_player", false);

        originalCriticalStrikeAddition = cfg.getDouble("original_critical_strike_addition", 0.2D);

        /* 数据库配置 */
        sub = cfg.getConfigurationSection("database");
        if (sub == null) {
            logWarning("全局配置文件错误，无法连接数据库");
            use_mysql = false;
            return;
        }
        use_mysql = sub.getBoolean("use_mysql", false);
        sub = sub.getConfigurationSection("mysql");
        if (sub != null) {
            host = sub.getString("host", "localhost");
            port = sub.getString("port", "3306");
            database_name = sub.getString("database_name");
            username = sub.getString("username");
            password = sub.getString("password");
        }
        if (use_mysql && sub == null) {
            logWarning("全局配置文件错误，无法连接数据库");
            use_mysql = false;
        }
    }

    /** 插件重载时 重载对应配置文件 */
    public static void onReload(@NotNull JavaPlugin plugin) {
        onEnable(plugin);
    }

    /** 生成全局配置文件 config.yml 和 配置示例文件 /XX/example_XXX.yml */
    public static void save(@NotNull JavaPlugin plugin){
        // 第一次加载_保存默认全局配置文件
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        // 没有全局配置文件则直接创建
        if(!configFile.exists()){
            generateExampleConfig(plugin);
            return;
        }
        // 已经有配置文件__判断是否需要生成示例配置文件
        if(generateExamples) generateExampleConfig(plugin);
    }

    /**
     * 递归加载路径下所有yml结尾的文件
     * @param directory 主目录
     * @param configs 所有yml配置文件的储存(确保修改的是同一个值)
     * @param path 起始路径(用于递归记录当前路径)
     */
    public static void loadCustomConfigs(@NotNull File directory, Map<String, ConfigurationSection> configs, String path) {
        File[] fs = directory.listFiles();
        if (fs == null) return;
        for (File file : fs) {
            String filePath = !path.isEmpty() ? path + File.separator + file.getName() : file.getName();
            if (file.isDirectory()) {
                loadCustomConfigs(file, configs, filePath);
            } else if (file.getName().endsWith(".yml")) {
                configs.put(filePath, YamlConfiguration.loadConfiguration(file));
            }
        }
    }

    /** 自动生成默认配置文件(存在的文件不会被更改) */
    private static void generateExampleConfig(@NotNull JavaPlugin plugin){
        File targetFolder = plugin.getDataFolder();
        URL sourceUrl = plugin.getClass().getResource("");
        if (sourceUrl == null) {
            plugin.getLogger().warning("The plugin package is incomplete, please re_download it!");
            return;
        }

        try (FileSystem fs = FileSystems.newFileSystem(sourceUrl.toURI(), Collections.emptyMap())) {
            Path cp = fs.getPath("/config/");
            try (Stream<Path> sourcePaths = Files.walk(cp)) {
                for (Path source : sourcePaths.toArray(Path[]::new)) {
                    Path targetPath = targetFolder.toPath().resolve(cp.relativize(source).toString());
                    if (Files.exists(targetPath)) continue;
                    if (Files.isDirectory(source)) Files.createDirectory(targetPath);
                    else Files.copy(source, targetPath);
                }
                plugin.getLogger().info("generate example config files success!");
            }
        } catch (IOException | URISyntaxException e) {
            plugin.getLogger().warning("Failed to load default example config file: " + e.getMessage());
        }
    }
}
