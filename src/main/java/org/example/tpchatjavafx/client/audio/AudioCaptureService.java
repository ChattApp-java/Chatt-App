package org.example.tpchatjavafx.client.audio;

import javax.sound.sampled.*;
import java.util.function.Consumer;

/**
 * Capture audio du microphone et envoie via callback.
 */
public class AudioCaptureService {

    private TargetDataLine microphone;
    private final byte[] buffer = new byte[4096];
    private volatile boolean isRunning = false;
    private Consumer<byte[]> onAudioCaptured;

    private static final AudioFormat FORMAT = new AudioFormat(
            16000,
            16,
            1,
            true,
            false
    );

    public void setOnAudioCaptured(Consumer<byte[]> callback) {
        this.onAudioCaptured = callback;
    }

    public void start() throws LineUnavailableException {
        if (isRunning) return;
        start(null);
    }

    public void start(Consumer<byte[]> callback) throws LineUnavailableException {
        if (isRunning) return;

        this.onAudioCaptured = callback;

        DataLine.Info info = new DataLine.Info(TargetDataLine.class, FORMAT);
        microphone = (TargetDataLine) AudioSystem.getLine(info);
        microphone.open(FORMAT);
        microphone.start();

        isRunning = true;
        Thread captureThread = new Thread(this::captureLoop, "AudioCapture");
        captureThread.setDaemon(true);
        captureThread.start();
    }

    private void captureLoop() {
        while (isRunning && microphone != null) {
            int bytesRead = microphone.read(buffer, 0, buffer.length);
            if (bytesRead > 0 && onAudioCaptured != null) {
                byte[] audioData = new byte[bytesRead];
                System.arraycopy(buffer, 0, audioData, 0, bytesRead);
                onAudioCaptured.accept(audioData);
            }
        }
    }

    public void stop() {
        isRunning = false;
        if (microphone != null) {
            microphone.stop();
            microphone.close();
            microphone = null;
        }
    }

    public static AudioFormat getFormat() {
        return FORMAT;
    }

    public boolean isRunning() {
        return isRunning;
    }
}