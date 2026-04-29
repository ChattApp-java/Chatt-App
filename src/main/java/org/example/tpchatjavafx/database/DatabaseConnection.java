package org.example.tpchatjavafx.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Pool de connexions HikariCP — MySQL localhost:3306/chattapp
 */
public class DatabaseConnection {

    private static HikariDataSource dataSource;
    private static volatile boolean initialized = false;
    private static String lastError = null;

    private static synchronized void init() {
        if (initialized) return;
        try {
            HikariConfig cfg = new HikariConfig();
            cfg.setJdbcUrl("jdbc:mysql://localhost:3306/wechat?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true");
            cfg.setUsername("root");
            cfg.setPassword("");
            cfg.setDriverClassName("com.mysql.cj.jdbc.Driver");
            cfg.setMaximumPoolSize(10);
            cfg.setMinimumIdle(2);
            cfg.setConnectionTimeout(30_000);
            cfg.setIdleTimeout(600_000);
            cfg.setMaxLifetime(1_800_000);
            cfg.setConnectionTestQuery("SELECT 1");
            dataSource  = new HikariDataSource(cfg);
            initialized = true;
            System.out.println("[DB] Pool HikariCP initialisé (max=10)");
        } catch (Exception e) {
            lastError = e.getMessage();
            System.err.println("[DB] Échec init pool : " + e.getMessage());
        }
    }

    public static Connection getConnection() throws SQLException {
        if (!initialized) init();
        if (dataSource == null || dataSource.isClosed())
            throw new SQLException("[DB] DataSource indisponible : " + lastError);
        return dataSource.getConnection();
    }

    public static boolean isHealthy() {
        try (Connection c = getConnection()) { return c.isValid(5); }
        catch (SQLException e) { return false; }
    }

    public static void closePool() {
        if (dataSource != null && !dataSource.isClosed()) dataSource.close();
        initialized = false;
        dataSource  = null;
    }

    private DatabaseConnection() {}
}
