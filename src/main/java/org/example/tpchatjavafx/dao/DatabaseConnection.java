package org.example.tpchatjavafx.dao;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;

public class DatabaseConnection {
    private static DatabaseConnection instance;
    private HikariDataSource dataSource;

    private DatabaseConnection() {
        HikariConfig config = new HikariConfig();

        // ============================================
        // CONFIGURATION TIDB CLOUD (ONLINE)
        // ============================================

        // Vos informations TiDB Cloud
        String host = "gateway01.eu-central-1.prod.aws.tidbcloud.com";
        String port = "4000";
        String database = "wechat";  // ← Votre base créée dans SQL Editor
        String user = "43jvDrD7MyYUkLK.root";
        String password = "MtY04UxKcRkHvHxR";  // ← Votre password

        // Si erreur SSL, ajoutez le chemin du certificat
        config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database
                + "?sslMode=VERIFY_IDENTITY"
                + "&serverTimezone=UTC"
                + "&useUnicode=true"
                + "&characterEncoding=UTF-8");

        config.setUsername(user);
        config.setPassword(password);

        // ============================================
        // CONFIGURATION HIKARI (optimisée pour cloud)
        // ============================================

        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);

        // Optimisations requêtes
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("connectTimeout", "10000");

        dataSource = new HikariDataSource(config);

        System.out.println("✅ Connexion TiDB Cloud initialisée !");
    }

    public static DatabaseConnection getInstance() {
        if (instance == null) {
            synchronized (DatabaseConnection.class) {
                if (instance == null) {
                    instance = new DatabaseConnection();
                }
            }
        }
        return instance;
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public void close() {
        if (dataSource != null) {
            dataSource.close();
            System.out.println("🔒 Pool de connexions fermé.");
        }
    }
}