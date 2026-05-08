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

    public void initiate(String remoteHost, int remotePort) throws Exception {
        initiate(remoteHost, remotePort, 0);
    }

    public void initiate(String remoteHost, int remotePort, int localPort) throws Exception {
        if (running) stop();

        this.remoteAddress = InetAddress.getByName(remoteHost);
        this.remotePort = remotePort;
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
            DatagramPacket packet = new DatagramPacket(audioData, audioData.length, remoteAddress, remotePort);
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
                    byte[] audioData = new byte[length];
                    System.arraycopy(packet.getData(), 0, audioData, 0, length);
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
}
