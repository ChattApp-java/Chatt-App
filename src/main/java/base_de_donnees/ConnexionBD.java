package base_de_donnees;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Singleton de connexion à la base MySQL.
 */
public class ConnexionBD {

    private static final String URL = "jdbc:mysql://localhost:3306/chatapp"
            + "?useSSL=false"
            + "&serverTimezone=UTC"
            + "&allowPublicKeyRetrieval=true";
    private static final String USER = "root";
    private static final String PASSWORD = "";

    private static Connection instance = null;

    private ConnexionBD() {}

    public static Connection getConnection() {
        try {
            if (instance == null || instance.isClosed()) {
                Class.forName("com.mysql.cj.jdbc.Driver");
                instance = DriverManager.getConnection(URL, USER, PASSWORD);
                System.out.println("[DB] Connexion MySQL établie.");
            }
        } catch (ClassNotFoundException e) {
            System.err.println("[DB] Driver introuvable : " + e.getMessage());
            instance = null;
        } catch (SQLException e) {
            System.err.println("[DB] Erreur connexion : " + e.getMessage());
            instance = null;
        }
        return instance;
    }

    public static void closeConnection() {
        try {
            if (instance != null && !instance.isClosed()) {
                instance.close();
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