package io.github.tanice.twItemManager.manager.skill;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static io.github.tanice.twItemManager.util.Logger.logInfo;
import static io.github.tanice.twItemManager.util.Logger.logWarning;

/**
 * 管理技能和技能冷却
 */
public class SkillManager {
    private final JavaPlugin plugin;

    private final Map<String, SkillData> skillMap;
    private final ConcurrentMap<String, ConcurrentMap<String, Long>> playerSkillCooldowns;

    public SkillManager(@NotNull JavaPlugin plugin) {
        this.plugin = plugin;
        skillMap = new HashMap<>();
        playerSkillCooldowns = new ConcurrentHashMap<>();
        this.loadSkills();
    }

    public void onReload() {
        skillMap.clear();
        this.loadSkills();
        logInfo("SkillManager reloaded");
    }

    public void OnDisable() {
        skillMap.clear();
        playerSkillCooldowns.clear();
    }

    /**
     * 获取技能数据
     */
    public @Nullable SkillData getSkillData(@NotNull String skillName) {
        return skillMap.get(skillName);
    }

    /**
     * 获取技能下次可用时间戳（不存在则创建并返回0）
     *
     * @param playerId 玩家ID
     * @param skillName 技能ID
     * @return 技能下次可用时间戳（单位：毫秒）
     */
    public long getSkillCooldown(@NotNull String playerId, @NotNull String skillName) {
        ConcurrentMap<String, Long> skillMap = playerSkillCooldowns.computeIfAbsent(
                playerId, k -> new ConcurrentHashMap<>()
        );
        return skillMap.computeIfAbsent(skillName, k -> 0L);
    }

    /**
     * 设置技能冷却时间戳
     *
     * @param playerId 玩家ID
     * @param skillName 技能ID
     * @param cooldownTimestamp 下次可用时间戳
     */
    public void setCooldown(@NotNull String playerId, @NotNull String skillName, long cooldownTimestamp) {
        playerSkillCooldowns
                .computeIfAbsent(playerId, k -> new ConcurrentHashMap<>())
                .put(skillName, cooldownTimestamp);
    }

    /**
     * 清除玩家所有技能数据（玩家下线时调用）
     *
     * @param playerId 玩家ID
     */
    public void clearPlayerData(@NotNull String playerId) {
        playerSkillCooldowns.remove(playerId);
    }

    public void addPlayerData(@NotNull String playerId) {
        playerSkillCooldowns.put(playerId, new ConcurrentHashMap<>());
    }

    private void loadSkills(){
        AtomicInteger total = new AtomicInteger();
        Path buffDir = plugin.getDataFolder().toPath().resolve("skills");
        if (!Files.exists(buffDir) || !Files.isDirectory(buffDir)) return;
        try (Stream<Path> files = Files.list(buffDir)) {
            files.forEach(file -> {
                String fileName = file.getFileName().toString();
                SkillData sd;
                ConfigurationSection subsection;
                if (fileName.endsWith(".yml")) {
                    ConfigurationSection section = YamlConfiguration.loadConfiguration(file.toFile());
                    for (String k : section.getKeys(false)) {
                        subsection = section.getConfigurationSection(k);
                        if (subsection == null) continue;
                        sd = new SkillData(k, subsection);
                        skillMap.put(k, sd);
                        total.getAndIncrement();
                    }
                }
                else logWarning("未知的文件格式: " + fileName);
            });
            logInfo("[loadSkills]: 共加载 技能" + total.get() + "个");
        } catch (IOException e) {
            logWarning("加载Skills文件错误: " + e);
        }
    }
}
