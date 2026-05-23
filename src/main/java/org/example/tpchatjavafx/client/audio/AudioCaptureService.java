package org.example.tpchatjavafx.client.audio;

import javax.sound.sampled.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

public class AudioCaptureService {
    private static final float[] SAMPLE_RATES = {44100.0f, 22050.0f, 16000.0f, 8000.0f};

    private TargetDataLine microphone;

    private final byte[] buffer = new byte[640];
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
        start(null, null);
    }

    public void start(Consumer<byte[]> callback) throws LineUnavailableException {
        start(null, callback);
    }

    public void start(AudioFormat preferredFormat, Consumer<byte[]> callback) throws LineUnavailableException {
        if (isRunning) return;

        this.onAudioCaptured = callback;

        AudioFormat format = resolveCaptureFormat(preferredFormat);
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

    public static AudioFormat findBestDuplexFormat() {
        for (float sampleRate : SAMPLE_RATES) {
            AudioFormat format = new AudioFormat(sampleRate, 16, 1, true, false);
            DataLine.Info micInfo = new DataLine.Info(TargetDataLine.class, format);
            DataLine.Info speakerInfo = new DataLine.Info(SourceDataLine.class, format);
            if (AudioSystem.isLineSupported(micInfo) && AudioSystem.isLineSupported(speakerInfo)) {
                return format;
            }
        }
        return null;
    }

    public static AudioFormat findSupportedCaptureFormat() {
        for (float sampleRate : SAMPLE_RATES) {
            AudioFormat format = new AudioFormat(sampleRate, 16, 1, true, false);
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
            if (AudioSystem.isLineSupported(info)) {
                return format;
            }
        }
        return null;
    }

    private AudioFormat resolveCaptureFormat(AudioFormat preferredFormat) {
        if (preferredFormat != null) {
            DataLine.Info preferredInfo = new DataLine.Info(TargetDataLine.class, preferredFormat);
            if (AudioSystem.isLineSupported(preferredInfo)) {
                return preferredFormat;
            }
        }

        AudioFormat duplexFormat = findBestDuplexFormat();
        if (duplexFormat != null) {
            System.out.println("[AUDIO_CAPTURE] Format duplex trouve: " + duplexFormat.getSampleRate() + " Hz");
            return duplexFormat;
        }

        AudioFormat captureFormat = findSupportedCaptureFormat();
        if (captureFormat != null) {
            System.out.println("[AUDIO_CAPTURE] Format capture trouve: " + captureFormat.getSampleRate() + " Hz");
        }
        return captureFormat;
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
