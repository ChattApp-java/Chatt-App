package video;

import commun.Protocole;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.Socket;

/**
 * Reçoit le flux vidéo du serveur relais TCP et l'affiche.
 */
public class RecepteurVideo {

    private Socket socket;
    private DataInputStream entree;
    private volatile boolean enCours = false;
    private JLabel ecran;
    private Thread threadReception;

    public void demarrer(String serveurIp, JLabel ecran) throws Exception {
        this.socket = new Socket(serveurIp, Protocole.PORT_VIDEO);
        this.entree = new DataInputStream(socket.getInputStream());
        this.ecran = ecran;
        enCours = true;

        threadReception = new Thread(() -> {
            while (enCours) {
                try {
                    int taille = entree.readInt();
                    byte[] donnees = new byte[taille];
                    entree.readFully(donnees);

                    BufferedImage image = ImageIO.read(new ByteArrayInputStream(donnees));
                    if (image != null) {
                        SwingUtilities.invokeLater(() -> {
                            ecran.setIcon(new ImageIcon(image));
                            ecran.setText("");
                        });
                    }
                } catch (Exception e) {
                    enCours = false;
                }
            }
        }, "Thread-Reception-Video");

        threadReception.setDaemon(true);
        threadReception.start();
    }

    public void arreter() {
        enCours = false;
        try {
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (Exception ignored) {}
    }
}