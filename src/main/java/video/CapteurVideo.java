package video;

import com.github.sarxos.webcam.Webcam;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

/**
 * Capture la webcam.
 */
public class CapteurVideo {

    private Webcam webcam;

    public void demarrer() {
        webcam = Webcam.getDefault();
        if (webcam != null && !webcam.isOpen()) {
            webcam.open();
        }
    }

    public byte[] capturerImage() throws Exception {
        if (webcam == null || !webcam.isOpen()) return null;

        BufferedImage image = webcam.getImage();
        if (image == null) return null;

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", baos);
        return baos.toByteArray();
    }

    public boolean estActif() {
        return webcam != null && webcam.isOpen();
    }

    public void arreter() {
        if (webcam != null) {
            webcam.close();
        }
    }
}