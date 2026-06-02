package org.example.tpchatjavafx.service;

import org.example.tpchatjavafx.dao.UtilisateurDAO;
import org.example.tpchatjavafx.model.Utilisateur;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.SQLException;

public class AuthService {

    private final UtilisateurDAO utilisateurDAO = new UtilisateurDAO();

    public record RegisterResult(boolean success, String reason) {}

    public Utilisateur login(String username, String password) {
        if (username == null || username.isBlank()) return null;
        if (password == null || password.isBlank()) return null;

        try {
            Utilisateur u = utilisateurDAO.findByUsername(username.trim());
            if (u == null) return null;
            if (!BCrypt.checkpw(password, u.getPassword())) return null;
            return u;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public RegisterResult register(String username, String password, String email) {
        if (username == null || username.isBlank())
            return new RegisterResult(false, "Nom d'utilisateur requis");
        if (password == null || password.length() < 4)
            return new RegisterResult(false, "Mot de passe trop court (min 4 caractÃƒÂ¨res)");

        username = username.trim();

        try {
            if (utilisateurDAO.findByUsername(username) != null)
                return new RegisterResult(false, "Nom d'utilisateur dÃƒÂ©jÃƒÂ  pris");

            String hash = BCrypt.hashpw(password, BCrypt.gensalt(12));

            if (email == null || email.isBlank()) {
                return new RegisterResult(false, "Adresse email requise");
            }
            String finalEmail = email.trim();
            if (!finalEmail.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
                return new RegisterResult(false, "Adresse email invalide");
            }

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
        return new RegisterResult(false, "Erreur lors de la crÃƒÂ©ation du compte");
    }
}
