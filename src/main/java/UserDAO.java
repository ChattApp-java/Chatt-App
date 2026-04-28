import org.mindrot.jbcrypt.BCrypt;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class UserDAO {

    public UserDAO() {}

    public boolean inscrire(User user) throws SQLException {
        if (userExiste(user.getUsername())) {
            System.err.println("[DB] Inscription refusée : l'utilisateur existe déjà.");
            return false;
        }

        String hashedPassword = BCrypt.hashpw(user.getPassword(), BCrypt.gensalt(12));

        String uName = user.getUsername().replace("'", "''");
        String uEmail = user.getEmail().replace("'", "''");

        String sql = "INSERT INTO users (username, password, email) VALUES ('"
                + uName + "', '" + hashedPassword + "', '" + uEmail + "')";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            int rows = stmt.executeUpdate(sql);
            return rows > 0;
        }
    }

    public User authentifier(String username, String password) {
        String uName = username.replace("'", "''");
        String sql = "SELECT * FROM users WHERE username = '" + uName + "'";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                String hashStocke = rs.getString("password");

                if (BCrypt.checkpw(password, hashStocke)) {
                    setStatut(username, true);
                    return extraireUserPublic(rs);
                }
            }

        } catch (SQLException e) {
            System.err.println("[DB] Erreur authentification : " + e.getMessage());
        }
        return null;
    }

    public boolean userExiste(String username) {
        String sql = "SELECT COUNT(*) FROM users WHERE username = '" + username.replace("'", "''") + "'";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1) > 0;
        } catch (SQLException e) {
            System.err.println("[DB] Erreur vérification : " + e.getMessage());
        }
        return false;
    }

    public void setStatut(String username, boolean status) {
        int statutInt = status ? 1 : 0;
        String sql = "UPDATE users SET status = " + statutInt + " WHERE username = '" + username.replace("'", "''") + "'";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            System.err.println("[DB] Erreur statut : " + e.getMessage());
        }
    }

    public User getUserByUsername(String username) {
        String sql = "SELECT id_user, username, email, status, created_at FROM users WHERE username = '" + username.replace("'", "''") + "'";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return extraireUserPublic(rs);
        } catch (SQLException e) {
            System.err.println("[DB] Erreur getUserByUsername : " + e.getMessage());
        }
        return null;
    }

    public List<User> getTousConnectes() {
        List<User> liste = new ArrayList<>();
        String sql = "SELECT id_user, username, email, status, created_at FROM users WHERE status = 1";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) liste.add(extraireUserPublic(rs));
        } catch (SQLException e) {
            System.err.println("[DB] Erreur getTousConnectes : " + e.getMessage());
        }
        return liste;
    }

    public void deconnecter(String username) {
        setStatut(username, false);
    }

    private User extraireUserPublic(ResultSet rs) throws SQLException {
        return new User(
                rs.getInt("id_user"),
                rs.getString("username"),
                null,
                rs.getString("email"),
                rs.getBoolean("status"),
                rs.getTimestamp("created_at") != null
                        ? rs.getTimestamp("created_at").toLocalDateTime()
                        : LocalDateTime.now()
        );
    }
}