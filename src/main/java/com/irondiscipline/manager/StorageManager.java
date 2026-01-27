package com.irondiscipline.manager;

import com.irondiscipline.IronDiscipline;
import com.irondiscipline.model.KillLog;
import com.irondiscipline.manager.WarningManager.Warning;
import org.bukkit.Bukkit;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

/**
 * データベースストレージマネージャー
 * H2/SQLiteを使用した非同期データ永続化
 */
public class StorageManager {

    private final IronDiscipline plugin;
    private Connection connection;
    private final String dbType;

    public StorageManager(IronDiscipline plugin) {
        this.plugin = plugin;
        this.dbType = plugin.getConfigManager().getDatabaseType();
        initializeDatabase();
    }

    private void initializeDatabase() {
        try {
            if ("mysql".equalsIgnoreCase(dbType)) {
                initMySQL();
            } else {
                initH2();
            }
            createTables();
            plugin.getLogger().info("データベース接続成功: " + dbType.toUpperCase());
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "データベース接続失敗", e);
        }
    }

    private void initH2() throws SQLException {
        // Shadeプラグインでリロケートされたドライバーを明示的にロード
        try {
            Class.forName("com.irondiscipline.lib.h2.Driver");
        } catch (ClassNotFoundException e) {
            // リロケートされていない場合は通常のドライバーを試す
            try {
                Class.forName("org.h2.Driver");
            } catch (ClassNotFoundException e2) {
                throw new SQLException("H2ドライバーが見つかりません", e2);
            }
        }

        // データフォルダを作成
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        File dbFile = new File(plugin.getDataFolder(), "irondiscipline");
        String url = "jdbc:h2:" + dbFile.getAbsolutePath() + ";MODE=MySQL";
        connection = DriverManager.getConnection(url, "sa", "");
    }

    private void initMySQL() throws SQLException {
        ConfigManager config = plugin.getConfigManager();
        String url = String.format("jdbc:mysql://%s:%d/%s?useSSL=false&autoReconnect=true",
                config.getMySQLHost(),
                config.getMySQLPort(),
                config.getMySQLDatabase());
        connection = DriverManager.getConnection(url, config.getMySQLUsername(), config.getMySQLPassword());
    }

    private void createTables() throws SQLException {
        // Kill logs table
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("""
                        CREATE TABLE IF NOT EXISTS kill_logs (
                            id BIGINT AUTO_INCREMENT PRIMARY KEY,
                            timestamp BIGINT NOT NULL,
                            killer_id VARCHAR(36),
                            killer_name VARCHAR(32),
                            victim_id VARCHAR(36) NOT NULL,
                            victim_name VARCHAR(32) NOT NULL,
                            weapon VARCHAR(64),
                            distance DOUBLE,
                            world VARCHAR(64),
                            x DOUBLE,
                            y DOUBLE,
                            z DOUBLE
                        )
                    """);

            // Jailed players table
            stmt.execute("""
                        CREATE TABLE IF NOT EXISTS jailed_players (
                            player_id VARCHAR(36) PRIMARY KEY,
                            player_name VARCHAR(32) NOT NULL,
                            reason TEXT,
                            jailed_at BIGINT NOT NULL,
                            jailed_by VARCHAR(36),
                            original_location TEXT,
                            inventory_backup LONGTEXT,
                            armor_backup LONGTEXT
                        )
                    """);

            // Migration: Add columns if they don't exist (for existing databases)
            try {
                stmt.execute("ALTER TABLE jailed_players ADD COLUMN inventory_backup LONGTEXT");
            } catch (SQLException ignored) {
                // Column likely already exists
            }
            try {
                stmt.execute("ALTER TABLE jailed_players ADD COLUMN armor_backup LONGTEXT");
            } catch (SQLException ignored) {
                // Column likely already exists
            }

            // Create indexes
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_kill_logs_timestamp ON kill_logs(timestamp)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_kill_logs_killer ON kill_logs(killer_id)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_kill_logs_victim ON kill_logs(victim_id)");

            // Warnings table
            stmt.execute("""
                        CREATE TABLE IF NOT EXISTS warnings (
                            id BIGINT AUTO_INCREMENT PRIMARY KEY,
                            player_id VARCHAR(36) NOT NULL,
                            player_name VARCHAR(32),
                            reason TEXT,
                            warned_by VARCHAR(36),
                            timestamp BIGINT NOT NULL
                        )
                    """);
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_warnings_player_id ON warnings(player_id)");
        }
    }

    /**
     * 戦闘ログを非同期で保存
     */
    public CompletableFuture<Void> saveKillLogAsync(KillLog log) {
        return CompletableFuture.runAsync(() -> {
            try {
                saveKillLog(log);
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "戦闘ログ保存失敗", e);
            }
        });
    }

    private void saveKillLog(KillLog log) throws SQLException {
        String sql = """
                    INSERT INTO kill_logs (timestamp, killer_id, killer_name, victim_id, victim_name,
                                           weapon, distance, world, x, y, z)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, log.getTimestamp());
            ps.setString(2, log.getKillerId() != null ? log.getKillerId().toString() : null);
            ps.setString(3, log.getKillerName());
            ps.setString(4, log.getVictimId().toString());
            ps.setString(5, log.getVictimName());
            ps.setString(6, log.getWeapon());
            ps.setDouble(7, log.getDistance());
            ps.setString(8, log.getWorld());
            ps.setDouble(9, log.getX());
            ps.setDouble(10, log.getY());
            ps.setDouble(11, log.getZ());
            ps.executeUpdate();
        }
    }

    /**
     * 戦闘ログを非同期で取得
     */
    public CompletableFuture<List<KillLog>> getKillLogsAsync(UUID playerId, int limit) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return getKillLogs(playerId, limit);
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "戦闘ログ取得失敗", e);
                return new ArrayList<>();
            }
        });
    }

    private List<KillLog> getKillLogs(UUID playerId, int limit) throws SQLException {
        List<KillLog> logs = new ArrayList<>();
        String sql = """
                    SELECT * FROM kill_logs
                    WHERE killer_id = ? OR victim_id = ?
                    ORDER BY timestamp DESC
                    LIMIT ?
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            String id = playerId.toString();
            ps.setString(1, id);
            ps.setString(2, id);
            ps.setInt(3, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    logs.add(parseKillLog(rs));
                }
            }
        }
        return logs;
    }

    /**
     * 全戦闘ログを非同期で取得
     */
    public CompletableFuture<List<KillLog>> getAllKillLogsAsync(int limit) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return getAllKillLogs(limit);
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "戦闘ログ取得失敗", e);
                return new ArrayList<>();
            }
        });
    }

    private List<KillLog> getAllKillLogs(int limit) throws SQLException {
        List<KillLog> logs = new ArrayList<>();
        String sql = "SELECT * FROM kill_logs ORDER BY timestamp DESC LIMIT ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    logs.add(parseKillLog(rs));
                }
            }
        }
        return logs;
    }

    private KillLog parseKillLog(ResultSet rs) throws SQLException {
        return KillLog.builder()
                .id(rs.getLong("id"))
                .timestamp(rs.getLong("timestamp"))
                .killer(
                        rs.getString("killer_id") != null ? UUID.fromString(rs.getString("killer_id")) : null,
                        rs.getString("killer_name"))
                .victim(UUID.fromString(rs.getString("victim_id")), rs.getString("victim_name"))
                .weapon(rs.getString("weapon"))
                .distance(rs.getDouble("distance"))
                .location(rs.getString("world"), rs.getDouble("x"), rs.getDouble("y"), rs.getDouble("z"))
                .build();
    }

    // ===== Jail Data =====

    /**
     * 隔離データを保存
     */
    /**
     * 隔離データを保存 (インベントリバックアップ付き)
     */
    public CompletableFuture<Boolean> saveJailedPlayerAsync(UUID playerId, String playerName, String reason,
            UUID jailedBy, String originalLocation,
            String inventoryBackup, String armorBackup) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String sql;
                if ("mysql".equalsIgnoreCase(dbType)) {
                    sql = """
                            INSERT INTO jailed_players (player_id, player_name, reason, jailed_at, jailed_by, original_location, inventory_backup, armor_backup)
                            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                            ON DUPLICATE KEY UPDATE
                            player_name = VALUES(player_name),
                            reason = VALUES(reason),
                            jailed_at = VALUES(jailed_at),
                            jailed_by = VALUES(jailed_by),
                            original_location = VALUES(original_location),
                            inventory_backup = VALUES(inventory_backup),
                            armor_backup = VALUES(armor_backup)
                        """;
                } else {
                    sql = """
                            MERGE INTO jailed_players (player_id, player_name, reason, jailed_at, jailed_by, original_location, inventory_backup, armor_backup)
                            KEY (player_id)
                            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                        """;
                }

                try (PreparedStatement ps = connection.prepareStatement(sql)) {
                    ps.setString(1, playerId.toString());
                    ps.setString(2, playerName);
                    ps.setString(3, reason);
                    ps.setLong(4, System.currentTimeMillis());
                    ps.setString(5, jailedBy != null ? jailedBy.toString() : null);
                    ps.setString(6, originalLocation);
                    ps.setString(7, inventoryBackup);
                    ps.setString(8, armorBackup);
                    ps.executeUpdate();
                    return true;
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "隔離データ保存失敗", e);
                return false;
            }
        });
    }

    /**
     * @deprecated Use saveJailedPlayerAsync instead
     */
    @Deprecated
    public void saveJailedPlayer(UUID playerId, String playerName, String reason,
            UUID jailedBy, String originalLocation,
            String inventoryBackup, String armorBackup) {
        saveJailedPlayerAsync(playerId, playerName, reason, jailedBy, originalLocation, inventoryBackup, armorBackup);
    }

    /**
     * @deprecated Use the version with inventory backup instead
     */
    @Deprecated
    public void saveJailedPlayer(UUID playerId, String playerName, String reason,
            UUID jailedBy, String originalLocation) {
        saveJailedPlayer(playerId, playerName, reason, jailedBy, originalLocation, null, null);
    }

    /**
     * 隔離データを削除
     */
    public CompletableFuture<Void> removeJailedPlayerAsync(UUID playerId) {
        return CompletableFuture.runAsync(() -> {
            try {
                String sql = "DELETE FROM jailed_players WHERE player_id = ?";
                try (PreparedStatement ps = connection.prepareStatement(sql)) {
                    ps.setString(1, playerId.toString());
                    ps.executeUpdate();
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "隔離データ削除失敗", e);
            }
        });
    }

    /**
     * @deprecated Use removeJailedPlayerAsync instead
     */
    @Deprecated
    public void removeJailedPlayer(UUID playerId) {
        removeJailedPlayerAsync(playerId);
    }

    /**
     * 隔離プレイヤーの元座標を取得 (同期 - 非推奨)
     * @deprecated Use getOriginalLocationAsync instead
     */
    @Deprecated
    public String getOriginalLocation(UUID playerId) {
        if (isPrimaryThread()) {
            plugin.getLogger().warning("Blocking database call on main thread: getOriginalLocation. Use getOriginalLocationAsync instead.");
        }
        return getOriginalLocationAsync(playerId).join();
    }

    protected boolean isPrimaryThread() {
        try {
            return Bukkit.isPrimaryThread();
        } catch (Exception e) {
            return false; // For testing context where Bukkit is not mocked
        }
    }

    /**
     * 隔離プレイヤーの元座標を取得 (非同期)
     */
    public CompletableFuture<String> getOriginalLocationAsync(UUID playerId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String sql = "SELECT original_location FROM jailed_players WHERE player_id = ?";
                try (PreparedStatement ps = connection.prepareStatement(sql)) {
                    ps.setString(1, playerId.toString());
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            return rs.getString("original_location");
                        }
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "元座標取得失敗", e);
            }
            return null;
        });
    }

    /**
     * 隔離プレイヤーのインベントリバックアップを取得 (同期 - 非推奨)
     * @deprecated Use getInventoryBackupAsync instead
     */
    @Deprecated
    public String getInventoryBackup(UUID playerId) {
        return getInventoryBackupAsync(playerId).join();
    }

    /**
     * 隔離プレイヤーのインベントリバックアップを取得 (非同期)
     */
    public CompletableFuture<String> getInventoryBackupAsync(UUID playerId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String sql = "SELECT inventory_backup FROM jailed_players WHERE player_id = ?";
                try (PreparedStatement ps = connection.prepareStatement(sql)) {
                    ps.setString(1, playerId.toString());
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            return rs.getString("inventory_backup");
                        }
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "インベントリバックアップ取得失敗", e);
            }
            return null;
        });
    }

    /**
     * 隔離プレイヤーの装備バックアップを取得 (同期 - 非推奨)
     * @deprecated Use getArmorBackupAsync instead
     */
    @Deprecated
    public String getArmorBackup(UUID playerId) {
        return getArmorBackupAsync(playerId).join();
    }

    /**
     * 隔離プレイヤーの装備バックアップを取得 (非同期)
     */
    public CompletableFuture<String> getArmorBackupAsync(UUID playerId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String sql = "SELECT armor_backup FROM jailed_players WHERE player_id = ?";
                try (PreparedStatement ps = connection.prepareStatement(sql)) {
                    ps.setString(1, playerId.toString());
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            return rs.getString("armor_backup");
                        }
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "装備バックアップ取得失敗", e);
            }
            return null;
        });
    }

    /**
     * 隔離中かどうか確認 (同期 - 非推奨)
     * @deprecated Use isJailedAsync instead
     */
    @Deprecated
    public boolean isJailed(UUID playerId) {
        return isJailedAsync(playerId).join();
    }

    /**
     * 隔離中かどうか確認 (非同期)
     */
    public CompletableFuture<Boolean> isJailedAsync(UUID playerId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String sql = "SELECT 1 FROM jailed_players WHERE player_id = ?";
                try (PreparedStatement ps = connection.prepareStatement(sql)) {
                    ps.setString(1, playerId.toString());
                    try (ResultSet rs = ps.executeQuery()) {
                        return rs.next();
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "隔離確認失敗", e);
            }
            return false;
        });
    }

    // ===== Warnings Data =====

    public CompletableFuture<Void> addWarningAsync(UUID playerId, String playerName, String reason, String warnedBy, long timestamp) {
        return CompletableFuture.runAsync(() -> {
            try {
                String sql = "INSERT INTO warnings (player_id, player_name, reason, warned_by, timestamp) VALUES (?, ?, ?, ?, ?)";
                try (PreparedStatement ps = connection.prepareStatement(sql)) {
                    ps.setString(1, playerId.toString());
                    ps.setString(2, playerName);
                    ps.setString(3, reason);
                    ps.setString(4, warnedBy);
                    ps.setLong(5, timestamp);
                    ps.executeUpdate();
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "警告データ保存失敗", e);
            }
        });
    }

    public CompletableFuture<List<Warning>> getWarningsAsync(UUID playerId) {
        return CompletableFuture.supplyAsync(() -> {
            List<Warning> warnings = new ArrayList<>();
            try {
                String sql = "SELECT * FROM warnings WHERE player_id = ? ORDER BY timestamp ASC";
                try (PreparedStatement ps = connection.prepareStatement(sql)) {
                    ps.setString(1, playerId.toString());
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            Warning w = new Warning();
                            w.reason = rs.getString("reason");
                            w.warnedBy = rs.getString("warned_by");
                            w.timestamp = rs.getLong("timestamp");
                            warnings.add(w);
                        }
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "警告データ取得失敗", e);
            }
            return warnings;
        });
    }

    public CompletableFuture<Void> clearWarningsAsync(UUID playerId) {
        return CompletableFuture.runAsync(() -> {
            try {
                String sql = "DELETE FROM warnings WHERE player_id = ?";
                try (PreparedStatement ps = connection.prepareStatement(sql)) {
                    ps.setString(1, playerId.toString());
                    ps.executeUpdate();
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "警告クリア失敗", e);
            }
        });
    }

    public CompletableFuture<Void> removeLastWarningAsync(UUID playerId) {
        return CompletableFuture.runAsync(() -> {
            try {
                String selectSql = "SELECT id FROM warnings WHERE player_id = ? ORDER BY timestamp DESC LIMIT 1";
                long idToDelete = -1;

                try (PreparedStatement ps = connection.prepareStatement(selectSql)) {
                    ps.setString(1, playerId.toString());
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            idToDelete = rs.getLong("id");
                        }
                    }
                }

                if (idToDelete != -1) {
                    String deleteSql = "DELETE FROM warnings WHERE id = ?";
                    try (PreparedStatement ps = connection.prepareStatement(deleteSql)) {
                        ps.setLong(1, idToDelete);
                        ps.executeUpdate();
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "最新警告削除失敗", e);
            }
        });
    }

    /**
     * 古いログを削除
     */
    public void cleanupOldLogs() {
        int days = plugin.getConfigManager().getKillLogRetentionDays();
        long cutoff = System.currentTimeMillis() - (days * 24L * 60 * 60 * 1000);

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                String sql = "DELETE FROM kill_logs WHERE timestamp < ?";
                try (PreparedStatement ps = connection.prepareStatement(sql)) {
                    ps.setLong(1, cutoff);
                    int deleted = ps.executeUpdate();
                    if (deleted > 0) {
                        plugin.getLogger().info("古い戦闘ログを削除: " + deleted + "件");
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "古いログ削除失敗", e);
            }
        });
    }

    /**
     * シャットダウン処理
     */
    public void shutdown() {
        if (connection != null) {
            try {
                connection.close();
                plugin.getLogger().info("データベース接続を閉じました");
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "データベース切断失敗", e);
            }
        }
    }
}
