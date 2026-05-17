package com.wechat.service;

import com.wechat.model.User;
import com.wechat.service.AuthService.AuthResult;

import java.util.Scanner;
import java.util.logging.Logger;

/**
 * Console de test du AuthService.
 * Permet de tester : inscription, connexion, déconnexion, sessions.
 *
 * Prérequis : Base de données MySQL démarrée avec tables créées.
 */
public class AuthServiceConsoleTest {

    private static final Logger LOGGER = Logger.getLogger(AuthServiceConsoleTest.class.getName());

    public static void main(String[] args) {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  🔐 CONSOLE TEST - AuthService WeChat");
        System.out.println("═══════════════════════════════════════════════════\n");

        AuthService authService = new AuthService();
        Scanner scanner = new Scanner(System.in);
        String currentToken = null;

        boolean running = true;
        while (running) {
            printMenu(currentToken);
            System.out.print("> ");
            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1" -> {
                    // Inscription
                    System.out.println("\n─── 📝 INSCRIPTION ───");
                    System.out.print("Nom d'utilisateur : ");
                    String username = scanner.nextLine();
                    System.out.print("Email : ");
                    String email = scanner.nextLine();
                    System.out.print("Mot de passe (min 6 caractères) : ");
                    String password = scanner.nextLine();
                    System.out.print("Pseudo (laisser vide = username) : ");
                    String nickname = scanner.nextLine();
                    if (nickname.isEmpty()) nickname = username;

                    AuthResult result = authService.register(username, email, password, nickname);

                    if (result.isSuccess()) {
                        System.out.println("✅ " + result.getMessage());
                        System.out.println("   User créé : " + result.getUser().getUsername() +
                                " (ID=" + result.getUser().getId() + ")");
                    } else {
                        System.out.println("❌ " + result.getMessage());
                    }
                }

                case "2" -> {
                    // Connexion
                    System.out.println("\n─── 🔐 CONNEXION ───");
                    System.out.print("Username ou Email : ");
                    String login = scanner.nextLine();
                    System.out.print("Mot de passe : ");
                    String password = scanner.nextLine();

                    AuthResult result = authService.login(login, password);

                    if (result.isSuccess()) {
                        currentToken = result.getToken();
                        System.out.println("✅ " + result.getMessage());
                        System.out.println("   Token : " + currentToken.substring(0, 20) + "...");
                        System.out.println("   User  : " + result.getUser().getNickname() +
                                " (ID=" + result.getUser().getId() + ")");
                        System.out.println("   Statut : " + result.getUser().getStatus());
                    } else {
                        System.out.println("❌ " + result.getMessage());
                    }
                }

                case "3" -> {
                    // Vérifier session
                    if (currentToken == null) {
                        System.out.println("❌ Aucune session active");
                        break;
                    }

                    var userOpt = authService.getUserByToken(currentToken);
                    if (userOpt.isPresent()) {
                        User u = userOpt.get();
                        System.out.println("✅ Session valide");
                        System.out.println("   User : " + u.getUsername() + " (ID=" + u.getId() + ")");
                        System.out.println("   Email : " + u.getEmail());
                        System.out.println("   Statut : " + u.getStatus());
                    } else {
                        System.out.println("❌ Session invalide ou expirée");
                        currentToken = null;
                    }
                }

                case "4" -> {
                    // Déconnexion
                    if (currentToken == null) {
                        System.out.println("❌ Aucune session active");
                        break;
                    }

                    authService.logout(currentToken);
                    System.out.println("👋 Déconnecté");
                    currentToken = null;
                }

                case "5" -> {
                    // Stats sessions
                    System.out.println("📊 Statistiques sessions :");
                    System.out.println("   Sessions actives : " + authService.getActiveSessionCount());
                    System.out.println("   Utilisateurs connectés :");
                    authService.getActiveSessions().forEach((token, user) -> {
                        System.out.println("      → " + user.getUsername() + " (ID=" + user.getId() + ")");
                    });
                }

                case "6" -> {
                    // Test hashage
                    System.out.println("\n─── 🔒 TEST HASHAGE ───");
                    System.out.print("Mot de passe à hasher : ");
                    String pwd = scanner.nextLine();

                    String hash1 = com.wechat.common.EncryptionUtil.hashPassword(pwd);
                    String hash2 = com.wechat.common.EncryptionUtil.hashPassword(pwd);

                    System.out.println("Hash 1 : " + hash1);
                    System.out.println("Hash 2 : " + hash2);
                    System.out.println("Différents (salt unique) : " + !hash1.equals(hash2));
                    System.out.println("Vérification hash1 : " +
                            com.wechat.common.EncryptionUtil.verifyPassword(pwd, hash1));
                    System.out.println("Vérification faux MDP : " +
                            !com.wechat.common.EncryptionUtil.verifyPassword("faux", hash1));
                }

                case "0", "q" -> {
                    running = false;
                    if (currentToken != null) {
                        authService.logout(currentToken);
                    }
                    System.out.println("👋 Au revoir !");
                }

                default -> System.out.println("❓ Option invalide");
            }
        }

        scanner.close();
    }

    private static void printMenu(String token) {
        System.out.println("\n┌─────────────────────────────────────────────────┐");
        System.out.println("│  🔐 MENU AUTH SERVICE                           │");
        System.out.println("├─────────────────────────────────────────────────┤");
        System.out.println("│  1. 📝 Inscription                              │");
        System.out.println("│  2. 🔐 Connexion                                │");
        System.out.println("│  3. 👤 Vérifier session                         │");
        System.out.println("│  4. 🔌 Déconnexion                              │");
        System.out.println("│  5. 📊 Stats sessions                           │");
        System.out.println("│  6. 🔒 Test hashage                             │");
        System.out.println("│  0. ❌ Quitter                                  │");
        System.out.println("└─────────────────────────────────────────────────┘");
        System.out.println("   Session : " + (token != null ? "✅ Connecté" : "❌ Déconnecté"));
    }
}