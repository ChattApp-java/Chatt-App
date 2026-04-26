package video;

import commun.Protocole;

import java.io.DataOutputStream;
import java.net.Socket;

/**
 * Envoie le flux vidéo vers le serveur relais TCP.
 */
public class EmetteurVideo {

    private Socket socket;
    private DataOutputStream sortie;
    private CapteurVideo capteur;
    private volatile boolean enCours = false;
    private Thread threadEnvoi;

    public void demarrer(String serveurIp) throws Exception {
        this.socket = new Socket(serveurIp, Protocole.PORT_VIDEO);
        this.sortie = new DataOutputStream(socket.getOutputStream());
        this.capteur = new CapteurVideo();
        capteur.demarrer();
        enCours = true;

        threadEnvoi = new Thread(() -> {
            while (enCours) {
                try {
                    byte[] image = capteur.capturerImage();
                    if (image != null) {
                        sortie.writeInt(image.length);
                        sortie.write(image);
                        sortie.flush();
                    }
                    Thread.sleep(50); // ~20 fps
                } catch (Exception e) {
                    enCours = false;
                }
            }
        }, "Thread-Envoi-Video");

        threadEnvoi.setDaemon(true);
        threadEnvoi.start();
    }

    public void arreter() {
        enCours = false;
        capteur.arreter();
        try {
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (Exception ignored) {}
    }
}