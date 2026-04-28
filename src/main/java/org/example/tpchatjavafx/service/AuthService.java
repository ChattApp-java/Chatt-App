package org.example.tpchatjavafx.service;

import org.example.tpchatjavafx.dao.UtilisateurDAO;
import org.example.tpchatjavafx.model.Utilisateur;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.SQLException;

/**
 * Service d'authentification : login et inscription.
 */
public class AuthService {

    private final UtilisateurDAO utilisateurDAO = new UtilisateurDAO();

    // ── Résultat d'inscription ──────────────────────────────────
    public record RegisterResult(boolean success, String reason) {}

    /**
     * Authentifie un utilisateur existant.
     * @return Utilisateur si OK, null si identifiants invalides
     */
    public Utilisateur login(String username, String password) {
        if (username == null || username.isBlank()) return null;
        if (password == null || password.isBlank()) return null;
        
        try {
            Utilisateur u = utilisateurDAO.findByUsername(username.trim());
            if (u == null) return null;
            if (!BCrypt.checkpw(password, u.getPassword())) return null;
            // The connection status and last login will be updated by ConnexionDAO when sockets connect.
            return u;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Inscrit un nouvel utilisateur.
     */
    public RegisterResult register(String username, String password, String email) {
        if (username == null || username.isBlank())
            return new RegisterResult(false, "Nom d'utilisateur requis");
        if (password == null || password.length() < 4)
            return new RegisterResult(false, "Mot de passe trop court (min 4 caractères)");

        username = username.trim();

        try {
            if (utilisateurDAO.findByUsername(username) != null)
                return new RegisterResult(false, "Nom d'utilisateur déjà pris");

            String hash = BCrypt.hashpw(password, BCrypt.gensalt(12));
            String finalEmail = (email == null || email.isBlank()) ? username + "@chat.local" : email.trim();
            
            Utilisateur u = new Utilisateur();
            u.setUsername(username);
            u.setPassword(hash);
            u.setEmail(finalEmail);
            
            Utilisateur created = utilisateurDAO.create(u);
            if (created != null && created.getId() > 0) {
                return new RegisterResult(true, "");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return new RegisterResult(false, "Erreur serveur : " + e.getMessage());
        }
        return new RegisterResult(false, "Erreur lors de la création du compte");
    }
}
