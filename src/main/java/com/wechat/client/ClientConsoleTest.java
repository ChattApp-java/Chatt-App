package com.wechat.client;

import com.wechat.common.Protocol;
import com.wechat.model.User;

import java.util.Scanner;
import java.util.logging.Logger;

/**
 * Console de test du client réseau.
 * Permet de tester : connexion, auth, envoi de messages, création de groupes.
 *
 * Lancer en parallèle du serveur :
 *   1. Terminal 1 : ServerMain
 *   2. Terminal 2 : ClientConsoleTest
 *
 * Ou lancer 2 instances de ClientConsoleTest pour chatter entre elles.
 */
public class ClientConsoleTest {

    private static final Logger LOGGER = Logger.getLogger(ClientConsoleTest.class.getName());

    public static void main(String[] args) {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  🖥️  CONSOLE CLIENT WECHAT - Test Réseau");
        System.out.println("═══════════════════════════════════════════════════\n");

        NetworkClient client = NetworkClient.getInstance();
        Scanner scanner = new Scanner(System.in);

        // ─── Configurer les listeners ───
        client.setConnectionListener(connected -> {
            // Ne rien faire ici, le menu s'affiche dans la boucle
        });

        client.setMessageListener(msg -> {
            // Afficher les messages reçus proprement
            System.out.println("\n┌─── MESSAGE REÇU ───");
            System.out.println("│ Type : " + msg.getType());
            System.out.println("│ De   : " + msg.getSenderId());
            if (msg.getContent() != null) {
                System.out.println("│ Msg  : " + msg.getContent());
            }
            System.out.println("└─────────────────────");
        });

        client.setUserStatusListener(user -> {
            System.out.println("\n[👤] Statut user " + user.getId() + " : " + user.getStatus());
        });

        client.setErrorListener(error -> {
            System.out.println("\n[❌] Erreur : " + error);
        });

        client.setTypingListener(info -> {
            System.out.println("\n[✏️ ] " + info);
        });

        client.setDisconnectListener(() -> {
            System.out.println("\n[🔌] Déconnecté du serveur");
        });

        // ─── Menu principal ───
        boolean running = true;
        while (running) {
            printMenu(client);
            System.out.print("> ");
            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1" -> {
                    // Connexion
                    System.out.println("🔌 Connexion au serveur " + Protocol.SERVER_HOST + ":" + Protocol.SERVER_PORT);
                    boolean ok = client.connect();
                    if (!ok) {
                        System.out.println("❌ Échec de connexion. Le serveur est-il démarré ?");
                    } else {
                        System.out.println("✅ Connecté !");
                    }
                }

                case "2" -> {
                    // Authentification
                    if (!client.isConnected()) {
                        System.out.println("❌ Connectez-vous d'abord (option 1)");
                        break;
                    }
                    System.out.print("Email/Username : ");
                    String email = scanner.nextLine();
                    System.out.print("Password : ");
                    String password = scanner.nextLine();

                    User user = client.authenticate(email, password);
                    if (user != null) {
                        System.out.println("✅ Authentifié ! ID=" + user.getId() + " | " + user.getUsername());
                    } else {
                        System.out.println("❌ Échec d'authentification");
                    }
                }

                case "3" -> {
                    // Inscription
                    if (!client.isConnected()) {
                        System.out.println("❌ Connectez-vous d'abord (option 1)");
                        break;
                    }
                    System.out.print("Nom : ");
                    String name = scanner.nextLine();
                    System.out.print("Email : ");
                    String email = scanner.nextLine();
                    System.out.print("Password : ");
                    String password = scanner.nextLine();

                    boolean ok = client.register(name, email, password);
                    if (ok) {
                        System.out.println("✅ Inscription réussie ! Connectez-vous (option 2)");
                    } else {
                        System.out.println("❌ Échec d'inscription");
                    }
                }

                case "4" -> {
                    // Envoyer message
                    if (!client.isAuthenticated()) {
                        System.out.println("❌ Authentifiez-vous d'abord (option 2)");
                        break;
                    }
                    System.out.print("ID du destinataire : ");
                    Long receiverId = Long.parseLong(scanner.nextLine());
                    System.out.print("Message : ");
                    String text = scanner.nextLine();

                    client.sendTextMessage(receiverId, text);
                    System.out.println("📤 Message envoyé !");
                }

                case "5" -> {
                    // Envoyer message groupe
                    if (!client.isAuthenticated()) {
                        System.out.println("❌ Authentifiez-vous d'abord");
                        break;
                    }
                    System.out.print("ID du groupe : ");
                    Long groupId = Long.parseLong(scanner.nextLine());
                    System.out.print("Message : ");
                    String text = scanner.nextLine();

                    client.sendGroupMessage(groupId, text);
                    System.out.println("📤 Message de groupe envoyé !");
                }

                case "6" -> {
                    // Créer groupe
                    if (!client.isAuthenticated()) {
                        System.out.println("❌ Authentifiez-vous d'abord");
                        break;
                    }
                    System.out.print("Nom du groupe : ");
                    String groupName = scanner.nextLine();
                    System.out.print("IDs des membres (séparés par virgule) : ");
                    String membersStr = scanner.nextLine();

                    java.util.List<Long> members = new java.util.ArrayList<>();
                    for (String s : membersStr.split(",")) {
                        members.add(Long.parseLong(s.trim()));
                    }

                    client.createGroup(groupName, members);
                    // La notification de succès arrivera via le listener
                    System.out.println("⏳ Demande envoyée, en attente de confirmation...");
                }

                case "7" -> {
                    // Typing
                    if (!client.isAuthenticated()) {
                        System.out.println("❌ Authentifiez-vous d'abord");
                        break;
                    }
                    System.out.print("ID conversation : ");
                    Long convId = Long.parseLong(scanner.nextLine());
                    client.sendTyping(convId);
                    System.out.println("✏️  Notification 'typing' envoyée");
                }

                case "8" -> {
                    // Déconnexion
                    client.disconnect();
                    System.out.println("👋 Déconnecté");
                }

                case "9" -> {
                    // Statut
                    System.out.println("\n📊 Statut client :");
                    System.out.println("   Connecté      : " + (client.isConnected() ? "✅ OUI" : "❌ NON"));
                    System.out.println("   Authentifié   : " + (client.isAuthenticated() ? "✅ OUI" : "❌ NON"));
                    if (client.getCurrentUser() != null) {
                        System.out.println("   User ID       : " + client.getCurrentUser().getId());
                        System.out.println("   Username      : " + client.getCurrentUser().getUsername());
                    }
                }

                case "0", "q", "quit" -> {
                    running = false;
                    client.disconnect();
                    System.out.println("👋 Au revoir !");
                }

                default -> System.out.println("❓ Option invalide");
            }
        }

        scanner.close();
    }

    private static void printMenu(NetworkClient client) {
        String connStatus = client.isConnected() ? "🟢 CONNECTÉ" : "🔴 DÉCONNECTÉ";
        String authStatus = client.isAuthenticated() ? "✅ AUTHENTIFIÉ" : "❌ NON AUTHENTIFIÉ";

        System.out.println("\n┌─────────────────────────────────────────────────┐");
        System.out.println("│  🖥️  MENU CLIENT                                │");
        System.out.println("├─────────────────────────────────────────────────┤");
        System.out.println("│  1. 🔌 Connecter au serveur                     │");
        System.out.println("│  2. 🔐 S'authentifier                           │");
        System.out.println("│  3. 📝 S'inscrire                               │");
        System.out.println("│  4. 💬 Envoyer message privé                    │");
        System.out.println("│  5. 👥 Envoyer message groupe                   │");
        System.out.println("│  6. ➕ Créer un groupe                          │");
        System.out.println("│  7. ✏️  Envoyer 'typing'                        │");
        System.out.println("│  8. 🔌 Se déconnecter                           │");
        System.out.println("│  9. 📊 Statut                                   │");
        System.out.println("│  0. ❌ Quitter                                  │");
        System.out.println("└─────────────────────────────────────────────────┘");
        System.out.println("   [" + connStatus + "] | [" + authStatus + "]");
    }
}