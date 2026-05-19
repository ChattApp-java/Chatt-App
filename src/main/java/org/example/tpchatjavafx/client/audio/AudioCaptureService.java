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

    private AudioFormat currentFormat;

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

        // Try multiple audio formats with fallback
        AudioFormat format = findSupportedAudioFormat();
        if (format == null) {
            throw new LineUnavailableException("Aucun format audio supporte n'a ete trouve");
        }

        this.currentFormat = format;
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
        microphone = (TargetDataLine) AudioSystem.getLine(info);
        microphone.open(format);
        microphone.start();

        isRunning = true;
        Thread captureThread = new Thread(this::captureLoop, "AudioCapture");
        captureThread.setDaemon(true);
        captureThread.start();
        System.out.println("[AUDIO_CAPTURE] Format audio utilise: " + format.getSampleRate() + " Hz");
    }

    private AudioFormat findSupportedAudioFormat() {
        // Essayer les formats dans cet ordre
        float[] sampleRates = {8000.0f, 16000.0f, 44100.0f, 22050.0f};
        
        for (float sampleRate : sampleRates) {
            AudioFormat format = new AudioFormat(sampleRate, 16, 1, true, false);
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
            
            if (AudioSystem.isLineSupported(info)) {
                System.out.println("[AUDIO_CAPTURE] Format audio trouve: " + sampleRate + " Hz");
                return format;
            }
        }
        return null;
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

    public void startCapture(java.util.function.Consumer<byte[]> callback) throws LineUnavailableException {
        start(callback);
    }

    public void stopCapture() {
        stop();
    }

    public AudioFormat getFormat() {
        return currentFormat;
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
