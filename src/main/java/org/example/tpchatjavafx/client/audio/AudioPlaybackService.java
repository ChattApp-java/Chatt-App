package org.example.tpchatjavafx.client.audio;

import javax.sound.sampled.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Joue l'audio reçu du réseau.
 */
public class AudioPlaybackService {

    private SourceDataLine speakers;
    private Thread playbackThread;
    private volatile boolean running = false;
    private final BlockingQueue<byte[]> audioQueue = new LinkedBlockingQueue<>();

    public void start() throws LineUnavailableException {
        if (running) return;

        AudioFormat format = AudioCaptureService.getFormat();
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
        speakers = (SourceDataLine) AudioSystem.getLine(info);
        speakers.open(format);
        speakers.start();

        running = true;
        playbackThread = new Thread(this::playbackLoop, "AudioPlayback");
        playbackThread.setDaemon(true);
        playbackThread.start();
    }

    public void playAudio(byte[] audioData) {
        if (!running || audioData == null || audioData.length == 0) return;
        audioQueue.offer(audioData);
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
    }

    private void playbackLoop() {
        while (running) {
            try {
                byte[] audioData = audioQueue.take();
                if (audioData != null && speakers != null) {
                    speakers.write(audioData, 0, audioData.length);
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

    public static AudioFormat getFormat() {
        return AudioCaptureService.getFormat();
    }

    public boolean isRunning() {
        return running;
    }
}