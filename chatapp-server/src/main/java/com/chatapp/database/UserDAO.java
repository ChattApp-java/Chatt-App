package com.chatapp.database;

import com.chatapp.model.User;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class UserDAO {

    public UserDAO() {}

    // ✅ CORRECTION : hash le mot de passe avant de l'enregistrer
    public boolean inscrire(User user) {
        String sql = "INSERT INTO users (username, password, email) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            // Hash du mot de passe avec BCrypt (jamais stocker en clair !)
            String hashedPassword = BCrypt.hashpw(user.getPassword(), BCrypt.gensalt());

            stmt.setString(1, user.getUsername());
            stmt.setString(2, hashedPassword);   // ← mot de passe hashé
            stmt.setString(3, user.getEmail());

            int rows = stmt.executeUpdate();
            System.out.println("[DB] Utilisateur inscrit : " + user.getUsername());
            return rows > 0;

        } catch (SQLException e) {
            System.err.println("[DB] Erreur inscription : " + e.getMessage());
            return false;
        }
    }

    // ✅ CORRECTION : vérifier le mot de passe avec BCrypt.checkpw()
    public User connecter(String username, String password) {
        // On récupère l'utilisateur par son nom, SANS comparer le mot de passe en SQL
        String sql = "SELECT * FROM users WHERE username = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                String hashStocke = rs.getString("password");

                // Vérification sécurisée du mot de passe
                if (BCrypt.checkpw(password, hashStocke)) {
                    setStatut(username, true);
                    System.out.println("[DB] Connexion réussie : " + username);
                    return extraireUser(rs);
                } else {
                    System.out.println("[DB] Mot de passe incorrect : " + username);
                    return null;
                }
            } else {
                System.out.println("[DB] Utilisateur introuvable : " + username);
                return null;
            }

        } catch (SQLException e) {
            System.err.println("[DB] Erreur connexion : " + e.getMessage());
            return null;
        }
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

    public User getUserByUsername(String username) {
        String sql = "SELECT * FROM users WHERE username = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return extraireUser(rs);

        } catch (SQLException e) {
            System.err.println("[DB] Erreur getUserByUsername : " + e.getMessage());
        }
        return null;
    }

    public User getUserById(int id) {
        String sql = "SELECT * FROM users WHERE id_user = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return extraireUser(rs);

        } catch (SQLException e) {
            System.err.println("[DB] Erreur getUserById : " + e.getMessage());
        }
        return null;
    }

    public List<User> getTousConnectes() {
        List<User> liste = new ArrayList<>();
        String sql = "SELECT * FROM users WHERE status = true";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) liste.add(extraireUser(rs));

        } catch (SQLException e) {
            System.err.println("[DB] Erreur getTousConnectes : " + e.getMessage());
        }
        return liste;
    }

    /**
     * Récupère une liste d'utilisateurs par leurs identifiants.
     * @param ids liste d'identifiants recherchés
     * @return liste des utilisateurs correspondants (peut être vide)
     */
    public List<User> getUsersByIds(List<Integer> ids) {
        List<User> liste = new ArrayList<>();
        if (ids == null || ids.isEmpty()) return liste;

        // Construit dynamiquement la clause IN : (?,?,?...)
        StringBuilder placeholders = new StringBuilder();
        for (int i = 0; i < ids.size(); i++) {
            placeholders.append(i == 0 ? "?" : ",?");
        }
        String sql = "SELECT * FROM users WHERE id_user IN (" + placeholders + ")";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            for (int i = 0; i < ids.size(); i++) {
                stmt.setInt(i + 1, ids.get(i));
            }
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                liste.add(extraireUser(rs));
            }
            System.out.println("[DB] getUsersByIds : " + liste.size() + " utilisateur(s) trouvé(s).");

        } catch (SQLException e) {
            System.err.println("[DB] Erreur getUsersByIds : " + e.getMessage());
        }
        return liste;
    }

    public void deconnecter(String username) {
        setStatut(username, false);
        System.out.println("[DB] " + username + " déconnecté.");
    }

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
}

