package com.chatapp.client.classic;

import javax.swing.SwingUtilities;

public class Main {

    public static void main(String[] args) {
        // Lancement de l'interface graphique de manière sécurisée
        SwingUtilities.invokeLater(() -> {
            try {
                // On instancie la fenêtre de connexion pour démarrer l'application
                new FenetreConnexion();
            } catch (Exception e) {
                System.err.println("Erreur lors du lancement de l'application : " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
}

