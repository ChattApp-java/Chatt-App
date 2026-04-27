package audio;

import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;

public class EnregistreurAudio {

    private TargetDataLine line;
    private AudioFileFormat.Type fileType = AudioFileFormat.Type.WAVE;
    private File wavFile;

    public void demarrer(String fileName) throws LineUnavailableException {
        this.wavFile = new File(fileName);
        AudioFormat format = new AudioFormat(44100, 16, 1, true, false);
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

        if (!AudioSystem.isLineSupported(info)) {
            throw new LineUnavailableException("Microphone non supporté");
        }

        line = (TargetDataLine) AudioSystem.getLine(info);
        line.open(format);
        line.start();

        Thread stopper = new Thread(() -> {
            AudioInputStream ais = new AudioInputStream(line);
            try {
                AudioSystem.write(ais, fileType, wavFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        stopper.start();
    }

    public void arreter() {
        if (line != null) {
            line.stop();
            line.close();
        }
    }
}
