package io.github.tanice.twItemManager.manager.buff;

import io.github.tanice.twItemManager.TwItemManager;
import io.github.tanice.twItemManager.manager.pdc.impl.BuffPDC;
import io.github.tanice.twItemManager.manager.pdc.impl.EntityPDC;
import io.github.tanice.twItemManager.manager.pdc.type.AttributeCalculateSection;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static io.github.tanice.twItemManager.infrastructure.PDCAPI.getEntityCalculablePDC;
import static io.github.tanice.twItemManager.infrastructure.PDCAPI.setEntityCalculablePDC;
import static io.github.tanice.twItemManager.util.Logger.logWarning;

public class BuffManager {
    private final JavaPlugin plugin;
    /** 全局可用Buff*/
    private final Map<String, BuffPDC> buffMap;

    public BuffManager(@NotNull JavaPlugin plugin) {
        this.plugin = plugin;
        buffMap = new HashMap<>();
        this.loadBuffFilesAndBuffMap();
    }

    public void onReload() {
        buffMap.clear();
        this.loadBuffFilesAndBuffMap();
    }

    public @Nullable BuffPDC getBuffPDC(@Nullable String bufInnerName) {
        return buffMap.get(bufInnerName);
    }

    /**
     * 让 buff 生效
     */
    public void activeBuff(@NotNull Player p, @NotNull List<String> buffNames) {
        BuffPDC bPDC;
        EntityPDC ePDC;
        int tick = 20;
        for (String bn : buffNames) {
            ePDC = getEntityCalculablePDC(p);
            if (ePDC == null) ePDC = new EntityPDC();
            bPDC = TwItemManager.getBuffManager().getBuffPDC(bn);
            if (bPDC == null) continue;

            /* 全局计算类属性 */
            if (bPDC.getAttributeCalculateSection() == AttributeCalculateSection.TIMER) {
                // TODO 用线程池创建任务，执行run方法
                logWarning("执行js任务");

                /* 非Timer类的都需要计算生效时间 */
            } else {
                tick = bPDC.getDuration() < 0 ? tick : bPDC.getDuration();
                bPDC.setEndTimeStamp(System.currentTimeMillis() + (long) 50 * tick);
                ePDC.addBuff(bPDC);
            }
            setEntityCalculablePDC(p, ePDC);

        }
    }

    /**
     * 让buff失效
     */
    public void deactivateBuff(@NotNull Player p, @NotNull List<String> buffNames) {
        BuffPDC bPDC;
        EntityPDC ePDC;
        for (String bn : buffNames) {
            ePDC = getEntityCalculablePDC(p);
            if (ePDC == null) ePDC = new EntityPDC();
            bPDC = TwItemManager.getBuffManager().getBuffPDC(bn);
            if (bPDC == null) continue;
            /* 全局计算类属性 */
            if (bPDC.getAttributeCalculateSection() == AttributeCalculateSection.TIMER) {
                // TODO 用线程池创建任务，执行run方法
                logWarning("终止js任务");
            } else ePDC.removeBuff(bPDC);
            /* 设置回去 */
            setEntityCalculablePDC(p, ePDC);
        }
    }

    private void loadBuffFilesAndBuffMap(){
        Path buffDir = plugin.getDataFolder().toPath().resolve("buff"); ;
        if (!Files.exists(buffDir) || !Files.isDirectory(buffDir)) return;
        try (Stream<Path> files = Files.list(buffDir)) {
            files.forEach(file -> {
                String fileName = file.getFileName().toString();
                String name = fileName.substring(0, fileName.lastIndexOf('.'));
                BuffPDC bPDC;
                List<BuffPDC> bPDCList = new ArrayList<>();
                ConfigurationSection subsection;
                if (fileName.endsWith(".yml")) {
                    ConfigurationSection section = YamlConfiguration.loadConfiguration(file.toFile());
                    for (String k : section.getKeys(false)) {
                        subsection = section.getConfigurationSection(k);
                        if (subsection == null) continue;
                        bPDC = new BuffPDC(k, section);
                        buffMap.put(k, bPDC);
                    }
                }
                else if (fileName.endsWith(".js")) {
                    bPDC = new BuffPDC(name, file);
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
