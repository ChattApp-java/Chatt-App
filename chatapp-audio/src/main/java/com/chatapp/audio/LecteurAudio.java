package com.chatapp.audio;

import javax.sound.sampled.*;

public class LecteurAudio {

    private SourceDataLine hautParleur;
    private final javax.sound.sampled.AudioFormat format;

    public LecteurAudio() {
        // MÊME format que CapteurAudio — obligatoire
        this.format = CapteurAudio.getFormat();
    }

    public void demarrer() throws LineUnavailableException {
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
        hautParleur = (SourceDataLine) AudioSystem.getLine(info);
        hautParleur.open(format);
        hautParleur.start();
    }

    public void jouer(byte[] chunk) {
        if (hautParleur != null && hautParleur.isOpen() && chunk.length > 0) {
            // Tronquer à un multiple entier de frameSize pour éviter l'erreur
            int frameSize = hautParleur.getFormat().getFrameSize();
            int validLen = chunk.length - (chunk.length % Math.max(frameSize, 1));
            if (validLen > 0) {
                hautParleur.write(chunk, 0, validLen);
            }
        }
    }

    public void arreter() {
        if (hautParleur != null) {
            hautParleur.drain();
            hautParleur.stop();
            hautParleur.close();
        }
    }
}

