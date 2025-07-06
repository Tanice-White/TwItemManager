package io.github.tanice.twItemManager.manager.item.base.impl;

import io.github.tanice.twItemManager.TwItemManager;
import io.github.tanice.twItemManager.event.PlayerDataLimitChangeEvent;
import io.github.tanice.twItemManager.manager.item.base.BaseItem;
import io.github.tanice.twItemManager.manager.player.PlayerData;
import io.github.tanice.twItemManager.util.MiniMessageUtil;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.github.tanice.twItemManager.constance.key.ConsumableAttributeKey.*;
import static io.github.tanice.twItemManager.infrastructure.PDCAPI.updateUpdateCode;
import static io.github.tanice.twItemManager.util.Logger.logWarning;

/**
 * 可食用物品
 * 由插件代劳，PDC不储存在物品中
 */
public class Consumable extends BaseItem {
    /* 食用cd(s) */
    @Getter
    private final int cd;
    /* 食用次数 */
    @Getter
    private final int times;

    private final PlayerData changedPlayerData;
    private final List<String> buffLore;
    private final List<String> commandLore;
    private final List<String> effectLore;
    private final boolean isLimitChange;

    private Sound soundName;
    private float soundVolume;
    private float soundPitch;

    public Consumable(@NotNull String innerName, @NotNull ConfigurationSection cfg) {
        super(innerName, cfg);
        cd = cfg.getInt(CD, -1);
        times = cfg.getInt(TIMES, -1);
        changedPlayerData = PlayerData.newFromConsumable(cfg);
        buffLore = cfg.getStringList(BUFF);
        commandLore = cfg.getStringList(COMMAND);
        effectLore = cfg.getStringList(EFFECT);

        isLimitChange = changedPlayerData.getMaxMana() != 0 || changedPlayerData.getMaxHealth() != 0;

        String v = cfg.getString(SOUND);
        if (v != null) {
            String[] sound = v.split(" ");
            if (sound.length != 3) {
                logWarning("可食用物品中不合法的声音配置: " + v);
                return;
            }
            String[] k = sound[0].split(":");
            if (k.length == 2) soundName = Registry.SOUNDS.get(new NamespacedKey(k[0], k[1]));
            else soundName = Registry.SOUNDS.get(NamespacedKey.minecraft(sound[0]));
            soundVolume = Float.parseFloat(sound[1]);
            soundPitch = Float.parseFloat(sound[2]);
        }
    }

    @Override
    protected void loadBase(@NotNull ItemMeta meta) {
        meta.displayName(MiniMessageUtil.serialize(displayName));
    }

    @Override
    protected void loadPDCs(@NotNull ItemMeta meta) {
        updateUpdateCode(meta);
    }

    /**
     * 属性生效
     * 返回是否生效成功
     */
    public boolean activate(@NotNull Player player) {
        PlayerData playerData = PlayerData.readPlayerData(player);
        if (isLimitChange) {
            PlayerDataLimitChangeEvent playerDataLimitChangeEvent = new PlayerDataLimitChangeEvent(player);
            Bukkit.getPluginManager().callEvent(playerDataLimitChangeEvent);
            if (playerDataLimitChangeEvent.isCancelled()) return false;
        }
        playerData.mergeAndActive(changedPlayerData);

        String[] v;
        if (buffLore != null && !buffLore.isEmpty()) {
            for (String bn : buffLore) {
                v = bn.split(" ");
                if (v.length != 2) {
                    logWarning("Consumable: " + innerName + " 中自定义 buff: " + bn + " 格式错误 ([buff名][空格][持续tick])");
                    continue;
                }
                TwItemManager.getBuffManager().activeBuffWithTimeLimit(player, v[0], Integer.parseInt(v[1]));
            }
        }

        if (effectLore != null && !effectLore.isEmpty()) {
            for (String en : effectLore) {
                v = en.split(" ");
                if (v.length != 3) {
                    logWarning("Consumable: " + innerName + " 中原版药水效果: " + en + " 格式错误 ([效果名][空格][药水效果等级][持续tick])");
                    continue;
                }
                PotionEffectType effectType = Registry.EFFECT.get(new NamespacedKey(NamespacedKey.MINECRAFT_NAMESPACE, v[0].toLowerCase()));
                if (effectType == null) {
                    logWarning("原版例子效果名称无法识别: " + v[0]);
                    continue;
                }

                PotionEffect effect = new PotionEffect(
                        effectType,
                        Math.max(Integer.parseInt(v[2]), 0),
                        Math.max(Integer.parseInt(v[1]) - 1, 0),
                        true,            // 是否显示粒子效果
                        true,            // 是否显示状态图标
                        false             // 是否有环境音效
                );
                player.addPotionEffect(effect);
            }
        }

        if (commandLore != null && !commandLore.isEmpty()) {
            for (String cn : commandLore) {
                if (cn.isEmpty()) continue;
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cn.replace(" self ", " " + player.getName() + " "));
            }
        }
        return true;
    }

    /**
     * 发出吃东西的声音
     */
    public void playSound(@NotNull Player player) {
        if (soundName == null) return;
        player.getWorld().playSound(player.getLocation(), soundName, soundVolume, soundPitch);
    }

    /**
     * 可变属性转为MAP
     */
    public @NotNull Map<String, Double> getContentLore() {
        Map<String, Double> contentMap = new HashMap<>();
        // PlayerData
        contentMap.put(HEALTH.toLowerCase(), changedPlayerData.getHealth());
        contentMap.put(MAX_HEALTH.toLowerCase(), changedPlayerData.getMaxHealth());
        contentMap.put(MANA.toLowerCase(), changedPlayerData.getMana());
        contentMap.put(MAX_MANA.toLowerCase(), changedPlayerData.getMaxMana());
        contentMap.put(FOOD.toLowerCase(), (double) changedPlayerData.getFood());
        contentMap.put(SATURATION.toLowerCase(), (double) changedPlayerData.getSaturation());
        contentMap.put(LEVEL.toLowerCase(), (double) changedPlayerData.getLevel());

        contentMap.put(CD.toLowerCase(), (double) cd);
        contentMap.put(TIMES.toLowerCase(), (double) times);

        return contentMap;
    }
}
