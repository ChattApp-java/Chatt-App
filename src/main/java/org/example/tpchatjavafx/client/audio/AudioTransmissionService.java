package org.example.tpchatjavafx.client.audio;

import javax.sound.sampled.LineUnavailableException;
import java.io.IOException;
import java.net.*;

/**
 * Transmission audio UDP entre 2 clients.
 * - Capture audio et envoie via UDP
 * - Reçoit audio UDP et joue
 */
public class AudioTransmissionService {

    private DatagramSocket socket;
    private InetAddress remoteAddress;
    private int remotePort;
    private AudioCaptureService captureService;
    private AudioPlaybackService playbackService;
    private volatile boolean running = false;
    private Thread receiveThread;
    private int meetingId;
    private int userId;

    public void initiate(String remoteHost, int remotePort) throws Exception {
        initiate(remoteHost, remotePort, 0);
    }

    public void initiate(String remoteHost, int remotePort, int localPort) throws Exception {
        initiate(remoteHost, remotePort, localPort, 0, 0);
    }

    public void initiate(String remoteHost, int remotePort, int localPort, int meetingId, int userId) throws Exception {
        if (running) stop();

        this.remoteAddress = InetAddress.getByName(remoteHost);
        this.remotePort = remotePort;
        this.meetingId = meetingId;
        this.userId = userId;
        this.socket = (localPort > 0) ? new DatagramSocket(localPort) : new DatagramSocket();
        this.socket.setSoTimeout(1000);

        captureService = new AudioCaptureService();
        playbackService = new AudioPlaybackService();

        captureService.setOnAudioCaptured(this::sendAudio);
        captureService.start();
        playbackService.start();

        running = true;
        receiveThread = new Thread(this::receiveAudioLoop, "AudioReceive");
        receiveThread.setDaemon(true);
        receiveThread.start();
    }

    public void sendAudioFrame(byte[] audioData) {
        sendAudio(audioData);
    }

    private void sendAudio(byte[] audioData) {
        if (!running || socket == null || remoteAddress == null || audioData == null || audioData.length == 0) {
            return;
        }

        try {
            byte[] payload = addRelayHeader(audioData);
            DatagramPacket packet = new DatagramPacket(payload, payload.length, remoteAddress, remotePort);
            socket.send(packet);
        } catch (IOException e) {
            if (running) {
                System.err.println("Erreur envoi audio: " + e.getMessage());
            }
        }
    }

    private void receiveAudioLoop() {
        byte[] buffer = new byte[4096];

        while (running) {
            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                int length = packet.getLength();
                if (length > 0 && playbackService != null) {
                    int offset = hasRelayHeader(packet.getData(), length) ? 8 : 0;
                    byte[] audioData = new byte[length - offset];
                    System.arraycopy(packet.getData(), offset, audioData, 0, audioData.length);
                    playbackService.playAudio(audioData);
                }
            } catch (SocketTimeoutException ignored) {
                // Timeout régulier pour vérifier running
            } catch (IOException e) {
                if (running) {
                    System.err.println("Erreur réception audio: " + e.getMessage());
                }
            }
        }
    }

    public int getLocalPort() {
        return socket != null ? socket.getLocalPort() : -1;
    }

    public void stop() {
        running = false;
        if (captureService != null) {
            captureService.stop();
        }
        if (playbackService != null) {
            playbackService.stop();
        }
        if (socket != null) {
            socket.close();
            socket = null;
        }
        if (receiveThread != null) {
            receiveThread.interrupt();
            try {
                receiveThread.join(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            receiveThread = null;
        }
    }

    public boolean isRunning() {
        return running;
    }

    private byte[] addRelayHeader(byte[] data) {
        if (meetingId <= 0 || userId <= 0) return data;
        byte[] payload = new byte[data.length + 8];
        writeInt(payload, 0, meetingId);
        writeInt(payload, 4, userId);
        System.arraycopy(data, 0, payload, 8, data.length);
        return payload;
    }

    private boolean hasRelayHeader(byte[] data, int length) {
        return length > 8 && readInt(data, 0) > 0 && readInt(data, 4) > 0;
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
