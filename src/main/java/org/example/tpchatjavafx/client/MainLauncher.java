package org.example.tpchatjavafx.client;

/**
 * Lanceur auxiliaire pour contourner les restrictions du module-path de JavaFX.
 * Cette classe ne doit PAS étendre Application.
 */
public class MainLauncher {
    public static void main(String[] args) {
        // Appelle le main de l'application réelle
        ChatClientApp.main(args);
    }
}
