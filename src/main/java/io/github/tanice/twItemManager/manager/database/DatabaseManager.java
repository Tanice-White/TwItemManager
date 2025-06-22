package io.github.tanice.twItemManager.manager.database;

import io.github.tanice.twItemManager.config.Config;
import io.github.tanice.twItemManager.manager.buff.BuffRecord;
import io.github.tanice.twItemManager.manager.pdc.impl.EntityPDC;
import io.github.tanice.twItemManager.util.serialize.OriSerializationUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.*;
import java.util.*;

import static io.github.tanice.twItemManager.util.Logger.logInfo;
import static io.github.tanice.twItemManager.util.Logger.logWarning;

/**
 * 数据库管理
 */
public class DatabaseManager {
    private static Connection connection;

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
    }

    /**
     * 批量保存BuffRecords
     * @param records BuffRecord集合
     */
    public void saveBuffRecords(@NotNull Collection<BuffRecord> records){
        if (records.isEmpty()) return;

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
    }

    /**
     * 获取实体的所有BuffRecords并删除数据库的记录
     * @param uuid 实体UUID
     * @return BuffRecord映射表
     */
    @NotNull
    public List<BuffRecord> loadPlayerBuffRecords(@NotNull String uuid) {
        List<BuffRecord> res = new ArrayList<>();

        String selectSql = "SELECT buff_inner_name, cooldown_counter, duration_counter "
                + "FROM buff_records WHERE uuid = ?";

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

            if (!res.isEmpty()) { // 只有在查询到记录时才执行删除
                String deleteSql = "DELETE FROM buff_records WHERE uuid = ?";
                try (PreparedStatement deletePst = connection.prepareStatement(deleteSql)) {
                    deletePst.setString(1, uuid);
                    deletePst.executeUpdate();
                }
            }
        } catch (SQLException e) {
            logWarning("加载玩家 " + uuid + " 的buff出错: " + e.getMessage());
        }
        return res;
    }

    // ==================== EntityPDC 序列化相关方法 ====================
    /**
     * 保存EntityPDC到数据库（序列化为byte数组）
     * @param uuid 实体UUID
     * @param entityPDC EntityPDC对象
     */
    public void saveEntityPDC(@NotNull String uuid, @Nullable EntityPDC entityPDC) {
        if (entityPDC == null) entityPDC = new EntityPDC();
        entityPDC.simplify(System.currentTimeMillis());

        byte[] data = OriSerializationUtil.serialize(entityPDC);

        String sql = "INSERT INTO entity_pdc_data (uuid, data) VALUES (?, ?) "
                + "ON DUPLICATE KEY UPDATE data = VALUES(data)";

        try (PreparedStatement pst = connection.prepareStatement(sql)) {
            pst.setString(1, uuid);
            pst.setBytes(2, data);
            pst.executeUpdate();
        } catch (SQLException e) {
            logWarning("保存玩家 " + uuid + " 的EntityPDC出错: " + e.getMessage());
        }
    }

    /**
     * 获取EntityPDC对象（从byte数组反序列化）
     * @param uuid 实体UUID
     * @return EntityPDC对象，不存在则返回null
     */
    @Nullable
    public EntityPDC getEntityPDC(@NotNull String uuid) {
        // 从数据库加载
        String sql = "SELECT data FROM entity_pdc_data WHERE uuid = ?";

        try (PreparedStatement pst = connection.prepareStatement(sql)) {
            pst.setString(1, uuid);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    byte[] data = rs.getBytes("data");
                    return (EntityPDC) OriSerializationUtil.deserialize(data);
                }
            }
        } catch (SQLException e) {
            logWarning("从数据库获取玩家 " + uuid + " 的EntityPDC出错: " + e.getMessage());
        }
        return null;
    }

    private boolean createTables(){
        String sql = "CREATE TABLE IF NOT EXISTS buff_records ("
                + "uuid VARCHAR(50) NOT NULL, "
                + "buff_inner_name VARCHAR(50) NOT NULL, "
                + "cooldown_counter INT NOT NULL, "
                + "duration_counter INT NOT NULL, "
                + "PRIMARY KEY (uuid, buff_inner_name))";

        // 创建EntityPDC序列化数据表
        String entityPdcTableSql = "CREATE TABLE IF NOT EXISTS entity_pdc_data ("
                + "uuid CHAR(50) PRIMARY KEY, "
                + "data LONGBLOB NOT NULL)";

        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate(sql);
            stmt.executeUpdate(entityPdcTableSql);
        } catch (SQLException e) {
            logWarning(e.getMessage());
            return false;
        }
        return true;
    }
}
