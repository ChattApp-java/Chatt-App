package com.chatapp.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {

    private static final String URL =
            "jdbc:mysql://localhost:3307/chatapp"
            + "?useSSL=false"
            + "&serverTimezone=UTC"
            + "&allowPublicKeyRetrieval=true";
    private static final String USER     = "root";
    private static final String PASSWORD = "";

    private static Connection instance = null;

    private DatabaseConnection() {}

    // ✅ CORRECTION : synchronized → thread-safe, plus de race condition
    public static synchronized Connection getConnection() {
        try {
            if (instance == null || instance.isClosed()) {
                Class.forName("com.mysql.cj.jdbc.Driver");
                instance = DriverManager.getConnection(URL, USER, PASSWORD);
                System.out.println("[DB] ✅ Connexion MySQL établie.");
            }
        } catch (ClassNotFoundException e) {
            System.err.println("[DB] ❌ Driver introuvable : " + e.getMessage());
            instance = null;
        } catch (SQLException e) {
            System.err.println("[DB] ❌ Erreur connexion : " + e.getMessage());
            instance = null;
        }

        // ✅ CORRECTION : lever une exception claire au lieu de retourner null
        if (instance == null) {
            throw new RuntimeException("[DB] ❌ Impossible d'obtenir une connexion MySQL !");
        }

        return instance;
    }

    public static synchronized void closeConnection() {
        try {
            if (instance != null && !instance.isClosed()) {
                instance.close();
                instance = null;
                System.out.println("[DB] Connexion fermée.");
            }
        } catch (SQLException e) {
            System.err.println("[DB] Erreur fermeture : " + e.getMessage());
        }
    }

    public static boolean isConnected() {
        try {
            return instance != null && !instance.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }
}

