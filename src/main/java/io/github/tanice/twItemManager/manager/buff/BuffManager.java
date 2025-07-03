package io.github.tanice.twItemManager.manager.buff;

import io.github.tanice.twItemManager.TwItemManager;
import io.github.tanice.twItemManager.config.Config;
import io.github.tanice.twItemManager.infrastructure.PDCAPI;
import io.github.tanice.twItemManager.manager.item.base.BaseItem;
import io.github.tanice.twItemManager.manager.item.base.impl.Gem;
import io.github.tanice.twItemManager.manager.item.base.impl.Item;
import io.github.tanice.twItemManager.manager.pdc.impl.BuffPDC;
import io.github.tanice.twItemManager.manager.pdc.EntityBuffPDC;
import io.github.tanice.twItemManager.manager.pdc.type.AttributeCalculateSection;
import io.github.tanice.twItemManager.util.SlotUtil;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static io.github.tanice.twItemManager.infrastructure.PDCAPI.*;
import static io.github.tanice.twItemManager.util.Logger.logInfo;
import static io.github.tanice.twItemManager.util.Logger.logWarning;

public class BuffManager {
    private static final int BUFF_RUN_CD = 2;
    private final JavaPlugin plugin;
    /** 全局可用Buff*/
    private final Map<String, BuffPDC> buffMap;
    /** buff记录 */
    @Getter
    private final BuffRecords buffRecords;
    /** 任务处理 */
    private BukkitTask buffTask;
    private BukkitTask buffParticleTask;
    /** 实体状态缓存 */
    private final Map<UUID, CachedEntity> entityCache;

    private static BuffManager instance;
    private final Random random;

    public BuffManager(@NotNull JavaPlugin plugin) {
        instance = this;
        this.plugin = plugin;
        buffMap = new HashMap<>();
        this.loadBuffFilesAndBuffMap();

        random = new Random();

        buffRecords = new BuffRecords();
        entityCache = new ConcurrentHashMap<>();

        buffTask = Bukkit.getScheduler().runTaskTimer(plugin, this::processBuffs, 10L, BUFF_RUN_CD);
        logInfo("BuffManager loaded, Scheduler started");
    }

    public void onReload() {
        onDisable();
        this.loadBuffFilesAndBuffMap();

        buffTask = Bukkit.getScheduler().runTaskTimer(plugin, this::processBuffs, 10L, BUFF_RUN_CD);
        logInfo("BuffManager reloaded, Scheduler restarted");
    }

    public void onDisable() {
        saveAllRecords();
        buffMap.clear();

        // 停止任务
        if (buffTask != null) {
            buffTask.cancel();
            buffTask = null;
        }

        if (buffParticleTask != null) {
            buffParticleTask.cancel();
            buffParticleTask = null;
        }
        // 清空缓存
        buffRecords.records.clear();
        entityCache.clear();
    }

    public @Nullable BuffPDC getBuffPDC(@Nullable String bufInnerName) {
        return buffMap.get(bufInnerName).clone();
    }

    /**
     * 需要延迟添加
     * 玩家登录后从数据库初始化对应的信息
     */
    public void onPlayerJoin(@NotNull Player player) {
        if (!Config.use_mysql) return;
        TwItemManager.getDatabaseManager().loadPlayerBuffRecords(player.getUniqueId().toString())
            .thenAccept(res -> {
                if (res.isEmpty()) return;
                for (BuffRecord r : res) buffRecords.addCacheAndRecord(player, r);
                if (Config.debug) {
                    StringBuilder s = new StringBuilder("玩家 " + player.getName() + "加入，同步buff: ");
                    for (BuffRecord r : res) s.append(r.toString()).append(" ");
                    logInfo(s.toString());
                }
            });
    }

    /**
     * 玩家退出时清理所有Timer Buff任务
     */
    public void onPlayerQuit(@NotNull Player player) {
        if (Config.use_mysql) {
            List<BuffRecord> res = buffRecords.getPlayerBuffs(player.getUniqueId());
            TwItemManager.getDatabaseManager().saveBuffRecords(res);

            if (Config.debug) {
                StringBuilder s = new StringBuilder("玩家 " + player.getName() + " 退出，储存buff: ");
                for (BuffRecord r : res) s.append(r.toString()).append(" ");
                logInfo(s.toString());
            }
        }
        buffRecords.removeAllRecordsForEntity(player.getUniqueId());
        entityCache.remove(player.getUniqueId());
    }

    /**
     * 遍历所有 hold_buff 并更新
     */
    public void updateHoldBuffs(@NotNull LivingEntity e) {
        /* 所有永久 buff 移除 */
        BaseItem bit;
//        if (preItems != null) {
//            for (ItemStack item : preItems) {
//                if (item == null) continue;
//                bit = TwItemManager.getItemManager().getBaseItem(item);
//                if (bit instanceof Item pi) deactivateBuff(e, pi.getHoldBuffs());
//            }
//        }
        deactivateAllHoldBuffs(e);

        EntityEquipment equip = e.getEquipment();
        if (equip == null) return;

        ItemStack it;
        it = equip.getItemInMainHand();
        if (SlotUtil.mainHandJudge(getSlot(it)) && isValid(it)) {
            bit = TwItemManager.getItemManager().getBaseItem(it);
            if (bit instanceof Item i) activeBuff(e, i.getHoldBuffs(), true);
        }

        it = equip.getItemInOffHand();
        if (SlotUtil.offHandJudge(getSlot(it)) && isValid(it)) {
            bit = TwItemManager.getItemManager().getBaseItem(it);
            if (bit instanceof Item i) activeBuff(e, i.getHoldBuffs(), true);
        }

        it = equip.getHelmet();
        if (isValid(it)) {
            bit = TwItemManager.getItemManager().getBaseItem(it);
            if (bit instanceof Item i) activeBuff(e, i.getHoldBuffs(), true);
        }

        it = equip.getChestplate();
        if (isValid(it)) {
            bit = TwItemManager.getItemManager().getBaseItem(it);
            if (bit instanceof Item i) activeBuff(e, i.getHoldBuffs(), true);
        }

        it = equip.getLeggings();
        if (isValid(it)) {
            bit = TwItemManager.getItemManager().getBaseItem(it);
            if (bit instanceof Item i) activeBuff(e, i.getHoldBuffs(), true);
        }

        it = equip.getBoots();
        if (isValid(it)) {
            bit = TwItemManager.getItemManager().getBaseItem(it);
            if (bit instanceof Item i) activeBuff(e, i.getHoldBuffs(), true);
        }
    }

    /**
     * 攻击时上的 buff
     * @param a 攻击方
     * @param b 被攻击方
     */
    public void doAttackBuffs(@NotNull LivingEntity a, @NotNull LivingEntity b) {
        EntityEquipment equip = a.getEquipment();
        if (equip == null) return;

        ItemStack it;
        BaseItem bit;
        it = equip.getItemInMainHand();
        if (SlotUtil.mainHandJudge(getSlot(it)) && isValid(it)) {
            bit = TwItemManager.getItemManager().getBaseItem(it);
            if (bit instanceof Item i) activeBuff(b, i.getHoldBuffs(), false);
        }

        it = equip.getItemInOffHand();
        if (SlotUtil.offHandJudge(getSlot(it)) && isValid(it)) {
            bit = TwItemManager.getItemManager().getBaseItem(it);
            if (bit instanceof Item i) activeBuff(b, i.getHoldBuffs(), false);
        }

        it = equip.getHelmet();
        if (isValid(it)) {
            bit = TwItemManager.getItemManager().getBaseItem(it);
            if (bit instanceof Item i) activeBuff(b, i.getHoldBuffs(), false);
        }

        it = equip.getChestplate();
        if (isValid(it)) {
            bit = TwItemManager.getItemManager().getBaseItem(it);
            if (bit instanceof Item i) activeBuff(b, i.getHoldBuffs(), false);
        }

        it = equip.getLeggings();
        if (isValid(it)) {
            bit = TwItemManager.getItemManager().getBaseItem(it);
            if (bit instanceof Item i) activeBuff(b, i.getHoldBuffs(), false);
        }

        it = equip.getBoots();
        if (isValid(it)) {
            bit = TwItemManager.getItemManager().getBaseItem(it);
            if (bit instanceof Item i) activeBuff(b, i.getHoldBuffs(), false);
        }
    }

    /**
     * 被攻击时上的 buff
     * @param a 攻击方
     * @param b 被攻击方
     */
    public void doDefenceBuffs(@NotNull LivingEntity a, @NotNull LivingEntity b) {
        EntityEquipment equip = b.getEquipment();
        if (equip == null) return;

        ItemStack it;
        BaseItem bit;
        it = equip.getItemInMainHand();
        if (SlotUtil.mainHandJudge(getSlot(it)) && isValid(it)) {
            bit = TwItemManager.getItemManager().getBaseItem(it);
            if (bit instanceof Item i) activeBuff(a, i.getHoldBuffs(), false);
        }

        it = equip.getItemInOffHand();
        if (SlotUtil.offHandJudge(getSlot(it)) && isValid(it)) {
            bit = TwItemManager.getItemManager().getBaseItem(it);
            if (bit instanceof Item i) activeBuff(a, i.getHoldBuffs(), false);
        }

        it = equip.getHelmet();
        if (isValid(it)) {
            bit = TwItemManager.getItemManager().getBaseItem(it);
            if (bit instanceof Item i) activeBuff(a, i.getHoldBuffs(), false);
        }

        it = equip.getChestplate();
        if (isValid(it)) {
            bit = TwItemManager.getItemManager().getBaseItem(it);
            if (bit instanceof Item i) activeBuff(a, i.getHoldBuffs(), false);
        }

        it = equip.getLeggings();
        if (isValid(it)) {
            bit = TwItemManager.getItemManager().getBaseItem(it);
            if (bit instanceof Item i) activeBuff(a, i.getHoldBuffs(), false);
        }

        it = equip.getBoots();
        if (isValid(it)) {
            bit = TwItemManager.getItemManager().getBaseItem(it);
            if (bit instanceof Item i) activeBuff(a, i.getHoldBuffs(), false);
        }
    }

    /**
     * 让 buff 生效
     */
    public void activeBuff(@NotNull LivingEntity e, @NotNull List<String> buffNames, boolean isHoldBuff) {
        if (buffNames.isEmpty()) return;

        BuffPDC bPDC;
        EntityBuffPDC ePDC = PDCAPI.getCalculablePDC(e);
        if (ePDC == null) ePDC = new EntityBuffPDC();

        for (String bn : buffNames) {
            bPDC = getBuffPDC(bn);
            if (bPDC == null) {
                logWarning("buff 名: " + bn + "不存在");
                continue;
            }
            if (!bPDC.isEnable() || (!isHoldBuff && random.nextDouble() < bPDC.getChance())) continue;
            /* 全局计算类属性 */
            if (bPDC.getAttributeCalculateSection() == AttributeCalculateSection.TIMER) {
                if (isHoldBuff) bPDC.setDuration(-1);
                if (Config.debug) logInfo("[activeBuff] " + e.getName() + " addTimerBuff: " + bPDC);
                startTimerBuffTask(e, bPDC);
            /* 非Timer类的都需要计算生效时间 */
            } else {
                /* 持续时间为 负数 则永续 */
                /* 这里的 beginTimeStamp 无效, 永续 buff 会被自动清除，不会写入数据库 */
                if (bPDC.getDuration() < 0 || isHoldBuff) bPDC.setEndTimeStamp(-1);
                else {
                    long curr = System.currentTimeMillis();
                    bPDC.setEndTimeStamp(curr + (50L * bPDC.getDuration()));
                }

                if (Config.debug) logInfo("[activeBuff] " + e.getName() + " addOtherBuff: " + bPDC);
            }
            ePDC.addBuff(bPDC);
        }
        PDCAPI.setCalculablePDC(e, ePDC);
    }

    /**
     * 属性 buff 失效
     */
    public void deactivateBuff(@NotNull LivingEntity e, @Nullable List<String> buffNames) {
        if (buffNames == null) return;

        BuffPDC bPDC;
        EntityBuffPDC ePDC = PDCAPI.getCalculablePDC(e);
        if (ePDC == null) ePDC = new EntityBuffPDC();

        for (String bn : buffNames) {
            bPDC = getBuffPDC(bn);
            if (bPDC == null) continue;
            /* TIMER */
            if (bPDC.getAttributeCalculateSection() == AttributeCalculateSection.TIMER) {
                cancelTimerBuffTask(e, bPDC.getInnerName());
            } else ePDC.removeBuff(bPDC);
        }
        PDCAPI.setCalculablePDC(e, ePDC);
    }

    /**
     * 移除所有永久buff
     */
    public void deactivateAllHoldBuffs(@NotNull LivingEntity e) {
        EntityBuffPDC ePDC = PDCAPI.getCalculablePDC(e);
        if (ePDC == null) ePDC = new EntityBuffPDC();
        for (BuffPDC bPDC : ePDC.getBuffPDCs()) {
            if (!bPDC.isEnable() || bPDC.getDuration() < 0 || bPDC.getEndTimeStamp() < 0) ePDC.removeBuff(bPDC);
            if (bPDC.getAttributeCalculateSection() == AttributeCalculateSection.TIMER) cancelTimerBuffTask(e, bPDC.getInnerName());
        }
    }

    /**
     * 物品不为空且不是宝石
     * TODO 判断耐久度
     */
    private boolean isValid(@Nullable ItemStack item) {
        return item != null && !(TwItemManager.getItemManager().getBaseItem(item) instanceof Gem);
    }

    /**
     * TIMER buff 失效
     */
    public void deactivatePlayerTimerBuff(@NotNull UUID playerId) {
        buffRecords.removeAllRecordsForEntity(playerId);
        if (!buffRecords.hasAnyBuff(playerId)) entityCache.remove(playerId);
    }

    private void saveAllRecords() {
        TwItemManager.getDatabaseManager().saveBuffRecords(buffRecords.records.values());
    }

    private void processBuffs() {
        buffRecords.process();
    }

    /**
     * 启动Timer类型的Buff任务
     */
    private void startTimerBuffTask(@NotNull LivingEntity entity, @NotNull BuffPDC buff) {
        entityCache.computeIfAbsent(entity.getUniqueId(), k -> new CachedEntity(entity));
        buffRecords.addBuff(entity, buff);
    }

    /**
     * 取消Timer类型的Buff任务
     */
    private void cancelTimerBuffTask(@NotNull LivingEntity entity, String buffName) {
        UUID uuid = entity.getUniqueId();
        buffRecords.removeRecord(uuid, buffName);
        boolean hasOtherBuffs = buffRecords.hasAnyBuff(uuid);
        if (!hasOtherBuffs) entityCache.remove(uuid);
    }

    private void loadBuffFilesAndBuffMap(){
        AtomicInteger total = new AtomicInteger();
        Path buffDir = plugin.getDataFolder().toPath().resolve("buff");
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
                        bPDC = new BuffPDC(k, subsection);
                        buffMap.put(k, bPDC);
                        total.getAndIncrement();
                    }
                }
                else if (fileName.endsWith(".js")) {
                    bPDC = new BuffPDC(name, file);
                    buffMap.put(bPDC.getInnerName(), bPDC);
                    total.getAndIncrement();
                }
                else logWarning("未知的文件格式: " + fileName);
            });
            logInfo("[loadBuffs]: 共加载BUFF" + total.get() + "个");
        } catch (IOException e) {
            logWarning("加载BUFF文件错误: " + e);
        }
    }

    // 实体状态缓存
    // 避免 通过uuid获取实体这种复杂操作
    private static class CachedEntity {
        final UUID uuid;
        LivingEntity entity;

        CachedEntity(@NotNull LivingEntity entity) {
            this.uuid = entity.getUniqueId();
            this.entity = entity;
        }
    }

    /**
     * 计时器类buff记录
     */
    private class BuffRecords {
        // key : [uuid]-[buffName]
        final Map<String, BuffRecord> records = new ConcurrentHashMap<>(100);

        public void process() {
            Iterator<Map.Entry<String, BuffRecord>> it = records.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, BuffRecord> entry = it.next();
                BuffRecord record = entry.getValue();
                // 检查CD
                record.cooldownCounter -= BUFF_RUN_CD;
                if (record.cooldownCounter <= 0) {
                    CachedEntity cached = BuffManager.instance.entityCache.get(record.uuid);
                    if (cached == null || cached.entity == null || !cached.entity.isValid() || cached.entity.isDead()) {
                        it.remove();
                        continue;
                    }
                    /* 考虑到从数据库加载的数据，尝试获取 */
                    if (record.buff == null) record.buff = getBuffPDC(record.buffInnerName);
                    if (record.buff != null) {
                        record.buff.execute(cached.entity);
                        record.cooldownCounter = Math.max(1, record.buff.getCd());
                    }
                }
                // 检查持续时间
                /* 最开始就小于0则永续 */
                if (record.durationCounter >= 0) {
                    record.durationCounter -= BUFF_RUN_CD;
                    if (record.durationCounter <= 0) it.remove();
                }
            }
        }

        // 移除指定记录
        public void removeRecord(@NotNull UUID uuid, String buffName) {
            records.remove(createKey(uuid.toString(), buffName));
        }

        public void removeAllRecordsForEntity(@NotNull UUID uuid) {
            records.keySet().removeIf(key -> key.startsWith(uuid + "-"));
        }

        public boolean hasAnyBuff(@NotNull UUID uuid) {
            for (String key : records.keySet()) {
                if (key.startsWith(uuid + "-")) return true;
            }
            return false;
        }

        public @NotNull List<BuffRecord> getPlayerBuffs(@NotNull UUID uuid) {
            List<BuffRecord> playerBuffs = new ArrayList<>();
            for (Map.Entry<String, BuffRecord> entry : records.entrySet()) {
                if (entry.getKey().startsWith(uuid.toString())) playerBuffs.add(entry.getValue());
            }
            return playerBuffs;
        }

        public void addBuff(@NotNull LivingEntity entity, @NotNull BuffPDC buff) {
            String key = createKey(entity.getUniqueId().toString(), buff.getInnerName());
            BuffRecord nbr = new BuffRecord(entity, buff);
            BuffRecord br = records.get(key);
            /* 新buff为永续 */
            if (br == null || nbr.durationCounter < 0) records.put(key, nbr);
            /* 非永续 */
            else {
                nbr.durationCounter = Math.max(br.durationCounter, nbr.durationCounter);
                records.put(key, nbr);
            }
        }

        /* 添加从数据库读取的玩家buff */
        public void addCacheAndRecord(@NotNull Player player, @NotNull BuffRecord dbBr) {
            String key = createKey(player.getUniqueId().toString(), dbBr.buffInnerName);
            /* 玩家身上现有的为新的 */
            BuffRecord obr = records.get(key);

            if (obr == null) records.put(key, dbBr);
            /* 数据库中的buff永续 */
            else if (dbBr.durationCounter < 0 || obr.durationCounter < 0) {
                obr.durationCounter = dbBr.durationCounter;
                records.put(key, obr);
            } else {
                obr.durationCounter = Math.max(obr.durationCounter, dbBr.durationCounter);
                records.put(key, obr);
            }
            entityCache.put(player.getUniqueId(), new CachedEntity(player));
        }

        // 创建唯一键
        @Contract(pure = true)
        private @NotNull String createKey(@NotNull String uuid, String buffName) {
            return uuid + "-" + buffName;
        }
    }
}
