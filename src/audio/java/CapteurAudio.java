import javax.sound.sampled.*;

public class CapteurAudio {

    private TargetDataLine microphone;
    private final javax.sound.sampled.AudioFormat format;
    public static final int CHUNK_SIZE = 4096;

    public CapteurAudio() {
        this.format = new javax.sound.sampled.AudioFormat(
                44100, 16, 1, true, false
        );
    }

    public void demarrer() throws LineUnavailableException {
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

        if (!AudioSystem.isLineSupported(info)) {
            throw new LineUnavailableException("Microphone non supporté");
        }

        microphone = (TargetDataLine) AudioSystem.getLine(info);
        microphone.open(format);
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
}