package org.example.tpchatjavafx.auth;

import org.example.tpchatjavafx.database.UserDAO;
import org.example.tpchatjavafx.model.User;

/**
 * Service d'authentification : login et inscription.
 */
public class AuthService {

    private final UserDAO userDAO = new UserDAO();

    // ── Résultat d'inscription ──────────────────────────────────
    public record RegisterResult(boolean success, String reason) {}

    /**
     * Authentifie un utilisateur existant.
     * @return User si OK, null si identifiants invalides
     */
    public User login(String username, String password) {
        if (username == null || username.isBlank()) return null;
        if (password == null || password.isBlank()) return null;
        return userDAO.authenticate(username.trim(), password);
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

        if (userDAO.findByUsername(username) != null)
            return new RegisterResult(false, "Nom d'utilisateur déjà pris");

        boolean created = userDAO.createUser(username, password, email == null ? "" : email.trim());
        if (created) return new RegisterResult(true, "");
        return new RegisterResult(false, "Erreur lors de la création du compte");
    }
}
