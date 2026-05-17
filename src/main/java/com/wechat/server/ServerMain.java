package com.wechat.server;

import java.util.Scanner;
import java.util.logging.Logger;

/**
 * Point d'entrée du serveur WeChat.
 * Lance le serveur et fournit une console d'administration.
 */
public class ServerMain {

    private static final Logger LOGGER = Logger.getLogger(ServerMain.class.getName());

    public static void main(String[] args) {
        ChatServer server = new ChatServer();

        // Hook d'arrêt propre (Ctrl+C)
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOGGER.info("🛑 Signal d'arrêt reçu (Ctrl+C)");
            server.stop();
        }));

        // Démarrer le serveur dans un thread séparé
        Thread serverThread = new Thread(server::start);
        serverThread.setName("ChatServer-Main");
        serverThread.start();

        // Console d'administration
        Scanner scanner = new Scanner(System.in);
        LOGGER.info("\n💻 Console d'administration du serveur");
        LOGGER.info("Commandes : status | clients | stop | help\n");

        while (server.isRunning()) {
            System.out.print("[wechat-server] > ");
            String command = scanner.nextLine().trim().toLowerCase();

            switch (command) {
                case "status" -> {
                    System.out.println("📊 Statut serveur : " + (server.isRunning() ? "EN LIGNE" : "ARRÊTÉ"));
                    System.out.println("👥 Clients connectés : " + server.getConnectedCount());
                }
                case "clients" -> {
                    System.out.println("📋 Clients connectés (" + server.getConnectedCount() + ") :");
                    System.out.println("   IDs : " + server.getOnlineUserIds());
                }
                case "stop" -> {
                    System.out.println("🛑 Arrêt demandé...");
                    server.stop();
                    break;
                }
                case "help" -> {
                    System.out.println("Commandes disponibles :");
                    System.out.println("  status  - Statut du serveur");
                    System.out.println("  clients - Liste des clients connectés");
                    System.out.println("  stop    - Arrêter le serveur");
                    System.out.println("  help    - Cette aide");
                }
                default -> System.out.println("❓ Commande inconnue. Tapez 'help' pour la liste.");
            }
        }

        scanner.close();
        LOGGER.info("👋 Au revoir !");
    }
}