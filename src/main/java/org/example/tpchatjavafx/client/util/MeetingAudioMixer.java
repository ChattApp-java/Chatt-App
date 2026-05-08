package org.example.tpchatjavafx.client.util;

import org.example.tpchatjavafx.client.audio.AudioPlaybackService;
import javax.sound.sampled.LineUnavailableException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mixeur audio pour les réunions multi-utilisateurs.
 * Reçoit les flux audio de N participants et les mixe en un seul flux de sortie.
 */
public class MeetingAudioMixer {
    private final Map<String, byte[]> participantFrames = new ConcurrentHashMap<>();
    private final AudioPlaybackService playbackService = new AudioPlaybackService();
    private volatile boolean running = false;
    private Thread mixingThread;

    public void start() throws LineUnavailableException {
        if (running) return;
        playbackService.start();
        running = true;
        mixingThread = new Thread(this::mixLoop, "MeetingAudioMixer");
        mixingThread.setDaemon(true);
        mixingThread.start();
    }

    public void addAudioFrame(String participantId, byte[] audioData) {
        if (participantId == null || audioData == null) return;
        participantFrames.put(participantId, audioData);
    }

    public void removeParticipant(String participantId) {
        if (participantId != null) {
            participantFrames.remove(participantId);
        }
    }

    public void stop() {
        running = false;
        if (mixingThread != null) {
            mixingThread.interrupt();
            try {
                mixingThread.join(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            mixingThread = null;
        }
        playbackService.stop();
        participantFrames.clear();
    }

    private void mixLoop() {
        while (running) {
            try {
                byte[] mixed = mixFrames();
                if (mixed != null) {
                    playbackService.playAudio(mixed);
                }
                // Attente courte pour laisser les frames s'accumuler
                Thread.sleep(20); 
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                System.err.println("Erreur mixage audio : " + e.getMessage());
            }
        }
    }

    private byte[] mixFrames() {
        if (participantFrames.isEmpty()) return null;

        int maxLength = participantFrames.values().stream().mapToInt(f -> f.length).max().orElse(0);
        if (maxLength == 0) return null;

        byte[] mixed = new byte[maxLength];
        
        // Mixage PCM 16-bit (2 octets par sample)
        for (int i = 0; i < maxLength; i += 2) {
            int sum = 0;
            int count = 0;
            
            for (byte[] frame : participantFrames.values()) {
                if (i + 1 < frame.length) {
                    // Little-endian 16-bit signed PCM
                    short sample = (short) ((frame[i + 1] << 8) | (frame[i] & 0xFF));
                    sum += sample;
                    count++;
                }
            }
            
            if (count > 0) {
                // Mixage simple avec clipping (écrêtage)
                int result = sum; 
                if (result > 32767) result = 32767;
                else if (result < -32768) result = -32768;
                
                mixed[i] = (byte) (result & 0xFF);
                mixed[i + 1] = (byte) ((result >> 8) & 0xFF);
            }
        }
        
        // On vide pour éviter de rejouer les mêmes frames au prochain tour
        participantFrames.clear(); 
        
        return mixed;
    }
}
