package org.example.tpchatjavafx.client.video;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;

public class MeetingVideoCapture {

    private DatagramSocket socket;
    private InetAddress remoteAddress;
    private int remotePort;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private Thread receiveThread;
    private BiConsumer<Integer, byte[]> onRemoteFrame;
    private int meetingId;
    private int userId;

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

        running.set(true);
        receiveThread = new Thread(this::receiveLoop, "MeetingVideoReceive");
        receiveThread.setDaemon(true);
        receiveThread.start();
        System.out.println("[MEETING_VIDEO] Relais UDP video actif sur le port " + socket.getLocalPort());
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
            System.err.println("[MEETING_VIDEO] Erreur envoi: " + e.getMessage());
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
                if (onRemoteFrame != null && senderId > 0) {
                    onRemoteFrame.accept(senderId, frame);
                }
            } catch (SocketTimeoutException ignored) {
            } catch (IOException e) {
                if (running.get()) {
                    System.err.println("[MEETING_VIDEO] Erreur reception: " + e.getMessage());
                }
            }
        }
    }

    public void stop() {
        running.set(false);
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
