package org.example.tpchatjavafx.client.video;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.example.tpchatjavafx.client.NetworkClient;
import org.example.tpchatjavafx.client.audio.AudioCaptureService;
import org.example.tpchatjavafx.client.audio.AudioFormatUtil;
import org.example.tpchatjavafx.client.model.ChatMessage;
import org.example.tpchatjavafx.common.MessageType;
import com.github.sarxos.webcam.Webcam;
import javax.sound.sampled.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Timer;
import java.util.TimerTask;

public class VideoCallController {

    // Les ImageView pour afficher la vidéo locale et distante dans l'UI
    @FXML private ImageView localVideo;
    @FXML private ImageView remoteVideo;

    // Singleton pour pouvoir recevoir les frames même depuis des méthodes statiques
    private static VideoCallController instance;

    private NetworkClient networkClient; // Pour envoyer les messages vidéo
    private String username;             // Mon pseudo
    private String otherUser;            // Pseudo de l'autre personne
    private Timer cameraTimer;           // Timer pour envoyer les frames périodiquement
    private Webcam webcam;               // Objet pour accéder à la webcam
    
    // Audio
    private TargetDataLine mic;
    private SourceDataLine speakers;
    private volatile boolean audioRunning = false;
    private Thread audioCaptureThread;
    private AudioFormat captureFormat;
    private AudioFormat playbackFormat;

    /**
     * Initialise le contrôleur avec le client réseau et les pseudos des deux utilisateurs
     */
    public void init(NetworkClient client, String me, String other) {
        instance = this;                  // Définit le singleton
        this.networkClient = client;      // Client réseau pour l'envoi des messages
        this.username = me;               // Mon pseudo
        this.otherUser = other;           // Pseudo de l'autre utilisateur

        // Ouvre la webcam par défaut
        webcam = Webcam.getDefault();
        if (webcam != null) webcam.open();

        // Démarre la capture et l'envoi des frames de la webcam
        startSendingCameraFrames();
        
        // Initialise et démarre l'audio
        try {
            startAudio();
        } catch (LineUnavailableException e) {
            System.err.println("Impossible de démarrer l'audio : " + e.getMessage());
        }
    }

    /**
     * Capture la webcam et envoie les frames toutes les 100ms (≈10 FPS)
     */
    private void startSendingCameraFrames() {
        cameraTimer = new Timer(true); // Timer en mode "daemon" (ne bloque pas la fermeture)

        cameraTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    // Vérifie si la webcam est ouverte
                    if (webcam == null || !webcam.isOpen()) return;

                    BufferedImage img = webcam.getImage(); // Capture une frame
                    if (img == null) return;

                    // Convertit BufferedImage en Image JavaFX pour affichage
                    Image fxImage = javafx.embed.swing.SwingFXUtils.toFXImage(img, null);
                    Platform.runLater(() -> localVideo.setImage(fxImage)); // Affiche localement

                    // Convertit l'image en tableau de bytes (JPG)
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    ImageIO.write(img, "jpg", baos);
                    byte[] data = baos.toByteArray();

                    // Crée un message vidéo et envoie au serveur / autre utilisateur
                    ChatMessage msg = new ChatMessage(
                            MessageType.VIDEO_FRAME,
                            username,
                            otherUser,
                            null,
                            null
                    );
                    msg.setBinaryData(data);       // Ajoute les données de l'image
                    networkClient.send(msg);       // Envoie le message
                } catch (Exception ignored) {}       // Ignorer les exceptions pour ne pas casser le timer
            }
        }, 0, 100); // 0 = pas de délai initial, 100ms = intervalle
    }

    /**
     * Reçoit une frame vidéo de l'autre utilisateur et l'affiche
     */
    public static void receiveFrame(byte[] data) {
        if (instance == null) return;

        try {
            // Convertit le tableau de bytes en Image JavaFX
            Image fxImage = new Image(new java.io.ByteArrayInputStream(data));
            Platform.runLater(() -> instance.remoteVideo.setImage(fxImage)); // Affiche à l'UI
        } catch (Exception ignored) {}
    }

    private void startAudio() throws LineUnavailableException {
        AudioFormat format = AudioCaptureService.findBestDuplexFormat();
        if (format == null) {
            format = new AudioFormat(44100, 16, 1, true, false);
        }
        DataLine.Info micInfo = new DataLine.Info(TargetDataLine.class, format);
        DataLine.Info spkInfo = new DataLine.Info(SourceDataLine.class, format);

        mic = (TargetDataLine) AudioSystem.getLine(micInfo);
        mic.open(format);
        mic.start();
        captureFormat = format;

        speakers = (SourceDataLine) AudioSystem.getLine(spkInfo);
        speakers.open(format);
        speakers.start();
        playbackFormat = format;

        audioRunning = true;
        audioCaptureThread = new Thread(this::audioCaptureLoop, "video-audio-capture");
        audioCaptureThread.setDaemon(true);
        audioCaptureThread.start();
    }

    private void audioCaptureLoop() {
        byte[] buffer = new byte[2048];
        try {
            while (audioRunning) {
                int count = mic.read(buffer, 0, buffer.length);
                if (count > 0) {
                    byte[] frame = new byte[count];
                    System.arraycopy(buffer, 0, frame, 0, count);
                    if (!AudioFormatUtil.sameFormat(captureFormat, AudioFormatUtil.NETWORK_FORMAT)) {
                        frame = AudioFormatUtil.convert(frame, captureFormat, AudioFormatUtil.NETWORK_FORMAT);
                    }

                    ChatMessage msg = new ChatMessage(MessageType.VOICE_FRAME, username, otherUser, null, "vframe");
                    msg.setBinaryData(frame);
                    networkClient.send(msg);
                }
            }
        } catch (Exception ignored) {}
    }

    public static void receiveAudio(byte[] data) {
        if (instance != null && instance.speakers != null && instance.audioRunning) {
            if (!AudioFormatUtil.sameFormat(AudioFormatUtil.NETWORK_FORMAT, instance.playbackFormat)) {
                data = AudioFormatUtil.convert(data, AudioFormatUtil.NETWORK_FORMAT, instance.playbackFormat);
            }
            instance.speakers.write(data, 0, data.length);
        }
    }

    /**
     * Termine l'appel vidéo
     */
    @FXML
    public void onEndCall() {
        // Ferme la webcam si elle est ouverte
        if (webcam != null && webcam.isOpen()) webcam.close();
        // Arrête le timer pour arrêter d'envoyer les frames
        if (cameraTimer != null) cameraTimer.cancel();
        
        // Arrête l'audio
        audioRunning = false;
        if (mic != null) { mic.stop(); mic.close(); }
        if (speakers != null) { speakers.stop(); speakers.close(); }

        // Envoie un message indiquant la fin de l'appel
        if (networkClient != null) {
            networkClient.send(new ChatMessage(
                    MessageType.VIDEO_CALL_END,
                    username,
                    otherUser,
                    null,
                    ""
            ));
        }

        // Ferme la fenêtre d'appel vidéo
        VideoCallWindow.closeCurrent();
    }
}
