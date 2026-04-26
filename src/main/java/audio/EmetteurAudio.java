package audio;

import commun.Protocole;

import java.io.*;
import java.net.Socket;

/**
 * Envoie l'audio capturé vers le serveur relais TCP.
 */
public class EmetteurAudio {

    private Socket socket;
    private OutputStream sortie;
    private CapteurAudio capteur;
    private volatile boolean enCours = false;
    private Thread threadEnvoi;

    public void demarrer(String serveurIp) throws Exception {
        this.socket = new Socket(serveurIp, Protocole.PORT_AUDIO);
        this.sortie = socket.getOutputStream();
        this.capteur = new CapteurAudio();
        capteur.demarrer();
        enCours = true;

        threadEnvoi = new Thread(() -> {
            while (enCours) {
                try {
                    byte[] chunk = capteur.lireChunk();
                    sortie.write(chunk);
                    sortie.flush();
                } catch (IOException e) {
                    enCours = false;
                }
            }
        }, "Thread-Envoi-Audio");

        threadEnvoi.setDaemon(true);
        threadEnvoi.start();
    }

    public void arreter() {
        enCours = false;
        capteur.arreter();
        try {
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException ignored) {}
    }
}