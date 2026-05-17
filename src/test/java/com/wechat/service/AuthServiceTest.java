package com.wechat.service;

import com.wechat.common.EncryptionUtil;
import com.wechat.dao.UserDAO;
import com.wechat.model.User;
import com.wechat.service.AuthService.AuthResult;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.sql.SQLException;
import java.util.Optional;

/**
 * Tests JUnit du AuthService.
 * Teste inscription, connexion, logout, sessions, hashage.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AuthServiceTest {

    private static AuthService authService;
    private static UserDAO userDAO;
    private static String testToken;
    private static Long testUserId;

    @BeforeAll
    static void setUp() {
        authService = new AuthService();
        userDAO = new UserDAO();
    }

    @Test
    @Order(1)
    @DisplayName("Inscription réussie")
    void testRegisterSuccess() {
        AuthResult result = authService.register(
                "testuser_auth", "test_auth@wechat.com", "password123", "Test User"
        );

        assertTrue(result.isSuccess(), "L'inscription doit réussir : " + result.getMessage());
        assertNotNull(result.getUser(), "L'utilisateur doit être créé");
        assertNotNull(result.getUser().getId(), "L'ID doit être généré");
        assertEquals("testuser_auth", result.getUser().getUsername());

        testUserId = result.getUser().getId();
        System.out.println("✅ testRegisterSuccess : User ID=" + testUserId);
    }

    @Test
    @Order(2)
    @DisplayName("Inscription échoue - username déjà pris")
    void testRegisterDuplicateUsername() {
        AuthResult result = authService.register(
                "testuser_auth", "autre@wechat.com", "password123", "Autre"
        );

        assertFalse(result.isSuccess(), "L'inscription doit échouer");
        assertTrue(result.getMessage().contains("déjà utilisé"));
        System.out.println("✅ testRegisterDuplicateUsername : " + result.getMessage());
    }

    @Test
    @Order(3)
    @DisplayName("Inscription échoue - email déjà pris")
    void testRegisterDuplicateEmail() {
        AuthResult result = authService.register(
                "autreuser", "test_auth@wechat.com", "password123", "Autre"
        );

        assertFalse(result.isSuccess(), "L'inscription doit échouer");
        assertTrue(result.getMessage().contains("déjà utilisé"));
        System.out.println("✅ testRegisterDuplicateEmail : " + result.getMessage());
    }

    @Test
    @Order(4)
    @DisplayName("Inscription échoue - email invalide")
    void testRegisterInvalidEmail() {
        AuthResult result = authService.register(
                "validuser", "pas-un-email", "password123", "Valid"
        );

        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("email"));
        System.out.println("✅ testRegisterInvalidEmail : " + result.getMessage());
    }

    @Test
    @Order(5)
    @DisplayName("Inscription échoue - mot de passe trop court")
    void testRegisterShortPassword() {
        AuthResult result = authService.register(
                "validuser2", "valid2@wechat.com", "123", "Valid"
        );

        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("mot de passe"));
        System.out.println("✅ testRegisterShortPassword : " + result.getMessage());
    }

    @Test
    @Order(6)
    @DisplayName("Connexion réussie par username")
    void testLoginByUsername() {
        AuthResult result = authService.login("testuser_auth", "password123");

        assertTrue(result.isSuccess(), "La connexion doit réussir : " + result.getMessage());
        assertNotNull(result.getUser());
        assertTrue(result.hasToken(), "Un token doit être généré");
        assertNotNull(result.getToken());

        // Stocker le token pour les tests suivants
        testToken = result.getToken();
        System.out.println("✅ testLoginByUsername : Token=" + testToken.substring(0, 20) + "...");
    }

    @Test
    @Order(7)
    @DisplayName("Connexion réussie par email (nouveau token)")
    void testLoginByEmail() {
        AuthResult result = authService.login("test_auth@wechat.com", "password123");

        assertTrue(result.isSuccess(), "La connexion par email doit réussir");
        assertNotNull(result.getUser());
        // Ne PAS écraser testToken car ce nouveau login invalide l'ancien
        System.out.println("✅ testLoginByEmail : Connecté par email (nouveau token généré)");
    }

    @Test
    @Order(8)
    @DisplayName("Connexion échoue - mauvais mot de passe")
    void testLoginWrongPassword() {
        AuthResult result = authService.login("testuser_auth", "mauvais_mdp");

        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("incorrect"));
        System.out.println("✅ testLoginWrongPassword : " + result.getMessage());
    }

    @Test
    @Order(9)
    @DisplayName("Connexion échoue - utilisateur inconnu")
    void testLoginUnknownUser() {
        AuthResult result = authService.login("inconnu12345", "password123");

        assertFalse(result.isSuccess());
        System.out.println("✅ testLoginUnknownUser : " + result.getMessage());
    }

    @Test
    @Order(10)
    @DisplayName("Session valide après connexion")
    void testValidSession() {
        assertNotNull(testToken, "Le token doit exister (du premier login)");

        // Se reconnecter pour obtenir un token valide (le précédent a été invalidé)
        AuthResult result = authService.login("testuser_auth", "password123");
        assertTrue(result.isSuccess(), "Reconnexion nécessaire pour obtenir un token valide");
        testToken = result.getToken();  // Mettre à jour avec le nouveau token valide

        Optional<User> userOpt = authService.getUserByToken(testToken);
        assertTrue(userOpt.isPresent(), "La session doit être valide");
        assertEquals("testuser_auth", userOpt.get().getUsername());

        System.out.println("✅ testValidSession : Session OK pour " + userOpt.get().getUsername());
    }

    @Test
    @Order(11)
    @DisplayName("Token invalide rejeté")
    void testInvalidToken() {
        Optional<User> userOpt = authService.getUserByToken("token_invalide_12345");
        assertFalse(userOpt.isPresent(), "Un token invalide doit être rejeté");
        System.out.println("✅ testInvalidToken : Token invalide rejeté");
    }

    @Test
    @Order(12)
    @DisplayName("Sessions actives comptées")
    void testActiveSessions() {
        int count = authService.getActiveSessionCount();
        assertTrue(count >= 1, "Au moins 1 session doit être active");
        System.out.println("✅ testActiveSessions : " + count + " session(s) active(s)");
    }

    @Test
    @Order(13)
    @DisplayName("Déconnexion")
    void testLogout() {
        assertNotNull(testToken);

        authService.logout(testToken);

        Optional<User> userOpt = authService.getUserByToken(testToken);
        assertFalse(userOpt.isPresent(), "La session doit être supprimée après logout");

        System.out.println("✅ testLogout : Session supprimée");
    }

    @Test
    @Order(14)
    @DisplayName("Hashage mot de passe - salts différents")
    void testPasswordHashing() {
        String password = "monMotDePasse123";

        String hash1 = EncryptionUtil.hashPassword(password);
        String hash2 = EncryptionUtil.hashPassword(password);

        assertNotEquals(hash1, hash2, "Deux hash du même MDP doivent être différents (salt unique)");
        assertTrue(EncryptionUtil.verifyPassword(password, hash1), "Vérification doit réussir");
        assertFalse(EncryptionUtil.verifyPassword("mauvais", hash1), "Vérification doit échouer");

        System.out.println("✅ testPasswordHashing : Hashage sécurisé OK");
    }

    @AfterAll
    static void cleanUp() throws SQLException {
        // Nettoyer l'utilisateur de test
        if (testUserId != null) {
            userDAO.delete(testUserId);
            System.out.println("🧹 Nettoyage : User test supprimé (ID=" + testUserId + ")");
        }
    }
}