package audio;

import commun.Protocole;

import java.io.*;
import java.net.Socket;

/**
 * Reçoit l'audio du serveur relais TCP et le joue.
 */
public class RecepteurAudio {

    private Socket socket;
    private InputStream entree;
    private LecteurAudio lecteur;
    private volatile boolean enCours = false;
    private Thread threadReception;

    public void demarrer(String serveurIp) throws Exception {
        this.socket = new Socket(serveurIp, Protocole.PORT_AUDIO);
        this.entree = socket.getInputStream();
        this.lecteur = new LecteurAudio();
        lecteur.demarrer();
        enCours = true;

        threadReception = new Thread(() -> {
            byte[] buffer = new byte[CapteurAudio.CHUNK_SIZE];
            while (enCours) {
                try {
                    int total = 0;
                    while (total < CapteurAudio.CHUNK_SIZE) {
                        int lu = entree.read(buffer, total, CapteurAudio.CHUNK_SIZE - total);
                        if (lu == -1) throw new IOException("Connexion fermée");
                        total += lu;
                    }
                    lecteur.jouer(buffer);
                } catch (IOException e) {
                    enCours = false;
                }
            }
        }, "Thread-Reception-Audio");

        threadReception.setDaemon(true);
        threadReception.start();
    }

    public void arreter() {
        enCours = false;
        lecteur.arreter();
        try {
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException ignored) {}
    }
}