import javax.sound.sampled.*;
import java.net.*;
import java.io.*;


public class EmetteurAudio {

    private Socket socket;
    private OutputStream sortie;
    private CapteurAudio capteur;
    private volatile boolean enCours = false;
    private Thread threadEnvoi;

    // Appelé par GestionnaireAppelAudio avec le socket déjà ouvert
    public void demarrer(Socket socketExistant) throws Exception {
        this.socket = socketExistant;
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
    }
}