package audio;

import javax.sound.sampled.*;

/**
 * Joue l'audio sur les haut-parleurs.
 */
public class LecteurAudio {

    private SourceDataLine hautParleur;
    private final javax.sound.sampled.AudioFormat format;

    public LecteurAudio() {
        this.format = new javax.sound.sampled.AudioFormat(44100, 16, 1, true, false);
    }

    public void demarrer() throws LineUnavailableException {
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
        hautParleur = (SourceDataLine) AudioSystem.getLine(info);
        hautParleur.open(format);
        hautParleur.start();
    }

    public void jouer(byte[] chunk) {
        if (hautParleur != null && hautParleur.isOpen()) {
            hautParleur.write(chunk, 0, chunk.length);
        }
    }

    public void jouer(byte[] chunk, int len) {
        if (hautParleur != null && hautParleur.isOpen()) {
            hautParleur.write(chunk, 0, len);
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