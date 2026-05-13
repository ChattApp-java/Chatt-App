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
import java.net.SocketTimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.BiConsumer;

public class MeetingVideoCapture {

    private OpenCVFrameGrabber grabber;
    private DatagramSocket socket;
    private InetAddress remoteAddress;
    private int remotePort;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private Thread captureThread;
    private Thread receiveThread;
    private final Java2DFrameConverter converter = new Java2DFrameConverter();
    private Consumer<byte[]> onFrameCaptured;
    private BiConsumer<Integer, byte[]> onRemoteFrame;
    private int meetingId;
    private int userId;

    public void setOnFrameCaptured(Consumer<byte[]> onFrameCaptured) {
        this.onFrameCaptured = onFrameCaptured;
    }

    public void setOnRemoteFrame(BiConsumer<Integer, byte[]> onRemoteFrame) {
        this.onRemoteFrame = onRemoteFrame;
    }

    public void start(String remoteHost, int remotePort, int localPort) throws Exception {
        start(remoteHost, remotePort, localPort, 0, 0);
    }

    public void start(String remoteHost, int remotePort, int localPort, int meetingId, int userId) throws Exception {
        stop();
        this.remoteAddress = InetAddress.getByName(remoteHost);
        this.remotePort = remotePort;
        this.meetingId = meetingId;
        this.userId = userId;
        this.socket = (localPort > 0) ? new DatagramSocket(localPort) : new DatagramSocket();
        this.socket.setSoTimeout(1000);
        grabber = new OpenCVFrameGrabber(0);
        grabber.setImageWidth(640);
        grabber.setImageHeight(480);
        grabber.start();

        running.set(true);
        captureThread = new Thread(this::captureLoop, "MeetingVideoCapture");
        captureThread.setDaemon(true);
        captureThread.start();
        receiveThread = new Thread(this::receiveLoop, "MeetingVideoReceive");
        receiveThread.setDaemon(true);
        receiveThread.start();
    }

    public void sendFrame(byte[] jpegFrame) {
        if (!running.get() || socket == null || remoteAddress == null || jpegFrame == null || jpegFrame.length == 0) {
            return;
        }

        byte[] payload = addRelayHeader(jpegFrame);
        DatagramPacket packet = new DatagramPacket(payload, payload.length, remoteAddress, remotePort);
        try {
            socket.send(packet);
        } catch (IOException e) {
            System.err.println("Erreur envoi vidéo : " + e.getMessage());
        }
    }

    private void receiveLoop() {
        byte[] buffer = new byte[65507];
        while (running.get()) {
            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                int length = packet.getLength();
                if (length <= 8) continue;
                int senderId = readInt(packet.getData(), 4);
                byte[] frame = new byte[length - 8];
                System.arraycopy(packet.getData(), 8, frame, 0, frame.length);
                if (onRemoteFrame != null && senderId > 0) onRemoteFrame.accept(senderId, frame);
            } catch (SocketTimeoutException ignored) {
            } catch (IOException e) {
                if (running.get()) System.err.println("Erreur reception video reunion : " + e.getMessage());
            }
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
            } catch (IOException | InterruptedException e) {
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
        if (receiveThread != null) {
            receiveThread.interrupt();
            receiveThread = null;
        }
    }

    public int getLocalPort() {
        return socket != null ? socket.getLocalPort() : -1;
    }

    private byte[] addRelayHeader(byte[] data) {
        if (meetingId <= 0 || userId <= 0) return data;
        byte[] payload = new byte[data.length + 8];
        writeInt(payload, 0, meetingId);
        writeInt(payload, 4, userId);
        System.arraycopy(data, 0, payload, 8, data.length);
        return payload;
    }

    private void writeInt(byte[] data, int offset, int value) {
        data[offset] = (byte) (value >>> 24);
        data[offset + 1] = (byte) (value >>> 16);
        data[offset + 2] = (byte) (value >>> 8);
        data[offset + 3] = (byte) value;
    }

    private int readInt(byte[] data, int offset) {
        return ((data[offset] & 0xFF) << 24)
                | ((data[offset + 1] & 0xFF) << 16)
                | ((data[offset + 2] & 0xFF) << 8)
                | (data[offset + 3] & 0xFF);
    }
}

