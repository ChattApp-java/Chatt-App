package org.example.tpchatjavafx.client.audio;

import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.AudioFormat;
import java.io.IOException;
import java.net.*;
import java.util.function.Consumer;

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
    private Consumer<byte[]> onAudioReceived;
    private AudioFormat captureFormat;

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
        this.socket.setReceiveBufferSize(262144);
        this.socket.setSendBufferSize(262144);

        captureService = new AudioCaptureService();
        playbackService = new AudioPlaybackService();
        AudioFormat transportFormat = AudioFormatUtil.NETWORK_FORMAT;

        captureService.setOnAudioCaptured(this::sendAudio);
        captureService.start(transportFormat, this::sendAudio);
        playbackService.start(transportFormat);
        captureFormat = captureService.getFormat();

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
            if (captureFormat != null && !AudioFormatUtil.sameFormat(captureFormat, AudioFormatUtil.NETWORK_FORMAT)) {
                audioData = AudioFormatUtil.convert(audioData, captureFormat, AudioFormatUtil.NETWORK_FORMAT);
            }
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
        byte[] buffer = new byte[65507];

        while (running) {
            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                int length = packet.getLength();
                if (length > 0 && playbackService != null) {
                    int offset = hasRelayHeader(packet.getData(), length) ? 8 : 0;
                    if (length <= offset) {
                        continue;
                    }
                    byte[] audioData = new byte[length - offset];
                    System.arraycopy(packet.getData(), offset, audioData, 0, audioData.length);
                    if (onAudioReceived != null) {
                        onAudioReceived.accept(audioData);
                    } else if (playbackService != null) {
                        playbackService.playAudio(audioData, AudioFormatUtil.NETWORK_FORMAT);
                    }
                }
            } catch (SocketTimeoutException ignored) {

            } catch (IOException e) {
                if (running) {
                    System.err.println("Erreur rÃƒÂ©ception audio: " + e.getMessage());
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

    public void setCaptureEnabled(boolean enabled) {
        if (captureService == null) return;
        if (enabled && !captureService.isRunning()) {
            try {
                captureService.setOnAudioCaptured(this::sendAudio);
                captureService.start(AudioFormatUtil.NETWORK_FORMAT, this::sendAudio);
                captureFormat = captureService.getFormat();
            } catch (LineUnavailableException e) {
                System.err.println("Impossible d'activer le micro: " + e.getMessage());
            }
        } else if (!enabled && captureService.isRunning()) {
            captureService.stop();
        }
    }

    public void setPlaybackVolume(double volume) {
        if (playbackService != null) {
            playbackService.setVolume(volume);
        }
    }

    public void setOnAudioReceived(Consumer<byte[]> callback) {
        this.onAudioReceived = callback;
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
