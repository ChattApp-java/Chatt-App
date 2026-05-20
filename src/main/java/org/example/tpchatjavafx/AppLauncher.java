package org.example.tpchatjavafx;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class AppLauncher {

    public static void main(String[] args) {
        System.out.println("==========================================");
        System.out.println("   WHATSAPP JAVA - LANCEUR COMPLET");
        System.out.println("==========================================\n");

        try {
            // Lancer le serveur dans un thread séparé
            System.out.println("[1/3] Demarrage du SERVEUR...");
            Thread serverThread = new Thread(() -> {
                try {
                    org.example.tpchatjavafx.server.ServerLauncher.main(args);
                } catch (Exception e) {
                    System.err.println("Erreur serveur: " + e.getMessage());
                }
            });
            serverThread.start();

            // Attendre 5 secondes que le serveur démarre
            System.out.println("[2/3] Attente du serveur (5s)...");
            Thread.sleep(5000);

            // Lancer le client
            System.out.println("[3/3] Demarrage du CLIENT...\n");
            org.example.tpchatjavafx.client.MainLauncher.main(args);

        } catch (Exception e) {
            System.err.println("Erreur lancement: " + e.getMessage());
            e.printStackTrace();
        }
    }
}