package com.chatapp.client.modern;

import com.github.sarxos.webcam.Webcam;
import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;

public class MoteurVideoTCP {

    private Webcam webcam;
    private boolean videoEnCours = false;

    // --- ENVOI DE LA VIDÉO (TCP) ---
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

                    Thread.sleep(50); // Environ 20 images par seconde
                }
                webcam.close();
            } catch (Exception e) {
                System.out.println("Fin de l'envoi vidéo.");
            }
        }).start();
    }

    // --- RÉCEPTION DE LA VIDÉO (TCP) ---
    // On demande ton JLabel "ecranAutre" pour y coller la vidéo !
    public void recevoirVideo(int portEcouteTCP, JLabel ecranAutre) {
        new Thread(() -> {
            try (ServerSocket serveur = new ServerSocket(portEcouteTCP);
                 Socket socket = serveur.accept();
                 DataInputStream entree = new DataInputStream(socket.getInputStream())) {

                while (true) {
                    int tailleImage = entree.readInt();
                    byte[] donnees = new byte[tailleImage];
                    entree.readFully(donnees);

                    BufferedImage imageRecue = ImageIO.read(new ByteArrayInputStream(donnees));
                    if (imageRecue != null) {
                        // On met à jour l'écran de l'autre utilisateur en direct
                        ecranAutre.setIcon(new ImageIcon(imageRecue));
                        ecranAutre.setText(""); // On efface le texte pour ne laisser que l'image
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
}

