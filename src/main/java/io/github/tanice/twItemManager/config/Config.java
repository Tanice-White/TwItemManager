package io.github.tanice.twItemManager.config;

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
import java.util.Objects;
import java.util.stream.Stream;

/**
 * 插件配置管理类
 * 管理全局配置，同时提供读取文件夹内所有yml文件的方法
 */
public class Config {

    public static boolean generateExamples;
    public static boolean cancelGenericParticles;
    public static boolean generateDamageIndicator;
    public static boolean anvilRepairable;
    public static boolean grindstoneRepairable;
    public static boolean canEnchant;

    /** 激活时 加载全局配置文件 */
    public static void onEnable(@NotNull JavaPlugin plugin) {
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "config.yml"));
        generateExamples = cfg.getBoolean("generate-examples", true);
        cancelGenericParticles = cfg.getBoolean("cancel-generic-particles", false);
        generateDamageIndicator = cfg.getBoolean("generate-damage-indicator", false);
        anvilRepairable = cfg.getBoolean("anvil-repairable", true);
        grindstoneRepairable = cfg.getBoolean("grindstone-repairable", true);
        canEnchant = cfg.getBoolean("can-enchant", true);
    }

    /** 插件重载时 重载对应配置文件 */
    public static void onReload(@NotNull JavaPlugin plugin) {
        onEnable(plugin);
    }

    /** 生成全局配置文件 config.yml 和 配置示例文件 /XX/example-XXX.yml */
    public static void save(@NotNull JavaPlugin plugin){
        // 第一次加载-保存默认全局配置文件
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        // 没有全局配置文件则直接创建
        if(!configFile.exists()){
            generateExampleConfig(plugin);
            return;
        }
        // 已经有配置文件--判断是否需要生成示例配置文件
        if(generateExamples) generateExampleConfig(plugin);
    }

    /**
     * 递归加载路径下所有yml结尾的文件
     * @param directory 主目录
     * @param configs 所有yml配置文件的储存(确保修改的是同一个值)
     * @param path 起始路径(用于递归记录当前路径)
     */
    public static void loadCustomConfigs(@NotNull File directory, Map<String, ConfigurationSection> configs, String path) {
        for (File file : Objects.requireNonNull(directory.listFiles())) {
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
            plugin.getLogger().warning("The plugin package is incomplete, please re-download it!");
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
