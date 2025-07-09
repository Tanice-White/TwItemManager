package io.github.tanice.twItemManager.manager.database;

import io.github.tanice.twItemManager.config.Config;
import io.github.tanice.twItemManager.manager.buff.BuffRecord;
import io.github.tanice.twItemManager.manager.pdc.EntityPDC;
import io.github.tanice.twItemManager.manager.player.PlayerData;
import io.github.tanice.twItemManager.util.serialize.OriSerializationUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.*;
import java.util.*;
import java.util.concurrent.*;

import static io.github.tanice.twItemManager.util.Logger.logInfo;
import static io.github.tanice.twItemManager.util.Logger.logWarning;

/**
 * 数据库管理
 */
public class DatabaseManager {
    private Connection connection;
    private ExecutorService dbExecutor;

    public DatabaseManager() {
        onEnable();
    }

    public void onEnable(){
        if (!Config.use_mysql) {
            logInfo("不使用数据库同步数据");
            return;
        }

        String url = "jdbc:mysql://" + Config.host + ":" + Config.port + "/" + Config.database_name;
        Properties properties = new Properties();
        properties.setProperty("user", Config.username);
        properties.setProperty("password", Config.password);
        properties.setProperty("useSSL", "false");
        properties.setProperty("autoReconnect", "true");
        try {
            connection = DriverManager.getConnection(url, properties);
            dbExecutor = Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r);
                t.setName("DatabaseManager-Async-Thread");
                t.setDaemon(true);
                return t;
            });
        } catch (SQLException e) {
            logWarning("数据库连接失败! 关闭数据库同步");
            logWarning(e.getMessage());
            Config.use_mysql = false;
            return;
        }
        if (createTables()) logInfo("数据库连接成功");
        else {
            logWarning("数据表创建失败，关闭数据库同步");
            Config.use_mysql = false;
        }
    }

    public void onReload() {
        logInfo("DatabaseManager reloading...");
        onDisable();
        onEnable();
    }

    public void onDisable() {
        if (dbExecutor != null) {
            dbExecutor.shutdown();
            try {
                if (!dbExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    dbExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                dbExecutor.shutdownNow();
                logWarning("数据库线程池关闭出现问题，可能导致部分数据写入失败");
            } finally {
                dbExecutor = null;
            }
        }

        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                logInfo("数据库连接关闭成功");
            }
        } catch (SQLException e) {
            logWarning("数据库关闭失败: " + e.getMessage());
        } finally {
            connection = null;
        }
    }

    // ==================== BuffRecord 相关方法 ====================
    /**
     * 保存BuffRecord到数据库
     * @param record BuffRecord对象
     */
    public void saveBuffRecord(@NotNull BuffRecord record) {
        dbExecutor.execute(() -> {
            String sql = "INSERT INTO buff_records (uuid, buff_inner_name, cooldown_counter, duration_counter) "
                    + "VALUES (?, ?, ?, ?) "
                    + "ON DUPLICATE KEY UPDATE "
                    + "cooldown_counter = VALUES(cooldown_counter), "
                    + "duration_counter = VALUES(duration_counter)";
            try (PreparedStatement pst = connection.prepareStatement(sql)) {
                pst.setString(1, record.getUuid().toString());
                pst.setString(2, record.getBuff().getInnerName());
                pst.setInt(3, record.getCooldownCounter());
                pst.setInt(4, record.getDurationCounter());
                pst.executeUpdate();
            } catch (SQLException e) {
                logWarning("保存单个 buff 记录出错: " + e.getMessage());
            }
        });
    }

    /**
     * 批量保存 BuffRecords 是否为 holdbuff 不重要，玩家进入后需要重新加载的
     * @param records BuffRecord集合
     */
    public void saveBuffRecords(@NotNull Collection<BuffRecord> records){
        if (records.isEmpty()) return;
        dbExecutor.execute(() -> {
            String sql = "INSERT INTO buff_records (uuid, buff_inner_name, cooldown_counter, duration_counter) "
                    + "VALUES (?, ?, ?, ?) "
                    + "ON DUPLICATE KEY UPDATE "
                    + "cooldown_counter = VALUES(cooldown_counter), "
                    + "duration_counter = VALUES(duration_counter)";
            try (PreparedStatement pst = connection.prepareStatement(sql)) {
                for (BuffRecord record : records) {
                    pst.setString(1, record.getUuid().toString());
                    pst.setString(2, record.getBuff().getInnerName());
                    pst.setInt(3, record.getCooldownCounter());
                    pst.setInt(4, record.getDurationCounter());
                    pst.addBatch();
                }
                pst.executeBatch();
            } catch (SQLException e) {
                logWarning("批量保存 buff 记录出错: " + e.getMessage());
            }
        });
    }

    /**
     * 获取实体的所有BuffRecords并删除数据库的记录
     * @param uuid 实体UUID
     * @return BuffRecord映射表
     */
    public CompletableFuture<List<BuffRecord>> loadPlayerBuffRecords(@NotNull String uuid) {
        CompletableFuture<List<BuffRecord>> future = new CompletableFuture<>();
        dbExecutor.execute(() -> {
            List<BuffRecord> res = new ArrayList<>();
            String selectSql = "SELECT buff_inner_name, cooldown_counter, duration_counter FROM buff_records WHERE uuid = ?";
            try (PreparedStatement pst = connection.prepareStatement(selectSql)) {
                pst.setString(1, uuid);
                try (ResultSet rs = pst.executeQuery()) {
                    while (rs.next()) {
                        String buffInnerName = rs.getString("buff_inner_name");
                        int cooldown = rs.getInt("cooldown_counter");
                        int duration = rs.getInt("duration_counter");
                        res.add(new BuffRecord(uuid, buffInnerName, cooldown, duration));
                    }
                }

                /* 由于写入的时候可能为空，则在读取的时候进行删除 */
                String deleteSql = "DELETE FROM buff_records WHERE uuid = ?";
                try (PreparedStatement deletePst = connection.prepareStatement(deleteSql)) {
                    deletePst.setString(1, uuid);
                    deletePst.executeUpdate();
                }

                future.complete(res);
            } catch (SQLException e) {
                logWarning("加载玩家 " + uuid + " 的buff出错: " + e.getMessage());
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    // ==================== EntityPDC 序列化相关方法 ====================
    /**
     * 保存EntityPDC到数据库（序列化为byte数组）
     * @param uuid 实体UUID
     * @param entityPDC EntityPDC对象
     */
    public void saveEntityPDC(@NotNull String uuid, @Nullable EntityPDC entityPDC) {
        final EntityPDC finalEntityPDC = (entityPDC == null) ? new EntityPDC() : entityPDC;
        dbExecutor.execute(() -> {
            byte[] data = OriSerializationUtil.serialize(finalEntityPDC);
            String sql = "INSERT INTO entity_pdc_data (uuid, data) VALUES (?, ?) ON DUPLICATE KEY UPDATE data = VALUES(data)";
            try (PreparedStatement pst = connection.prepareStatement(sql)) {
                pst.setString(1, uuid);
                pst.setBytes(2, data);
                pst.executeUpdate();
            } catch (SQLException e) {
                logWarning("保存玩家 " + uuid + " 的EntityPDC出错: " + e.getMessage());
            }
        });
    }

    /**
     * 获取EntityPDC对象（从byte数组反序列化）
     * @param uuid 实体UUID
     * @return EntityPDC对象，不存在则返回null
     */
    public CompletableFuture<EntityPDC> loadEntityPDC(@NotNull String uuid) {
        CompletableFuture<EntityPDC> future = new CompletableFuture<>();
        dbExecutor.execute(() -> {
            String sql = "SELECT data FROM entity_pdc_data WHERE uuid = ?";
            try (PreparedStatement pst = connection.prepareStatement(sql)) {
                pst.setString(1, uuid);
                try (ResultSet rs = pst.executeQuery()) {
                    if (rs.next()) {
                        byte[] data = rs.getBytes("data");
                        future.complete((EntityPDC) OriSerializationUtil.deserialize(data));
                        return;
                    }
                }
            } catch (SQLException e) {
                logWarning("从数据库获取玩家 " + uuid + " 的EntityPDC出错: " + e.getMessage());
                future.completeExceptionally(null);
            }
            future.complete(null);
        });
        return future;
    }

    // ==================== PlayerData 相关操作 ====================
    /**
     * 保存PlayerData到数据库
     * @param playerData PlayerData对象
     */
    public void savePlayerData(@NotNull PlayerData playerData) {
        dbExecutor.execute(() -> {
            String sql = "INSERT INTO player_data (" +
                    "uuid, health, max_health, mana, max_mana, " +
                    "food, saturation, level, allow_flight) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE " +
                    "health = VALUES(health), max_health = VALUES(max_health), " +
                    "mana = VALUES(mana), max_mana = VALUES(max_mana), " +
                    "food = VALUES(food), saturation = VALUES(saturation), " +
                    "level = VALUES(level), allow_flight = VALUES(allow_flight)";
            try (PreparedStatement pst = connection.prepareStatement(sql)) {
                pst.setString(1, playerData.getUuid());
                pst.setDouble(2, playerData.getHealth());
                pst.setDouble(3, playerData.getMaxHealth());
                pst.setDouble(4, playerData.getMana());
                pst.setDouble(5, playerData.getMaxMana());
                pst.setDouble(6, playerData.getFood());
                pst.setDouble(7, playerData.getSaturation());
                pst.setDouble(8, playerData.getLevel());
                pst.setBoolean(9, playerData.isAllowFlight());
                pst.executeUpdate();
            } catch (SQLException e) {
                logWarning("保存玩家数据出错: " + e.getMessage());
            }
        });
    }

    /**
     * 从数据库加载PlayerData
     * @param uuid 玩家UUID
     * @return PlayerData对象，不存在则返回null
     */
    public CompletableFuture<PlayerData> loadPlayerData(@NotNull String uuid) {
        CompletableFuture<PlayerData> future = new CompletableFuture<>();
        dbExecutor.execute(() -> {
            String sql = "SELECT health, max_health, mana, max_mana, " +
                    "food, saturation, level, allow_flight " +
                    "FROM player_data WHERE uuid = ?";
            try (PreparedStatement pst = connection.prepareStatement(sql)) {
                pst.setString(1, uuid);
                try (ResultSet rs = pst.executeQuery()) {
                    if (rs.next()) {
                        double health = rs.getDouble("health");
                        double maxHealth = rs.getDouble("max_health");
                        double mana = rs.getDouble("mana");
                        double maxMana = rs.getDouble("max_mana");
                        int food = rs.getInt("food");
                        double saturation = rs.getDouble("saturation");
                        int level = rs.getInt("level");
                        boolean allowFlight = rs.getBoolean("allow_flight");
                        future.complete(new PlayerData(uuid, food, (float) saturation, level,
                                health, maxHealth, allowFlight, mana, maxMana));
                        return;
                    }
                }
            } catch (SQLException e) {
                logWarning("加载玩家 " + uuid + " 的数据出错: " + e.getMessage());
                future.completeExceptionally(e);
            }
            future.complete(null);
        });
        return future;
    }

    private boolean createTables(){
        String buffRecordsSql = "CREATE TABLE IF NOT EXISTS buff_records ("
                + "uuid VARCHAR(50) NOT NULL, "
                + "buff_inner_name VARCHAR(50) NOT NULL, "
                + "cooldown_counter INT NOT NULL, "
                + "duration_counter INT NOT NULL, "
                + "PRIMARY KEY (uuid, buff_inner_name))";

        String entityPdcTableSql = "CREATE TABLE IF NOT EXISTS entity_pdc_data ("
                + "uuid VARCHAR(50) PRIMARY KEY, "
                + "data LONGBLOB NOT NULL)";

        String playerDataTableSql = "CREATE TABLE IF NOT EXISTS player_data ("
                + "uuid VARCHAR(50) PRIMARY KEY, "
                + "health DOUBLE NOT NULL, "
                + "max_health DOUBLE NOT NULL, "
                + "mana DOUBLE NOT NULL, "
                + "max_mana DOUBLE NOT NULL, "
                + "food DOUBLE NOT NULL, "
                + "saturation DOUBLE NOT NULL, "
                + "level DOUBLE NOT NULL, "
                + "allow_flight BOOLEAN NOT NULL)";

        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate(buffRecordsSql);
            stmt.executeUpdate(entityPdcTableSql);
            stmt.executeUpdate(playerDataTableSql);
        } catch (SQLException e) {
            logWarning(e.getMessage());
            return false;
        }
        return true;
    }
}
