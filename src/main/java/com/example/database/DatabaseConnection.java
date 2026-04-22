package com.example.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {

    // Paramètres de connexion
    private static final String URL      = "jdbc:mysql://localhost:3307/chatapp"+ "?useSSL=false"+ "&serverTimezone=UTC"+ "&allowPublicKeyRetrieval=true";
    private static final String USER     = "root";
    private static final String PASSWORD = ""; 

    // Instance unique (Singleton) 
    private static Connection instance = null;

    // Constructeur privé → on ne peut pas faire new DatabaseConnection()
    private DatabaseConnection() {}

    // Méthode principale : obtenir la connexion 
   
    public static Connection getConnection() {
        try {
            if (instance == null || instance.isClosed()) {

                Class.forName("com.mysql.cj.jdbc.Driver");

                instance = DriverManager.getConnection(URL, USER, PASSWORD);

                System.out.println("[DB] ✅ Connexion MySQL établie avec succès.");
            }
        } catch (ClassNotFoundException e) {
            System.err.println("[DB] ❌ Driver MySQL introuvable : " + e.getMessage());
            instance = null; 
        } catch (SQLException e) {
            System.err.println("[DB] ❌ Erreur connexion MySQL : " + e.getMessage());
            instance = null; 
        }
        return instance;
    }

    // Fermer la connexion 
    public static void closeConnection() {
        try {
            if (instance != null && !instance.isClosed()) {
                instance.close();
                System.out.println("[DB] Connexion MySQL fermée.");
            }
        } catch (SQLException e) {
            System.err.println("[DB] Erreur fermeture : " + e.getMessage());
        }
    }

    // Test de connexion 
    public static boolean isConnected() {
        try {
            return instance != null && !instance.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }
}