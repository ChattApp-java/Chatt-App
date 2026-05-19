package org.example.tpchatjavafx.client.audio;

import javax.sound.sampled.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.IntConsumer;

/**
 * Joue l'audio recu du reseau.
 */
public class AudioPlaybackService {
    private static final int MAX_QUEUE_PACKETS = 24;
    private static final int PREBUFFER_PACKETS = 3;
    private static final int PACKET_DURATION_MS = 20;

    private SourceDataLine speakers;
    private Thread playbackThread;
    private volatile boolean running = false;
    private volatile double volume = 1.0;
    private final BlockingQueue<byte[]> audioQueue = new ArrayBlockingQueue<>(MAX_QUEUE_PACKETS);
    private IntConsumer onProgress;
    private AudioFormat currentFormat;
    private volatile boolean playbackPrimed = false;

    public void setOnProgress(IntConsumer onProgress) {
        this.onProgress = onProgress;
    }

    /**
     * Demarre la lecture audio avec un format supporte par les haut-parleurs.
     */
    public void start() throws LineUnavailableException {
        start(null);
    }

    public void start(AudioFormat preferredFormat) throws LineUnavailableException {
        if (running) return;

        currentFormat = getSupportedPlaybackFormat(preferredFormat);

        DataLine.Info info = new DataLine.Info(SourceDataLine.class, currentFormat);
        if (!AudioSystem.isLineSupported(info)) {
            throw new LineUnavailableException("Aucun format audio supporte par les haut-parleurs");
        }

        speakers = (SourceDataLine) AudioSystem.getLine(info);
        speakers.open(currentFormat);
        speakers.start();

        running = true;
        playbackPrimed = false;
        playbackThread = new Thread(this::playbackLoop, "AudioPlayback");
        playbackThread.setDaemon(true);
        playbackThread.start();

        System.out.println("[AudioPlayback] Demarre avec format: " + currentFormat);
    }

    private AudioFormat getSupportedPlaybackFormat(AudioFormat preferredFormat) {
        if (preferredFormat != null) {
            DataLine.Info preferredInfo = new DataLine.Info(SourceDataLine.class, preferredFormat);
            if (AudioSystem.isLineSupported(preferredInfo)) {
                return preferredFormat;
            }
        }

        AudioFormat duplexFormat = AudioCaptureService.findBestDuplexFormat();
        if (duplexFormat != null) {
            return duplexFormat;
        }

        AudioFormat captureFormat = AudioCaptureService.findSupportedCaptureFormat();
        if (captureFormat != null) {
            DataLine.Info captureInfo = new DataLine.Info(SourceDataLine.class, captureFormat);
            if (AudioSystem.isLineSupported(captureInfo)) {
                return captureFormat;
            }
        }

        return new AudioFormat(44100, 16, 1, true, false);
    }

    public void playAudio(byte[] audioData) {
        if (!running || volume <= 0.0 || audioData == null || audioData.length == 0) return;
        offerLatestAudio(audioData);
    }

    public void playAudio(byte[] audioData, AudioFormat sourceFormat) {
        if (!running || volume <= 0.0 || audioData == null || audioData.length == 0) return;
        if (sourceFormat != null && currentFormat != null && !AudioFormatUtil.sameFormat(sourceFormat, currentFormat)) {
            audioData = AudioFormatUtil.convert(audioData, sourceFormat, currentFormat);
        }
        offerLatestAudio(audioData);
    }

    public void setVolume(double volume) {
        this.volume = Math.max(0.0, Math.min(1.0, volume));
        if (speakers != null && speakers.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
            try {
                FloatControl gainControl = (FloatControl) speakers.getControl(FloatControl.Type.MASTER_GAIN);
                double safeVolume = Math.max(0.0001, this.volume);
                float dB = (float) (20.0 * Math.log10(safeVolume));
                dB = Math.max(gainControl.getMinimum(), Math.min(gainControl.getMaximum(), dB));
                gainControl.setValue(dB);
            } catch (Exception ignored) {
            }
        }
    }

    public void startPlayback() throws LineUnavailableException {
        start();
    }

    public void stopPlayback() {
        stop();
    }

    public void stop() {
        running = false;

        if (playbackThread != null) {
            playbackThread.interrupt();
            try {
                playbackThread.join(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            playbackThread = null;
        }

        if (speakers != null) {
            speakers.drain();
            speakers.stop();
            speakers.close();
            speakers = null;
        }

        audioQueue.clear();
        playbackPrimed = false;
        System.out.println("[AudioPlayback] Arrete");
    }

    private void playbackLoop() {
        byte[] silencePacket = buildSilencePacket();
        while (running) {
            try {
                if (!playbackPrimed) {
                    if (audioQueue.size() < PREBUFFER_PACKETS) {
                        Thread.sleep(5);
                        continue;
                    }
                    playbackPrimed = true;
                }

                byte[] audioData = audioQueue.poll(PACKET_DURATION_MS, TimeUnit.MILLISECONDS);
                if (audioData == null) {
                    audioData = silencePacket;
                }

                if (audioData != null && speakers != null && speakers.isOpen()) {
                    byte[] output = volume < 1.0 ? applyVolume(audioData) : audioData;
                    speakers.write(output, 0, output.length);
                    if (onProgress != null) onProgress.accept(speakers.getFramePosition());
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                if (running) {
                    System.err.println("[AudioPlayback] Erreur lecture: " + e.getMessage());
                }
            }
        }
    }

    private void offerLatestAudio(byte[] audioData) {
        if (!audioQueue.offer(audioData)) {
            audioQueue.poll();
            audioQueue.offer(audioData);
        }
    }

    private byte[] buildSilencePacket() {
        if (currentFormat == null) {
            return new byte[640];
        }
        int frameSize = Math.max(1, currentFormat.getFrameSize());
        int framesPerPacket = Math.max(1, Math.round(currentFormat.getFrameRate() * PACKET_DURATION_MS / 1000f));
        return new byte[framesPerPacket * frameSize];
    }

    private byte[] applyVolume(byte[] audioData) {
        if (volume >= 1.0) return audioData;

        byte[] result = new byte[audioData.length];
        for (int i = 0; i < audioData.length - 1; i += 2) {
            short sample = (short) ((audioData[i + 1] << 8) | (audioData[i] & 0xFF));
            sample = (short) (sample * volume);
            result[i] = (byte) (sample & 0xFF);
            result[i + 1] = (byte) ((sample >> 8) & 0xFF);
        }
        return result;
    }

    public static AudioFormat getFormat() {
        AudioFormat duplexFormat = AudioCaptureService.findBestDuplexFormat();
        return duplexFormat != null ? duplexFormat : new AudioFormat(44100, 16, 1, true, false);
    }

    public boolean isRunning() {
        return running;
    }

    public AudioFormat getCurrentFormat() {
        return currentFormat;
    }
}
