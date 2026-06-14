package org.yuemi.bingogacha.plugin.repository;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public class DatabaseManager {

    private final JavaPlugin plugin;
    private final String storageType;
    private final Map<String, Object> dbSettings;
    private HikariDataSource dataSource;

    public DatabaseManager(
            @NotNull JavaPlugin plugin,
            @NotNull String storageType,
            @NotNull Map<String, Object> dbSettings
    ) {
        this.plugin = plugin;
        this.storageType = storageType;
        this.dbSettings = dbSettings;
    }

    public void init() throws SQLException {
        HikariConfig config = new HikariConfig();

        if (storageType.equalsIgnoreCase("mysql")) {
            String host = (String) dbSettings.getOrDefault("mysql.host", "localhost");
            int port = ((Number) dbSettings.getOrDefault("mysql.port", 3306)).intValue();
            String database = (String) dbSettings.getOrDefault("mysql.database", "bingo_gacha");
            String username = (String) dbSettings.getOrDefault("mysql.username", "root");
            String password = (String) dbSettings.getOrDefault("mysql.password", "");

            config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database);
            config.setUsername(username);
            config.setPassword(password);
            config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        } else {
            // SQLite
            String fileName = (String) dbSettings.getOrDefault("sqlite.file", "data.db");
            File dbFile = new File(plugin.getDataFolder(), fileName);
            if (!dbFile.getParentFile().exists()) {
                dbFile.getParentFile().mkdirs();
            }

            config.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());
            config.setDriverClassName("org.sqlite.JDBC");
        }

        // Pool settings
        config.setMaximumPoolSize(((Number) dbSettings.getOrDefault("mysql.pool-settings.maximum-pool-size", 10)).intValue());
        config.setMinimumIdle(((Number) dbSettings.getOrDefault("mysql.pool-settings.minimum-idle", 2)).intValue());
        config.setConnectionTimeout(((Number) dbSettings.getOrDefault("mysql.pool-settings.connection-timeout", 30000)).longValue());
        config.setIdleTimeout(((Number) dbSettings.getOrDefault("mysql.pool-settings.idle-timeout", 600000)).longValue());
        config.setMaxLifetime(((Number) dbSettings.getOrDefault("mysql.pool-settings.max-lifetime", 1800000)).longValue());

        // Optimize performance
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

        dataSource = new HikariDataSource(config);
        createTables();
    }

    private void createTables() throws SQLException {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            
            String sql = "CREATE TABLE IF NOT EXISTS bingo_player_cards (" +
                    "id INTEGER PRIMARY KEY " + (storageType.equalsIgnoreCase("mysql") ? "AUTO_INCREMENT" : "AUTOINCREMENT") + ", " +
                    "player_uuid VARCHAR(36) NOT NULL, " +
                    "template_id VARCHAR(64) NOT NULL, " +
                    "completed TINYINT NOT NULL DEFAULT 0, " +
                    "unlocked_slots TEXT, " +
                    "created_at BIGINT NOT NULL, " +
                    "completed_at BIGINT NOT NULL DEFAULT 0" +
                    ")";
            stmt.executeUpdate(sql);
            
            // Create index for fast retrieval
            try {
                stmt.executeUpdate("CREATE INDEX idx_player_uuid ON bingo_player_cards(player_uuid)");
            } catch (SQLException ignored) {
                // SQLite may throw if the index already exists or syntax differs slightly
            }
        }
    }

    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    @NotNull
    public Connection getConnection() throws SQLException {
        if (dataSource == null) {
            throw new SQLException("DatabaseManager is not initialized.");
        }
        return dataSource.getConnection();
    }
}
