package com.chatapp.audio;

import javax.sound.sampled.*;

public class CapteurAudio {

    private TargetDataLine microphone;
    private final javax.sound.sampled.AudioFormat format;
    public static final int CHUNK_SIZE = 1024;

    // Format universel 8000Hz (téléphonique) — supporté par tous les micros
    public static final float SAMPLE_RATE = 8000f;
    public static final int SAMPLE_SIZE_BITS = 16;
    public static final int CHANNELS = 1;
    public static final boolean SIGNED = true;
    public static final boolean BIG_ENDIAN = false;
    public static final int FRAME_SIZE = 2; // 16-bit mono = 2 bytes/frame

    public CapteurAudio() {
        this.format = new javax.sound.sampled.AudioFormat(
                SAMPLE_RATE, SAMPLE_SIZE_BITS, CHANNELS, SIGNED, BIG_ENDIAN
        );
    }

    public void demarrer() throws LineUnavailableException {
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

        if (!AudioSystem.isLineSupported(info)) {
            // Fallback : essayer avec le format par défaut du système
            try {
                microphone = AudioSystem.getTargetDataLine(null);
                microphone.open();
            } catch (Exception ex) {
                throw new LineUnavailableException("Microphone non supporte : " + ex.getMessage());
            }
        } else {
            microphone = (TargetDataLine) AudioSystem.getLine(info);
            microphone.open(format);
        }
        microphone.start();
    }

    public byte[] lireChunk() {
        byte[] buffer = new byte[CHUNK_SIZE];
        microphone.read(buffer, 0, buffer.length);
        return buffer;
    }

    public boolean estActif() {
        return microphone != null && microphone.isOpen();
    }

    public void arreter() {
        if (microphone != null) {
            microphone.stop();
            microphone.close();
        }
    }

    public static javax.sound.sampled.AudioFormat getFormat() {
        return new javax.sound.sampled.AudioFormat(
                SAMPLE_RATE, SAMPLE_SIZE_BITS, CHANNELS, SIGNED, BIG_ENDIAN
        );
    }
}

