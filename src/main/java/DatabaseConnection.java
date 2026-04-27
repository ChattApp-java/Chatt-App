import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.SQLException;

public class DatabaseConnection {
    private static final int POOL_SIZE = 10;
    private static HikariDataSource dataSource;
    private static volatile boolean initialized = false;
    private static String lastError = null;

    private static synchronized void initializePool() {
        if (initialized || dataSource != null) return;

        try {
            HikariConfig config = new HikariConfig();

            // Configuration externalisée (sécurité : pas d'identifiants en dur)
            /*
            String dbUrl  = System.getenv().getOrDefault("CHATAPP_DB_URL",
                    "jdbc:mysql://192.168.225.148:3307/chattapp?useSSL=true&serverTimezone=UTC&allowPublicKeyRetrieval=false");
            String dbUser = System.getenv().getOrDefault("CHATAPP_DB_USER", "root");
            String dbPass = System.getenv().getOrDefault("CHATAPP_DB_PASSWORD", "");
            */

            String dbUrl = System.getenv().getOrDefault(
                    "CHATAPP_DB_URL",
                    "jdbc:mysql://192.168.225.148:3307/chattapp?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true"
            );
            String dbUser = System.getenv().getOrDefault(
                    "CHATAPP_DB_USER",
                    "chatuser"
            );
            String dbPass = System.getenv().getOrDefault(
                    "CHATAPP_DB_PASSWORD",
                    "1234"
            );

            config.setJdbcUrl(dbUrl);
            config.setUsername(dbUser);
            config.setPassword(dbPass);

            // Pool settings
            config.setMaximumPoolSize(POOL_SIZE);
            config.setMinimumIdle(2);
            config.setConnectionTimeout(30000); // 30s
            config.setIdleTimeout(600000);      // 10min
            config.setMaxLifetime(1800000);     // 30min

            // Health checks
            config.setConnectionTestQuery("SELECT 1");
            config.setLeakDetectionThreshold(60000); // 60s

            // Driver class
            config.setDriverClassName("com.mysql.cj.jdbc.Driver");

            dataSource = new HikariDataSource(config);
            initialized = true;

            System.out.println("[DB] ✅ Pool HikariCP initialisé (max=" + POOL_SIZE + ")");

        } catch (Exception e) {
            lastError = e.getMessage();
            System.err.println("[DB] ❌ Échec de l'initialisation du pool : " + e.getMessage());
            dataSource = null;
            initialized = false;
        }
    }

    /**
     * Obtient une connexion depuis le pool.
     * L'appelant DOIT la fermer (try-with-resources) pour la retourner au pool.
     */
    public static Connection getConnection() throws SQLException {
        if (!initialized) {
            initializePool();
        }

        if (dataSource == null || dataSource.isClosed()) {
            throw new SQLException("[DB] ❌ DataSource non initialisée ou fermée. Dernière erreur : " + lastError);
        }

        return dataSource.getConnection();
    }

    /**
     * Ferme proprement le pool (à appeler à l'arrêt du serveur).
     */
    public static void closePool() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            System.out.println("[DB] Pool HikariCP fermé.");
        }
        initialized = false;
        dataSource = null;
    }

    public static boolean isHealthy() {
        try (Connection conn = getConnection()) {
            return conn.isValid(5);
        } catch (SQLException e) {
            System.err.println("[DB] Health check failed: " + e.getMessage());
            return false;
        }
    }

    public static String getLastError() {
        return lastError;
    }

    private DatabaseConnection() {}
}