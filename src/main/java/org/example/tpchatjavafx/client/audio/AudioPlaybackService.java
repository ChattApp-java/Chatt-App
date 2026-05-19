package org.example.tpchatjavafx.client.audio;

import javax.sound.sampled.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.IntConsumer;

/**
 * Joue l'audio reçu du réseau.
 */
public class AudioPlaybackService {

    private SourceDataLine speakers;
    private Thread playbackThread;
    private volatile boolean running = false;
    private volatile double volume = 1.0;
    private final BlockingQueue<byte[]> audioQueue = new LinkedBlockingQueue<>();
    private IntConsumer onProgress;
    private AudioFormat currentFormat;

    public void setOnProgress(IntConsumer onProgress) {
        this.onProgress = onProgress;
    }

    /**
     * Démarre la lecture audio avec un format supporté par les haut-parleurs.
     */
    public void start() throws LineUnavailableException {
        if (running) return;

        // Essayer plusieurs formats jusqu'à trouver un supporté
        currentFormat = getSupportedPlaybackFormat();

        DataLine.Info info = new DataLine.Info(SourceDataLine.class, currentFormat);
        if (!AudioSystem.isLineSupported(info)) {
            throw new LineUnavailableException("Aucun format audio supporté par les haut-parleurs");
        }

        speakers = (SourceDataLine) AudioSystem.getLine(info);
        speakers.open(currentFormat);
        speakers.start();

        running = true;
        playbackThread = new Thread(this::playbackLoop, "AudioPlayback");
        playbackThread.setDaemon(true);
        playbackThread.start();

        System.out.println("[AudioPlayback] Démarré avec format: " + currentFormat);
    }

    /**
     * Trouve un format audio supporté par les haut-parleurs.
     */
    private AudioFormat getSupportedPlaybackFormat() {
        float[] sampleRates = {16000f, 8000f, 44100f, 22050f};
        int[] sampleSizes = {16, 8};

        for (float rate : sampleRates) {
            for (int size : sampleSizes) {
                AudioFormat format = new AudioFormat(rate, size, 1, true, false);
                DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
                if (AudioSystem.isLineSupported(info)) {
                    System.out.println("[AudioPlayback] Format supporté trouvé: " + rate + "Hz, " + size + "bit");
                    return format;
                }
            }
        }

        // Fallback ultime
        return new AudioFormat(44100, 16, 1, true, false);
    }

    public void playAudio(byte[] audioData) {
        if (!running || volume <= 0.0 || audioData == null || audioData.length == 0) return;
        audioQueue.offer(audioData);
    }

    public void setVolume(double volume) {
        this.volume = Math.max(0.0, Math.min(1.0, volume));
        // Appliquer le volume au contrôle de gain si disponible
        if (speakers != null && speakers.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
            try {
                FloatControl gainControl = (FloatControl) speakers.getControl(FloatControl.Type.MASTER_GAIN);
                float dB = (float) (20.0 * Math.log10(volume));
                dB = Math.max(gainControl.getMinimum(), Math.min(gainControl.getMaximum(), dB));
                gainControl.setValue(dB);
            } catch (Exception e) {
                // Ignorer si le contrôle n'est pas disponible
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
        System.out.println("[AudioPlayback] Arrêté");
    }

    private void playbackLoop() {
        while (running) {
            try {
                byte[] audioData = audioQueue.take();
                if (audioData != null && speakers != null && speakers.isOpen()) {
                    // Appliquer le volume en modifiant les échantillons si nécessaire
                    if (volume < 1.0) {
                        audioData = applyVolume(audioData);
                    }
                    speakers.write(audioData, 0, audioData.length);
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

    /**
     * Applique le volume aux données audio (16-bit PCM little-endian).
     */
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
        // Format par défaut compatible
        return new AudioFormat(16000, 16, 1, true, false);
    }

    public boolean isRunning() {
        return running;
    }

    public AudioFormat getCurrentFormat() {
        return currentFormat;
    }
}