package org.example.tpchatjavafx.client.util;

import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.bytedeco.javacv.OpenCVFrameGrabber;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.function.Consumer;

/**
 * Capture vidéo de la webcam avec JavaCV.
 */
public class VideoCaptureService {

    private FrameGrabber grabber;
    private Java2DFrameConverter converter;
    private Consumer<byte[]> onFrameCaptured;
    private boolean isRunning = false;

    public void setOnFrameCaptured(Consumer<byte[]> callback) {
        this.onFrameCaptured = callback;
    }

    public void start() {
        if (isRunning) return;

        try {
            grabber = new OpenCVFrameGrabber(0); // Webcam par défaut
            grabber.setImageWidth(640);
            grabber.setImageHeight(480);
            grabber.start();
            converter = new Java2DFrameConverter();
            isRunning = true;

            // Thread de capture
            new Thread(this::captureLoop, "VideoCapture").start();

        } catch (FrameGrabber.Exception e) {
            System.err.println("Aucune webcam disponible: " + e.getMessage());
        }
    }

    private void captureLoop() {
        int frameSkip = 0;

        while (isRunning) {
            try {
                Frame frame = grabber.grab();
                if (frame != null && frame.image != null) {
                    BufferedImage bufferedImage = converter.convert(frame);

                    // Envoyer 1 frame tous les 3 (3-4 FPS)
                    if (frameSkip++ % 3 == 0) {
                        byte[] jpegData = convertToJPEG(bufferedImage);
                        if (onFrameCaptured != null) {
                            onFrameCaptured.accept(jpegData);
                        }
                    }
                }
            } catch (FrameGrabber.Exception e) {
                System.err.println("Erreur capture: " + e.getMessage());
                break;
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
        stopCapture();
    }

    public void startCapture(java.util.function.Consumer<byte[]> callback) {
        setOnFrameCaptured(callback);
        start();
    }

    public void stopCapture() {
        isRunning = false;
        if (grabber != null) {
            try {
                grabber.stop();
                grabber.release();
            } catch (FrameGrabber.Exception e) {
                System.err.println("Erreur arrêt webcam: " + e.getMessage());
            }
        }
        if (converter != null) {
            converter.close();
            converter = null;
        }
    }

    public void switchCamera(boolean front) {
        stopCapture();
        start();
    }
}