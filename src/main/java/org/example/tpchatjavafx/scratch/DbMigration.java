package org.example.tpchatjavafx.scratch;

import org.example.tpchatjavafx.dao.DatabaseConnection;

import java.sql.Connection;
import java.sql.Statement;

import java.sql.DriverManager;

public class DbMigration {

    public static void main(String[] args) {
        runMigration();
    }

    public static void runMigration() {
        System.out.println("Starting Database Migration...");
        try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/?useSSL=false&serverTimezone=UTC", "root", "");
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS wechat CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;");
            System.out.println("Database 'wechat' ensured.");
        } catch (Exception e) {
            System.err.println("Could not ensure database exists: " + e.getMessage());
        }

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement()) {
            String content = new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get("chattapp_db.sql")));
            String[] queries = content.split(";");
            for (String q : queries) {
                if (!q.trim().isEmpty()) {
                    try {
                        stmt.execute(q.trim());
                    } catch (Exception e) {
                        System.out.println("Init SQL skipped: " + e.getMessage());
                    }
                }
            }
            System.out.println("Base schema initialized.");
        } catch (Exception e) {
            System.err.println("Error running base schema: " + e.getMessage());
        }

        String[] statements = {

            "ALTER TABLE message ADD COLUMN message_parent_id INT NULL",
            "ALTER TABLE message ADD COLUMN reply_preview_text VARCHAR(255) NULL",
            "ALTER TABLE message ADD COLUMN reply_to_user_id INT NULL",
            "ALTER TABLE message ADD CONSTRAINT fk_msg_parent FOREIGN KEY (message_parent_id) REFERENCES message(id) ON DELETE SET NULL",
            "ALTER TABLE message ADD CONSTRAINT fk_reply_to_user FOREIGN KEY (reply_to_user_id) REFERENCES utilisateur(id) ON DELETE SET NULL",

            "ALTER TABLE groupe ADD COLUMN ephemeral_timer INT DEFAULT 0",
            "ALTER TABLE groupe_membre ADD COLUMN is_muted BOOLEAN DEFAULT FALSE",
            "ALTER TABLE groupe_membre ADD COLUMN muted_until DATETIME NULL",
            "ALTER TABLE groupe_membre ADD COLUMN is_favorite BOOLEAN DEFAULT FALSE",

            "CREATE TABLE IF NOT EXISTS appel_planifie (" +
            "  id INT AUTO_INCREMENT PRIMARY KEY," +
            "  groupe_id INT NOT NULL," +
            "  titre VARCHAR(100) NOT NULL," +
            "  date_heure DATETIME NOT NULL," +
            "  type_appel VARCHAR(10) NOT NULL," +
            "  lien_unique VARCHAR(255) NOT NULL," +
            "  FOREIGN KEY (groupe_id) REFERENCES groupe(id) ON DELETE CASCADE" +
            ") ENGINE = InnoDB"
        };

        try (Connection conn = DatabaseConnection.getInstance().getConnection()) {
            for (String sql : statements) {
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute(sql);
                    System.out.println("SUCCESS: " + sql);
                } catch (Exception e) {
                    System.out.println("INFO (Probably already applied): " + e.getMessage());
                }
            }
            System.out.println("Database migration completed!");
        } catch (Exception e) {
            System.err.println("Fatal error during migration: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
