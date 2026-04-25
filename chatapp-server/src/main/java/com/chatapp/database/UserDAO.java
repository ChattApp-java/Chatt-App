package com.chatapp.database;

import com.chatapp.model.User;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class UserDAO {

    public UserDAO() {}

    /**
     * Inscrit un nouvel utilisateur avec mot de passe hashé.
     * Vérifie d'abord que le nom d'utilisateur n'existe pas.
     * @return true si inscrit, false si l'utilisateur existe déjà
     * @throws SQLException en cas d'erreur base de données
     */
    public boolean inscrire(User user) throws SQLException {
        if (userExiste(user.getUsername())) {
            System.err.println("[DB] Inscription refusée : utilisateur '" + user.getUsername() + "' existe déjà.");
            return false;
        }

        String sql = "INSERT INTO users (username, password, email) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            String hashedPassword = BCrypt.hashpw(user.getPassword(), BCrypt.gensalt(12));

            stmt.setString(1, user.getUsername());
            stmt.setString(2, hashedPassword);
            stmt.setString(3, user.getEmail());

            int rows = stmt.executeUpdate();
            System.out.println("[DB] Utilisateur inscrit : " + user.getUsername());
            return rows > 0;
        }
    }

    /**
     * Authentifie un utilisateur avec son mot de passe en clair.
     * Retourne l'utilisateur si succès, null sinon.
     * Met à jour le statut en ligne.
     */
    public User authentifier(String username, String password) {
        String sql = "SELECT * FROM users WHERE username = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                String hashStocke = rs.getString("password");

                if (BCrypt.checkpw(password, hashStocke)) {
                    setStatut(username, true);
                    System.out.println("[DB] Authentification réussie : " + username);
                    return extraireUserPublic(rs);
                } else {
                    System.out.println("[DB] Mot de passe incorrect : " + username);
                    return null;
                }
            } else {
                System.out.println("[DB] Utilisateur introuvable : " + username);
                return null;
            }

        } catch (SQLException e) {
            System.err.println("[DB] Erreur authentification : " + e.getMessage());
            return null;
        }
    }

    /**
     * @deprecated Utilisez authentifier() qui vérifie le mot de passe.
     */
    @Deprecated
    public User connecter(String username, String password) {
        return authentifier(username, password);
    }

    public boolean userExiste(String username) {
        String sql = "SELECT COUNT(*) FROM users WHERE username = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getInt(1) > 0;

        } catch (SQLException e) {
            System.err.println("[DB] Erreur vérification : " + e.getMessage());
        }
        return false;
    }

    public void setStatut(String username, boolean status) {
        String sql = "UPDATE users SET status = ? WHERE username = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setBoolean(1, status);
            stmt.setString(2, username);
            stmt.executeUpdate();
            System.out.println("[DB] Statut mis à jour : "
                    + username + " → " + (status ? "En ligne" : "Hors ligne"));

        } catch (SQLException e) {
            System.err.println("[DB] Erreur statut : " + e.getMessage());
        }
    }

    /**
     * Retourne l'utilisateur sans le hash du mot de passe (sécurité).
     */
    public User getUserByUsername(String username) {
        String sql = "SELECT id_user, username, email, status, created_at FROM users WHERE username = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return extraireUserPublic(rs);

        } catch (SQLException e) {
            System.err.println("[DB] Erreur getUserByUsername : " + e.getMessage());
        }
        return null;
    }

    public User getUserById(int id) {
        String sql = "SELECT id_user, username, email, status, created_at FROM users WHERE id_user = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return extraireUserPublic(rs);

        } catch (SQLException e) {
            System.err.println("[DB] Erreur getUserById : " + e.getMessage());
        }
        return null;
    }

    public List<User> getTousConnectes() {
        List<User> liste = new ArrayList<>();
        String sql = "SELECT id_user, username, email, status, created_at FROM users WHERE status = true";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) liste.add(extraireUserPublic(rs));

        } catch (SQLException e) {
            System.err.println("[DB] Erreur getTousConnectes : " + e.getMessage());
        }
        return liste;
    }

    public void deconnecter(String username) {
        setStatut(username, false);
        System.out.println("[DB] " + username + " déconnecté.");
    }

    /**
     * Extrait un User COMPLET (avec password) — usage interne uniquement (auth).
     */
    private User extraireUser(ResultSet rs) throws SQLException {
        return new User(
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

    /**
     * Extrait un User PUBLIC (sans password) — usage API.
     */
    private User extraireUserPublic(ResultSet rs) throws SQLException {
        return new User(
                rs.getInt("id_user"),
                rs.getString("username"),
                null, // password jamais exposé
                rs.getString("email"),
                rs.getBoolean("status"),
                rs.getTimestamp("created_at") != null
                        ? rs.getTimestamp("created_at").toLocalDateTime()
                        : LocalDateTime.now()
        );
    }
}

