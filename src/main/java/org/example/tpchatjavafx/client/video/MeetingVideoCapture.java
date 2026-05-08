package org.example.tpchatjavafx.client.video;

import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.bytedeco.javacv.OpenCVFrameGrabber;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class MeetingVideoCapture {

    private OpenCVFrameGrabber grabber;
    private DatagramSocket socket;
    private InetAddress remoteAddress;
    private int remotePort;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private Thread captureThread;
    private final Java2DFrameConverter converter = new Java2DFrameConverter();
    private Consumer<byte[]> onFrameCaptured;

    public void setOnFrameCaptured(Consumer<byte[]> onFrameCaptured) {
        this.onFrameCaptured = onFrameCaptured;
    }

    public void start(String remoteHost, int remotePort, int localPort) throws Exception {
        stop();
        this.remoteAddress = InetAddress.getByName(remoteHost);
        this.remotePort = remotePort;
        this.socket = (localPort > 0) ? new DatagramSocket(localPort) : new DatagramSocket();
        grabber = new OpenCVFrameGrabber(0);
        grabber.setImageWidth(640);
        grabber.setImageHeight(480);
        grabber.start();

        running.set(true);
        captureThread = new Thread(this::captureLoop, "MeetingVideoCapture");
        captureThread.setDaemon(true);
        captureThread.start();
    }

    public void sendFrame(byte[] jpegFrame) {
        if (!running.get() || socket == null || remoteAddress == null || jpegFrame == null || jpegFrame.length == 0) {
            return;
        }

        DatagramPacket packet = new DatagramPacket(jpegFrame, jpegFrame.length, remoteAddress, remotePort);
        try {
            socket.send(packet);
        } catch (IOException e) {
            System.err.println("Erreur envoi vidéo : " + e.getMessage());
        }
    }

    private void captureLoop() {
        while (running.get()) {
            try {
                Frame frame = grabber.grab();
                if (frame != null && frame.image != null) {
                    byte[] jpeg = frameToJpeg(frame);
                    if (jpeg != null) {
                        if (onFrameCaptured != null) {
                            onFrameCaptured.accept(jpeg);
                        }
                        sendFrame(jpeg);
                    }
                }
                Thread.sleep(100);
            } catch (FrameGrabber.Exception | IOException | InterruptedException e) {
                if (running.get()) {
                    System.err.println("Erreur capture vidéo réunion : " + e.getMessage());
                }
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private byte[] frameToJpeg(Frame frame) throws IOException {
        BufferedImage image = converter.convert(frame);
        if (image == null) {
            return null;
        }
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(image, "jpg", baos);
            return baos.toByteArray();
        }
    }

    public void stop() {
        running.set(false);
        if (captureThread != null) {
            captureThread.interrupt();
            try {
                captureThread.join(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            captureThread = null;
        }
        if (grabber != null) {
            try {
                grabber.stop();
                grabber.release();
            } catch (FrameGrabber.Exception e) {
                // ignore
            }
            grabber = null;
        }
        if (socket != null) {
            socket.close();
            socket = null;
        }
    }
}

