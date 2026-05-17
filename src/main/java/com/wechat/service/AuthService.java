package com.wechat.service;

import com.wechat.common.EncryptionUtil;
import com.wechat.dao.UserDAO;
import com.wechat.model.User;
import com.wechat.model.User.UserStatus;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Service d'authentification WeChat.
 * Gère l'inscription, la connexion, la déconnexion et les sessions utilisateur.
 * Utilise BCrypt pour le hashage des mots de passe.
 */
public class AuthService {

    private static final Logger LOGGER = Logger.getLogger(AuthService.class.getName());

    // ─── SESSIONS EN MÉMOIRE ───
    // token → User (sessions actives)
    private static final Map<String, User> activeSessions = new ConcurrentHashMap<>();

    // userId → token (pour éviter les doubles connexions)
    private static final Map<Long, String> userTokens = new ConcurrentHashMap<>();

    // ─── VALIDATION ───
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{3,30}$");
    private static final int MIN_PASSWORD_LENGTH = 6;

    private final UserDAO userDAO;

    public AuthService() {
        this.userDAO = new UserDAO();
    }

    // ═══════════════════════════════════════════════════════════
    // INSCRIPTION
    // ═══════════════════════════════════════════════════════════

    /**
     * Inscrit un nouvel utilisateur.
     *
     * @param username  Nom d'utilisateur (3-30 caractères, alphanumérique + _)
     * @param email     Email valide
     * @param password  Mot de passe (min 6 caractères)
     * @param nickname  Pseudo affiché
     * @return Result avec le User créé ou message d'erreur
     */
    public AuthResult register(String username, String email, String password, String nickname) {
        // ─── Validation ───
        if (username == null || username.trim().isEmpty()) {
            return AuthResult.error("Le nom d'utilisateur est requis");
        }
        if (!USERNAME_PATTERN.matcher(username).matches()) {
            return AuthResult.error("Nom d'utilisateur invalide (3-30 caractères, alphanumériques et _ uniquement)");
        }

        if (email == null || email.trim().isEmpty()) {
            return AuthResult.error("L'email est requis");
        }
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            return AuthResult.error("Format d'email invalide");
        }

        if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
            return AuthResult.error("Le mot de passe doit contenir au moins " + MIN_PASSWORD_LENGTH + " caractères");
        }

        if (nickname == null || nickname.trim().isEmpty()) {
            nickname = username;
        }

        try {
            // ─── Vérifier unicité ───
            Optional<User> existingUsername = userDAO.findByUsername(username);
            if (existingUsername.isPresent()) {
                return AuthResult.error("Ce nom d'utilisateur est déjà utilisé");
            }

            Optional<User> existingEmail = userDAO.findByEmail(email);
            if (existingEmail.isPresent()) {
                return AuthResult.error("Cet email est déjà utilisé");
            }

            // ─── Créer l'utilisateur ───
            String hashedPassword = EncryptionUtil.hashPassword(password);
            User newUser = new User(username, email, hashedPassword, nickname);
            newUser.setStatus(UserStatus.OFFLINE);

            User saved = userDAO.save(newUser);

            LOGGER.info("✅ Inscription réussie : " + username + " (ID=" + saved.getId() + ")");
            return AuthResult.success(saved, "Inscription réussie !");

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erreur SQL lors de l'inscription", e);
            return AuthResult.error("Erreur serveur lors de l'inscription");
        }
    }

    /**
     * Inscription simplifiée (nickname = username par défaut)
     */
    public AuthResult register(String username, String email, String password) {
        return register(username, email, password, username);
    }

    // ═══════════════════════════════════════════════════════════
    // CONNEXION
    // ═══════════════════════════════════════════════════════════

    /**
     * Connecte un utilisateur existant.
     *
     * @param usernameOrEmail  Nom d'utilisateur ou email
     * @param password         Mot de passe en clair
     * @return Result avec le User + token de session ou message d'erreur
     */
    public AuthResult login(String usernameOrEmail, String password) {
        if (usernameOrEmail == null || usernameOrEmail.trim().isEmpty()) {
            return AuthResult.error("Veuillez entrer votre nom d'utilisateur ou email");
        }
        if (password == null || password.isEmpty()) {
            return AuthResult.error("Veuillez entrer votre mot de passe");
        }

        try {
            // ─── Chercher par username ou email ───
            Optional<User> userOpt;
            if (EMAIL_PATTERN.matcher(usernameOrEmail).matches()) {
                userOpt = userDAO.findByEmail(usernameOrEmail);
            } else {
                userOpt = userDAO.findByUsername(usernameOrEmail);
            }

            if (userOpt.isEmpty()) {
                return AuthResult.error("Nom d'utilisateur ou mot de passe incorrect");
            }

            User user = userOpt.get();

            // ─── Vérifier le mot de passe ───
            if (!EncryptionUtil.verifyPassword(password, user.getPassword())) {
                return AuthResult.error("Nom d'utilisateur ou mot de passe incorrect");
            }

            // ─── Déconnecter l'ancienne session si existante ───
            String oldToken = userTokens.get(user.getId());
            if (oldToken != null) {
                activeSessions.remove(oldToken);
                LOGGER.info("🔄 Ancienne session invalide pour " + user.getUsername());
            }

            // ─── Créer une nouvelle session ───
            String token = generateToken();
            user.setStatus(UserStatus.ONLINE);
            user.setLastSeen(LocalDateTime.now());

            // Mettre à jour le statut en base
            userDAO.updateStatus(user.getId(), UserStatus.ONLINE);
            userDAO.updateLastSeen(user.getId());

            // Stocker la session
            activeSessions.put(token, user);
            userTokens.put(user.getId(), token);

            LOGGER.info("🔓 Connexion réussie : " + user.getUsername() + " (ID=" + user.getId() + ")");
            return AuthResult.success(user, token, "Connexion réussie !");

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erreur SQL lors de la connexion", e);
            return AuthResult.error("Erreur serveur lors de la connexion");
        }
    }

    // ═══════════════════════════════════════════════════════════
    // DÉCONNEXION
    // ═══════════════════════════════════════════════════════════

    /**
     * Déconnecte un utilisateur par son token de session.
     */
    public void logout(String token) {
        if (token == null) return;

        User user = activeSessions.remove(token);
        if (user != null) {
            userTokens.remove(user.getId());

            try {
                userDAO.updateStatus(user.getId(), UserStatus.OFFLINE);
                userDAO.updateLastSeen(user.getId());
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Erreur mise à jour statut logout", e);
            }

            LOGGER.info("👋 Déconnexion : " + user.getUsername());
        }
    }

    /**
     * Déconnecte un utilisateur par son ID.
     */
    public void logoutByUserId(Long userId) {
        String token = userTokens.get(userId);
        if (token != null) {
            logout(token);
        }
    }

    // ═══════════════════════════════════════════════════════════
    // SESSIONS
    // ═══════════════════════════════════════════════════════════

    /**
     * Récupère l'utilisateur associé à un token.
     */
    public Optional<User> getUserByToken(String token) {
        return Optional.ofNullable(activeSessions.get(token));
    }

    /**
     * Vérifie si un token est valide.
     */
    public boolean isTokenValid(String token) {
        return token != null && activeSessions.containsKey(token);
    }

    /**
     * Récupère l'utilisateur actuellement connecté (pour le client local).
     */
    public Optional<User> getCurrentUser(String token) {
        return getUserByToken(token);
    }

    /**
     * Retourne le nombre de sessions actives.
     */
    public int getActiveSessionCount() {
        return activeSessions.size();
    }

    /**
     * Liste tous les utilisateurs connectés.
     */
    public Map<String, User> getActiveSessions() {
        return new ConcurrentHashMap<>(activeSessions);
    }

    // ═══════════════════════════════════════════════════════════
    // UTILITAIRES
    // ═══════════════════════════════════════════════════════════

    private String generateToken() {
        return UUID.randomUUID().toString().replace("-", "") +
                System.currentTimeMillis();
    }

    // ═══════════════════════════════════════════════════════════
    // CLASSE RÉSULTAT
    // ═══════════════════════════════════════════════════════════

    /**
     * Résultat d'une opération d'authentification.
     */
    public static class AuthResult {
        private final boolean success;
        private final User user;
        private final String token;
        private final String message;

        private AuthResult(boolean success, User user, String token, String message) {
            this.success = success;
            this.user = user;
            this.token = token;
            this.message = message;
        }

        public static AuthResult success(User user, String message) {
            return new AuthResult(true, user, null, message);
        }

        public static AuthResult success(User user, String token, String message) {
            return new AuthResult(true, user, token, message);
        }

        public static AuthResult error(String message) {
            return new AuthResult(false, null, null, message);
        }

        // Getters
        public boolean isSuccess() { return success; }
        public User getUser() { return user; }
        public String getToken() { return token; }
        public String getMessage() { return message; }

        public boolean hasToken() { return token != null && !token.isEmpty(); }

        @Override
        public String toString() {
            return "AuthResult{success=" + success +
                    ", user=" + (user != null ? user.getUsername() : "null") +
                    ", message='" + message + "'}";
        }
    }
}