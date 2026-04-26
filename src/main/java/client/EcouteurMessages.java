package client;

import commun.Protocole;
import frontend.FenetreChat;

import javax.swing.*;
import java.io.IOException;

/**
 * Thread d'écoute des messages du serveur.
 */
public class EcouteurMessages implements Runnable {

    private final ConnexionServeur connexion;
    private final FenetreChat fenetre;
    private volatile boolean running = true;

    public EcouteurMessages(ConnexionServeur connexion, FenetreChat fenetre) {
        this.connexion = connexion;
        this.fenetre = fenetre;
    }

    @Override
    public void run() {
        try {
            String line;
            while (running && (line = connexion.lire()) != null) {
                String[] parts = line.split("\\" + Protocole.SEP);
                String type = parts[0];

                switch (type) {
                    case Protocole.USER_LIST -> SwingUtilities.invokeLater(() -> {
                        fenetre.mettreAJourListe(parts.length > 1 ? parts[1].split(",") : new String[0]);
                    });

                    case Protocole.MSG_RECV -> SwingUtilities.invokeLater(() -> {
                        if (parts.length >= 3) fenetre.ajouterMessage(parts[1], parts[2], false);
                    });

                    case Protocole.USER_JOINED -> SwingUtilities.invokeLater(() -> {
                        if (parts.length > 1) {
                            fenetre.ajouterUtilisateur(parts[1]);
                            fenetre.ajouterMessageSysteme(parts[1] + " a rejoint le chat");
                        }
                    });

                    case Protocole.USER_LEFT -> SwingUtilities.invokeLater(() -> {
                        if (parts.length > 1) {
                            fenetre.retirerUtilisateur(parts[1]);
                            fenetre.ajouterMessageSysteme(parts[1] + " a quitté le chat");
                        }
                    });

                    case Protocole.CALL_INCOMING -> SwingUtilities.invokeLater(() -> {
                        if (parts.length >= 3) fenetre.gererAppelEntrant(parts[1], parts[2]);
                    });

                    case Protocole.CALL_ACCEPTED -> SwingUtilities.invokeLater(() -> {
                        fenetre.appelAccepte(parts);
                    });

                    case Protocole.CALL_REJECTED -> SwingUtilities.invokeLater(() -> {
                        if (parts.length > 1) fenetre.appelRefuse(parts[1]);
                    });

                    case Protocole.CALL_ENDED -> SwingUtilities.invokeLater(() -> {
                        fenetre.appelTermine();
                    });
                }
            }
        } catch (IOException e) {
            if (running) {
                SwingUtilities.invokeLater(() -> fenetre.ajouterMessageSysteme("Connexion perdue."));
            }
        }
    }

    public void arreter() {
        running = false;
    }
}