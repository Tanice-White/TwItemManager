package io.github.tanice.twItemManager.manager.item.base.impl;

import io.github.tanice.twItemManager.TwItemManager;
import io.github.tanice.twItemManager.event.entity.EntityAttributeChangeEvent;
import io.github.tanice.twItemManager.event.entity.PlayerDataLimitChangeEvent;
import io.github.tanice.twItemManager.manager.item.base.BaseItem;
import io.github.tanice.twItemManager.manager.player.PlayerData;
import io.github.tanice.twItemManager.pdc.impl.BuffPDC;
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
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static io.github.tanice.twItemManager.constance.key.ConsumableAttributeKey.*;
import static io.github.tanice.twItemManager.infrastructure.PDCAPI.updateUpdateCode;
import static io.github.tanice.twItemManager.util.Logger.logWarning;

/**
 * 可食用物品
 * 由插件代劳，PDC不储存在物品中
 */
public class Consumable extends BaseItem {
    private final Random random;
    /* 食用cd(s) */
    @Getter
    private final int cd;
    /* 食用次数 */
    @Getter
    private final int times;

    private final PlayerData changedPlayerData;
    private final List<BuffPDC> buffs;
    private final List<String> commandLore;
    private final List<PotionEffect> effects;
    private final boolean isLimitChange;

    private final ConsumeSound sound;

    public Consumable(@NotNull String innerName, @NotNull ConfigurationSection cfg) {
        super(innerName, cfg);
        random = new Random();

        cd = cfg.getInt(CD, -1);
        times = cfg.getInt(TIMES, -1);
        changedPlayerData = PlayerData.newFromConsumable(cfg);
        buffs = new ArrayList<>();
        commandLore = cfg.getStringList(COMMAND);
        effects = new ArrayList<>();

        isLimitChange = changedPlayerData.getMaxMana() != 0 || changedPlayerData.getMaxHealth() != 0;

        this.sound = new ConsumeSound(cfg.getString(SOUND));
        this.initBuffs(cfg.getStringList(BUFF));
        this.initEffects(cfg.getStringList(EFFECT));
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

        if (buffs != null && !buffs.isEmpty()) {
            if (TwItemManager.getBuffManager().activateBuffs(player, buffs))
                Bukkit.getPluginManager().callEvent(new EntityAttributeChangeEvent(player));
        }

        if (effects != null && !effects.isEmpty()) player.addPotionEffects(effects);

        if (commandLore != null && !commandLore.isEmpty()) {
            for (String cn : commandLore) {
                if (cn.isEmpty()) continue;
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cn.replace("@self", player.getName()));
            }
        }
        return true;
    }

    /**
     * 发出吃东西的声音
     */
    public void playSound(@NotNull Player player) {
        if (!this.sound.enable) return;
        player.getWorld().playSound(player.getLocation(), this.sound.soundName, this.sound.soundVolume, this.sound.soundPitch);
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

    private void initBuffs(@NotNull Collection<String> lore) {
        String[] v;
        BuffPDC bPDC;
        for (String bn : lore) {
            v = bn.trim().split("\\s+");
            if (v.length > 3) {
                logWarning("Consumable: " + innerName + " 中自定义 buff: " + bn + " 格式错误 ([buff名] [#持续tick] [%概率])");
                continue;
            }

            bPDC = TwItemManager.getBuffManager().getBuffPDC(v[0]);
            if (bPDC == null) {
                logWarning("Consumable: " + innerName + " 中自定义 buff: " + bn + " 错误, 未找到对应 buff 定义");
                continue;
            }
            /* 解析可选参数 */
            int duration;
            double chance;
            for (int i = 1; i < v.length; i++) {
                String param = v[i];
                if (param.startsWith("#")) {
                    duration = Integer.parseInt(param.substring(1));
                    if (duration > 0) bPDC.setDuration(duration);

                } else if (param.startsWith("%")) {
                    chance = Double.parseDouble(param.substring(1));
                    if (chance > 0 && chance <= 100) bPDC.setChance(chance);

                }
            }
            buffs.add(bPDC);
        }
    }

    private void initEffects(@NotNull Collection<String> lore) {
        if (lore.isEmpty()) return;
        String[] v;

        for (String en : lore) {
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
            this.effects.add(
                    new PotionEffect(
                    effectType,
                    Math.max(Integer.parseInt(v[2]), 0),
                    Math.max(Integer.parseInt(v[1]) - 1, 0),
                    true,            // 是否显示粒子效果
                    true,            // 是否显示状态图标
                    false             // 是否有环境音效
                    )
            );
        }
    }


    private static class ConsumeSound {
        boolean enable;
        Sound soundName;
        float soundVolume;
        float soundPitch;

        ConsumeSound(@Nullable String lore) {
            if (lore == null || lore.isEmpty()) {
                enable = false;
                return;
            }
            String[] sound = lore.split(" ");
            if (sound.length != 3) {
                logWarning("可食用物品中不合法的声音配置: " + lore);
                enable = false;
                return;
            }
            String[] k = sound[0].split(":");
            if (k.length == 2) soundName = Registry.SOUNDS.get(new NamespacedKey(k[0], k[1]));
            else soundName = Registry.SOUNDS.get(NamespacedKey.minecraft(sound[0]));
            soundVolume = Float.parseFloat(sound[1]);
            soundPitch = Float.parseFloat(sound[2]);
            enable = true;
        }
    }
}
