package org.example.tpchatjavafx.client.audio;

import javax.sound.sampled.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * Capture audio du microphone et envoie via callback.
 */
public class AudioCaptureService {

    private TargetDataLine microphone;
    private final byte[] buffer = new byte[4096];
    private volatile boolean isRunning = false;
    private Consumer<byte[]> onAudioCaptured;
    private final List<Double> amplitudeData = Collections.synchronizedList(new ArrayList<>());

    private static final AudioFormat FORMAT = new AudioFormat(
            16000,
            16,
            1,
            true,
            false
    );

    public void setOnAudioCaptured(Consumer<byte[]> callback) {
        this.onAudioCaptured = callback;
        amplitudeData.clear();
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
                amplitudeData.add(calculateAmplitude(buffer, bytesRead));
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

    public List<Double> getAmplitudeData() {
        synchronized (amplitudeData) {
            return new ArrayList<>(amplitudeData);
        }
    }

    public static double calculateAmplitude(byte[] audioData, int length) {
        if (audioData == null || length < 2) return 0.0;
        long sum = 0;
        int samples = 0;
        for (int i = 0; i + 1 < length; i += 2) {
            int sample = (audioData[i + 1] << 8) | (audioData[i] & 0xff);
            sum += Math.abs(sample);
            samples++;
        }
        if (samples == 0) return 0.0;
        return Math.min(1.0, (sum / (double) samples) / 32768.0);
    }
}
