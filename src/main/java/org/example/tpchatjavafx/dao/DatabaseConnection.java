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

        String host = "gateway01.eu-central-1.prod.aws.tidbcloud.com";
        String port = "4000";
        String database = "wechat";  
        String user = "43jvDrD7MyYUkLK.root";
        String password = "MtY04UxKcRkHvHxR";  

        config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database
                + "?sslMode=VERIFY_IDENTITY"
                + "&serverTimezone=UTC"
                + "&useUnicode=true"
                + "&characterEncoding=UTF-8");

        config.setUsername(user);
        config.setPassword(password);

        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);

        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("connectTimeout", "10000");

        dataSource = new HikariDataSource(config);

        System.out.println("Ã¢Å“â€¦ Connexion TiDB Cloud initialisÃƒÂ©e !");
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
            System.out.println("Ã°Å¸â€â€™ Pool de connexions fermÃƒÂ©.");
        }
    }
}
