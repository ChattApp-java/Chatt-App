import javax.sound.sampled.*;

public class LecteurAudio {

    private SourceDataLine hautParleur;
    private final javax.sound.sampled.AudioFormat format;

    public LecteurAudio() {
        // MÊME format que CapteurAudio — obligatoire
        this.format = new javax.sound.sampled.AudioFormat(
                44100, 16, 1, true, false
        );
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

    public void arreter() {
        if (hautParleur != null) {
            hautParleur.drain();
            hautParleur.stop();
            hautParleur.close();
        }
    }
}