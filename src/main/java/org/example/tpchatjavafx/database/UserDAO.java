package org.example.tpchatjavafx.database;

import org.example.tpchatjavafx.model.User;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.*;

/**
 * Accès base de données pour la table {@code users}.
 */
public class UserDAO {

    /** Recherche par username ou email. Retourne null si introuvable. */
    public User findByUsername(String username) {
        String sql = "SELECT id, username, email, password_hash FROM users WHERE username = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return map(rs);
        } catch (SQLException e) {
            System.err.println("[UserDAO] findByUsername: " + e.getMessage());
        }
        return null;
    }

    public User findByEmail(String email) {
        String sql = "SELECT id, username, email, password_hash FROM users WHERE email = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return map(rs);
        } catch (SQLException e) {
            System.err.println("[UserDAO] findByEmail: " + e.getMessage());
        }
        return null;
    }

    /**
     * Crée un nouvel utilisateur avec hash bcrypt.
     * @return true si succès, false si username/email déjà pris
     */
    public boolean createUser(String username, String plainPassword, String email) {
        String hash = BCrypt.hashpw(plainPassword, BCrypt.gensalt(12));
        String sql  = "INSERT INTO users (username, email, password_hash) VALUES (?, ?, ?)";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, email.isEmpty() ? username + "@chat.local" : email);
            ps.setString(3, hash);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("[UserDAO] createUser: " + e.getMessage());
            return false;
        }
    }

    /** Vérifie username + mot de passe en clair. Retourne l'User si OK, null sinon. */
    public User authenticate(String username, String plainPassword) {
        User user = findByUsername(username);
        if (user == null) return null;
        if (!BCrypt.checkpw(plainPassword, user.getPasswordHash())) return null;
        updateLastLogin(user.getId());
        return user;
    }

    private void updateLastLogin(int userId) {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("UPDATE users SET last_login=NOW() WHERE id=?")) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        } catch (SQLException ignored) {}
    }

    private User map(ResultSet rs) throws SQLException {
        return new User(
            rs.getInt("id"),
            rs.getString("username"),
            rs.getString("email"),
            rs.getString("password_hash")
        );
    }
}
