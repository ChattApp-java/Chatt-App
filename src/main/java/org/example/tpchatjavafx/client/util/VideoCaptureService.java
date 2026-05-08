package org.example.tpchatjavafx.client.util;

import com.github.sarxos.webcam.Webcam;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.image.PixelWriter;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.function.Consumer;

/**
 * Capture vidéo de la webcam.
 */
public class VideoCaptureService {
    
    private Webcam webcam;
    private Consumer<byte[]> onFrameCaptured;
    private boolean isRunning = false;
    
    public void setOnFrameCaptured(Consumer<byte[]> callback) {
        this.onFrameCaptured = callback;
    }
    
    public void start() {
        if (isRunning) return;
        
        webcam = Webcam.getDefault();
        if (webcam == null) {
            System.err.println("Aucune webcam disponible");
            return;
        }
        
        webcam.open();
        isRunning = true;
        
        // Thread de capture
        new Thread(this::captureLoop, "VideoCapture").start();
    }
    
    private void captureLoop() {
        int frameSkip = 0;
        
        while (isRunning) {
            BufferedImage frame = webcam.getImage();
            if (frame != null) {
                // Envoyer 1 frame tous les 3 (3-4 FPS)
                if (frameSkip++ % 3 == 0) {
                    byte[] jpegData = convertToJPEG(frame);
                    if (onFrameCaptured != null) {
                        onFrameCaptured.accept(jpegData);
                    }
                }
            }
            
            try {
                Thread.sleep(30); // ~33 FPS capture, skip 2/3
            } catch (InterruptedException e) {
                break;
            }
        }
    }
    
    private byte[] convertToJPEG(BufferedImage image) {
        try {
            // Redimensionner pour réduire bande passante
            BufferedImage resized = new BufferedImage(
                320, 240, BufferedImage.TYPE_INT_RGB
            );
            java.awt.Graphics2D g2d = resized.createGraphics();
            g2d.drawImage(image, 0, 0, 320, 240, null);
            g2d.dispose();
            
            // Convertir en JPEG
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(resized, "jpg", baos);
            return baos.toByteArray();
        } catch (Exception e) {
            System.err.println("Erreur conversion JPEG: " + e.getMessage());
            return new byte[0];
        }
    }
    
    public void stop() {
        isRunning = false;
        if (webcam != null && webcam.isOpen()) {
            webcam.close();
        }
    }
}