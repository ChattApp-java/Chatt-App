package client;

import commun.Protocole;
import frontend.MainChatApp;
import javafx.application.Platform;

import java.io.IOException;

/**
 * Thread d'écoute des messages du serveur.
 */
public class EcouteurMessages implements Runnable {

    private final ConnexionServeur connexion;
    private final MainChatApp app;
    private volatile boolean running = true;

    public EcouteurMessages(ConnexionServeur connexion, MainChatApp app) {
        this.connexion = connexion;
        this.app = app;
    }

    @Override
    public void run() {
        try {
            String line;
            while (running && (line = connexion.lire()) != null) {
                String[] parts = line.split("\\" + Protocole.SEP);
                if (parts.length == 0) continue;
                String type = parts[0];

                switch (type) {
                    case Protocole.USER_LIST -> Platform.runLater(() -> {
                        app.mettreAJourListe(parts.length > 1 ? parts[1].split(",") : new String[0]);
                    });

                    case Protocole.MSG_RECV -> Platform.runLater(() -> {
                        if (parts.length >= 3) app.ajouterMessage(parts[1], parts[2], false);
                    });

                    case Protocole.USER_JOINED -> Platform.runLater(() -> {
                        if (parts.length > 1) {
                            app.ajouterUtilisateur(parts[1]);
                            app.ajouterMessageSysteme(parts[1] + " a rejoint le chat");
                        }
                    });

                    case Protocole.USER_LEFT -> Platform.runLater(() -> {
                        if (parts.length > 1) {
                            app.retirerUtilisateur(parts[1]);
                            app.ajouterMessageSysteme(parts[1] + " a quitté le chat");
                        }
                    });

                    case Protocole.CALL_INCOMING -> Platform.runLater(() -> {
                        if (parts.length >= 3) app.gererAppelEntrant(parts[1], parts[2]);
                    });

                    case Protocole.CALL_ACCEPTED -> Platform.runLater(() -> {
                        app.appelAccepte(parts);
                    });

                    case Protocole.CALL_REJECTED -> Platform.runLater(() -> {
                        if (parts.length > 1) app.appelRefuse(parts[1]);
                    });

                    case Protocole.CALL_ENDED -> Platform.runLater(() -> {
                        app.appelTermine();
                    });

                    case Protocole.FILE_INFO -> Platform.runLater(() -> {
                        if (parts.length >= 4) {
                            app.ajouterMessageSysteme("Fichier reçu : " + parts[2] + " (" + parts[3] + " bytes)");
                        }
                    });
                }
            }
        } catch (IOException e) {
            if (running) {
                Platform.runLater(() -> app.ajouterMessageSysteme("Connexion perdue."));
            }
        }
    }

    public void arreter() {
        running = false;
    }
}