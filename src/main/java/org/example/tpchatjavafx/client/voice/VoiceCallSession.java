package org.example.tpchatjavafx.client.voice;

import org.example.tpchatjavafx.client.NetworkClient;
import org.example.tpchatjavafx.client.audio.AudioCaptureService;
import org.example.tpchatjavafx.client.model.ChatMessage;
import org.example.tpchatjavafx.common.MessageType;

import javax.sound.sampled.*;

public class VoiceCallSession {

    private final NetworkClient networkClient;
    private final String localUser;
    private final String remoteUser;

    private TargetDataLine mic;
    private SourceDataLine speakers;

    private Thread captureThread;
    private volatile boolean running = false;

    public VoiceCallSession(NetworkClient networkClient, String localUser, String remoteUser) {
        this.networkClient = networkClient;
        this.localUser = localUser;
        this.remoteUser = remoteUser;
    }

    public void start() throws LineUnavailableException {
        AudioFormat format = AudioCaptureService.findBestDuplexFormat();
        if (format == null) {
            format = new AudioFormat(44100, 16, 1, true, false);
        }

        DataLine.Info micInfo = new DataLine.Info(TargetDataLine.class, format);
        DataLine.Info spkInfo = new DataLine.Info(SourceDataLine.class, format);

        mic = (TargetDataLine) AudioSystem.getLine(micInfo);
        mic.open(format);
        mic.start();

        speakers = (SourceDataLine) AudioSystem.getLine(spkInfo);
        speakers.open(format);
        speakers.start();

        running = true;
        captureThread = new Thread(this::captureLoop, "voice-capture-thread");
        captureThread.setDaemon(true);
        captureThread.start();
    }

    private void captureLoop() {
        byte[] buffer = new byte[2048];

        try {
            while (running) {
                int count = mic.read(buffer, 0, buffer.length);
                if (count > 0) {
                    byte[] frame = new byte[count];
                    System.arraycopy(buffer, 0, frame, 0, count);

                    ChatMessage msg = new ChatMessage(
                            MessageType.VOICE_FRAME,
                            localUser,
                            remoteUser,
                            null,
                            "frame"
                    );
                    msg.setBinaryData(frame);
                    networkClient.send(msg);
                }
            }
        } catch (Exception ignored) {
        }
    }

    public void playRemoteAudio(byte[] data) {
        if (speakers == null || data == null) return;
        speakers.write(data, 0, data.length);
    }

    public void stop() {
        running = false;
        try {
            if (captureThread != null) captureThread.join(300);
        } catch (InterruptedException ignored) {}

        if (mic != null) {
            mic.stop();
            mic.close();
        }
        if (speakers != null) {
            speakers.drain();
            speakers.stop();
            speakers.close();
        }
    }
}
