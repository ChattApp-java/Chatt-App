package base_de_donnees;

import modele.Utilisateur;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object pour les utilisateurs.
 */
public class DAOUtilisateur {

    public DAOUtilisateur() {}

    public boolean inscrire(Utilisateur user) {
        String sql = "INSERT INTO users (username, password, email) VALUES (?, ?, ?)";
        try (Connection conn = ConnexionBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, user.getUsername());
            stmt.setString(2, user.getPassword());
            stmt.setString(3, user.getEmail());

            int rows = stmt.executeUpdate();
            System.out.println("[DB] Utilisateur inscrit : " + user.getUsername());
            return rows > 0;

        } catch (SQLException e) {
            System.err.println("[DB] Erreur inscription : " + e.getMessage());
            return false;
        }
    }

    public Utilisateur connecter(String username, String password) {
        String sql = "SELECT * FROM users WHERE username = ? AND password = ?";
        try (Connection conn = ConnexionBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            stmt.setString(2, password);

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                setStatut(username, true);
                System.out.println("[DB] Connexion réussie : " + username);
                return extraireUser(rs);
            } else {
                System.out.println("[DB] Connexion échouée : " + username);
                return null;
            }

        } catch (SQLException e) {
            System.err.println("[DB] Erreur connexion : " + e.getMessage());
            return null;
        }
    }

    public boolean userExiste(String username) {
        String sql = "SELECT COUNT(*) FROM users WHERE username = ?";
        try (Connection conn = ConnexionBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }

        } catch (SQLException e) {
            System.err.println("[DB] Erreur vérification : " + e.getMessage());
        }
        return false;
    }

    public void setStatut(String username, boolean status) {
        String sql = "UPDATE users SET status = ? WHERE username = ?";
        try (Connection conn = ConnexionBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setBoolean(1, status);
            stmt.setString(2, username);
            stmt.executeUpdate();
            System.out.println("[DB] Statut : " + username + " → " + (status ? "En ligne" : "Hors ligne"));

        } catch (SQLException e) {
            System.err.println("[DB] Erreur statut : " + e.getMessage());
        }
    }

    public Utilisateur getUserByUsername(String username) {
        String sql = "SELECT * FROM users WHERE username = ?";
        try (Connection conn = ConnexionBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return extraireUser(rs);

        } catch (SQLException e) {
            System.err.println("[DB] Erreur getUserByUsername : " + e.getMessage());
        }
        return null;
    }

    public Utilisateur getUserById(int id) {
        String sql = "SELECT * FROM users WHERE id_user = ?";
        try (Connection conn = ConnexionBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return extraireUser(rs);

        } catch (SQLException e) {
            System.err.println("[DB] Erreur getUserById : " + e.getMessage());
        }
        return null;
    }

    public List<Utilisateur> getTousConnectes() {
        List<Utilisateur> liste = new ArrayList<>();
        String sql = "SELECT * FROM users WHERE status = true";
        try (Connection conn = ConnexionBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                liste.add(extraireUser(rs));
            }

        } catch (SQLException e) {
            System.err.println("[DB] Erreur getTousConnectes : " + e.getMessage());
        }
        return liste;
    }

    public void deconnecter(String username) {
        setStatut(username, false);
        System.out.println("[DB] " + username + " déconnecté.");
    }

    private Utilisateur extraireUser(ResultSet rs) throws SQLException {
        return new Utilisateur(
                rs.getInt("id_user"),
                rs.getString("username"),
                rs.getString("password"),
                rs.getString("email"),
                rs.getBoolean("status"),
                rs.getTimestamp("created_at") != null
                        ? rs.getTimestamp("created_at").toLocalDateTime()
                        : LocalDateTime.now()
        );
    }
}