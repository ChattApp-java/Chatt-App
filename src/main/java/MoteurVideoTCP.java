
import com.github.sarxos.webcam.Webcam;
import javafx.application.Platform;
import javafx.scene.image.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;

public class MoteurVideoTCP {

    private Webcam  webcam;
    private boolean videoEnCours = false;

    // ── ENVOI DE LA VIDÉO (TCP) ───────────────────────────────
    public void envoyerVideo(String ipDistante, int portTCP) {
        new Thread(() -> {
            try (Socket socket = new Socket(ipDistante, portTCP);
                 DataOutputStream sortie = new DataOutputStream(socket.getOutputStream())) {

                webcam = Webcam.getDefault();
                if (webcam != null && !webcam.isOpen()) {
                    webcam.open();
                }
                videoEnCours = true;

                while (videoEnCours) {
                    BufferedImage image = webcam.getImage();
                    ByteArrayOutputStream convertisseur = new ByteArrayOutputStream();
                    ImageIO.write(image, "jpg", convertisseur);
                    byte[] imageBytes = convertisseur.toByteArray();

                    sortie.writeInt(imageBytes.length); // Envoie la taille
                    sortie.write(imageBytes);           // Envoie l'image
                    sortie.flush();

                    Thread.sleep(50); // ~20 images par seconde
                }
                webcam.close();

            } catch (Exception e) {
                System.out.println("Fin de l'envoi vidéo.");
            }
        }).start();
    }

    // ── RÉCEPTION DE LA VIDÉO (TCP) ──────────────────────────
    // On reçoit un ImageView JavaFX au lieu d'un JLabel Swing
    public void recevoirVideo(int portEcouteTCP, ImageView ecranAutre) {
        new Thread(() -> {
            try (ServerSocket serveur = new ServerSocket(portEcouteTCP);
                 Socket socket      = serveur.accept();
                 DataInputStream entree = new DataInputStream(socket.getInputStream())) {

                while (true) {
                    int    tailleImage = entree.readInt();
                    byte[] donnees     = new byte[tailleImage];
                    entree.readFully(donnees);

                    BufferedImage imageRecue = ImageIO.read(new ByteArrayInputStream(donnees));
                    if (imageRecue != null) {
                        // Conversion BufferedImage → JavaFX Image
                        Image fxImage = bufferedToFxImage(imageRecue);

                        // Mise à jour de l'ImageView sur le thread JavaFX
                        Platform.runLater(() -> ecranAutre.setImage(fxImage));
                    }
                }
            } catch (Exception e) {
                System.out.println("Fin de la réception vidéo.");
            }
        }).start();
    }

    public void arreterVideo() {
        videoEnCours = false;
        if (webcam != null) webcam.close();
    }

    // ── Conversion BufferedImage → JavaFX Image ───────────────
    private static Image bufferedToFxImage(BufferedImage img) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, "png", out);
        return new Image(new ByteArrayInputStream(out.toByteArray()));
    }
}