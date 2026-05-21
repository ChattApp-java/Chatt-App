package org.example.tpchatjavafx.client.video;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_videoio.VideoCapture;
import org.example.tpchatjavafx.client.NetworkClient;
import org.example.tpchatjavafx.client.audio.AudioCaptureService;
import org.example.tpchatjavafx.client.audio.AudioFormatUtil;
import org.example.tpchatjavafx.client.model.ChatMessage;
import org.example.tpchatjavafx.common.MessageType;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;

public class VideoCallController {
    private static final int CAMERA_WIDTH = 480;
    private static final int CAMERA_HEIGHT = 360;
    private static final int FRAME_INTERVAL_MS = 120;
    private static final float JPEG_QUALITY = 0.70f;

    @FXML private ImageView localVideo;
    @FXML private ImageView remoteVideo;
    @FXML private Label remoteNameLabel;
    @FXML private Label remoteNameOverlayLabel;
    @FXML private Label statusLabel;
    @FXML private Label timerLabel;
    @FXML private Label callTypeLabel;
    @FXML private Button videoButton;
    @FXML private Button speakerButton;
    @FXML private Button micButton;
    @FXML private Button endCallButton;
    @FXML private VBox remotePlaceholder;
    @FXML private VBox localPlaceholder;

    private static VideoCallController instance;

    private NetworkClient networkClient;
    private String username;
    private String otherUser;
    private Timer cameraTimer;

    // ========== JAVACV - Remplacement de Webcam ==========
    private VideoCapture videoCapture;

    private TargetDataLine mic;
    private SourceDataLine speakers;
    private volatile boolean audioRunning;
    private Thread audioCaptureThread;
    private AudioFormat captureFormat;
    private AudioFormat playbackFormat;

    private Timeline timerTimeline;
    private int elapsedSeconds;
    private volatile boolean microphoneMuted;
    private volatile boolean speakerEnabled = true;
    private volatile boolean videoEnabled = true;
    private int sentFrames;
    private int receivedFrames;

    public void init(NetworkClient client, String me, String other) {
        instance = this;
        this.networkClient = client;
        this.username = me;
        this.otherUser = other;
        this.microphoneMuted = false;
        this.speakerEnabled = true;
        this.videoEnabled = true;

        remoteNameLabel.setText(other != null && !other.isBlank() ? other : "Contact");
        if (remoteNameOverlayLabel != null) {
            remoteNameOverlayLabel.setText(other != null && !other.isBlank() ? other : "Contact");
        }
        callTypeLabel.setText("Appel video");
        statusLabel.setText("Connexion video active");
        remotePlaceholder.setVisible(true);
        remotePlaceholder.setManaged(true);
        localPlaceholder.setVisible(false);
        localPlaceholder.setManaged(false);
        localVideo.setImage(null);
        remoteVideo.setImage(null);
        updateMicButton();
        updateSpeakerButton();
        updateVideoButton();
        startTimer();

        // ========== JAVACV - Ouverture webcam ==========
        videoCapture = new VideoCapture(0);
        if (videoCapture.isOpened()) {
            configureCamera(videoCapture);
            System.out.println("[VIDEO_CALL] Webcam ouverte avec JavaCV");
        } else {
            videoEnabled = false;
            localPlaceholder.setVisible(true);
            localPlaceholder.setManaged(true);
            statusLabel.setText("Aucune camera detectee");
        }

        startSendingCameraFrames();

        try {
            startAudio();
        } catch (LineUnavailableException e) {
            statusLabel.setText("Audio indisponible sur ce PC");
            System.err.println("Impossible de demarrer l'audio : " + e.getMessage());
        }
    }

    // ========== JAVACV - Capture et envoi ==========
    private void startSendingCameraFrames() {
        cameraTimer = new Timer(true);
        cameraTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (!videoEnabled || videoCapture == null || !videoCapture.isOpened()) {
                    return;
                }

                try {
                    Mat frame = new Mat();
                    if (!videoCapture.read(frame)) {
                        return;
                    }

                    // OpenCV fournit deja les pixels en BGR, ce format est attendu par TYPE_3BYTE_BGR.
                    BufferedImage img = matToBufferedImage(frame);
                    if (img == null) {
                        frame.release();
                        return;
                    }

                    Image fxImage = javafx.embed.swing.SwingFXUtils.toFXImage(img, null);
                    Platform.runLater(() -> {
                        localPlaceholder.setVisible(false);
                        localPlaceholder.setManaged(false);
                        localVideo.setImage(fxImage);
                    });

                    byte[] data = encodeJpeg(img);
                    if (data == null || data.length == 0) {
                        frame.release();
                        return;
                    }

                    // ENVOI TCP (inchangé)
                    ChatMessage msg = new ChatMessage(
                            MessageType.VIDEO_FRAME,
                            username,
                            otherUser,
                            null,
                            null
                    );
                    msg.setBinaryData(data);
                    networkClient.send(msg);
                    sentFrames++;
                    if (sentFrames % 30 == 0) {
                        System.out.println("[VIDEO_CALL] " + username + " a envoye " + sentFrames
                                + " frames vers " + otherUser + " (dernier=" + data.length + " octets)");
                    }

                    frame.release();

                } catch (Exception e) {
                    System.err.println("[VIDEO_CALL] Erreur envoi frame: " + e.getMessage());
                }
            }
        }, 0, FRAME_INTERVAL_MS);
    }

    private void configureCamera(VideoCapture capture) {
        if (capture == null || !capture.isOpened()) {
            return;
        }
        capture.set(org.bytedeco.opencv.global.opencv_videoio.CAP_PROP_FRAME_WIDTH, CAMERA_WIDTH);
        capture.set(org.bytedeco.opencv.global.opencv_videoio.CAP_PROP_FRAME_HEIGHT, CAMERA_HEIGHT);
        capture.set(org.bytedeco.opencv.global.opencv_videoio.CAP_PROP_FPS, 15);
    }

    // ========== JAVACV - Conversion Mat -> BufferedImage ==========
    private BufferedImage matToBufferedImage(Mat mat) {
        int width = mat.cols();
        int height = mat.rows();
        int channels = mat.channels();

        byte[] sourcePixels = new byte[width * height * channels];
        mat.data().get(sourcePixels);

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
        final byte[] targetPixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
        System.arraycopy(sourcePixels, 0, targetPixels, 0, sourcePixels.length);

        return image;
    }

    private byte[] encodeJpeg(BufferedImage image) throws IOException {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
        if (!writers.hasNext()) {
            ByteArrayOutputStream fallback = new ByteArrayOutputStream();
            ImageIO.write(image, "jpg", fallback);
            return fallback.toByteArray();
        }

        ImageWriter writer = writers.next();
        ByteArrayOutputStream outputBytes = new ByteArrayOutputStream();
        try (MemoryCacheImageOutputStream output = new MemoryCacheImageOutputStream(outputBytes)) {
            writer.setOutput(output);
            ImageWriteParam params = writer.getDefaultWriteParam();
            if (params.canWriteCompressed()) {
                params.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                params.setCompressionQuality(JPEG_QUALITY);
            }
            writer.write(null, new javax.imageio.IIOImage(image, null, null), params);
        } finally {
            writer.dispose();
        }
        return outputBytes.toByteArray();
    }

    public static void receiveFrame(byte[] data) {
        if (data == null || data.length == 0) {
            return;
        }
        if (instance == null) {
            System.out.println("[VIDEO_CALL] Frame distante recue mais aucune fenetre video n'est active");
            return;
        }

        try {
            Image fxImage = new Image(new ByteArrayInputStream(data));
            Platform.runLater(() -> {
                if (instance == null) {
                    return;
                }
                if (fxImage.isError()) {
                    System.err.println("[VIDEO_CALL] Image distante invalide");
                    return;
                }
                instance.receivedFrames++;
                instance.remoteVideo.setImage(fxImage);
                instance.remotePlaceholder.setVisible(false);
                instance.remotePlaceholder.setManaged(false);
                if (!instance.videoEnabled) {
                    instance.statusLabel.setText("Camera locale coupee, video distante visible");
                } else {
                    instance.statusLabel.setText("Video en direct");
                }
                if (instance.receivedFrames % 30 == 0) {
                    System.out.println("[VIDEO_CALL] " + instance.username + " a affiche "
                            + instance.receivedFrames + " frames distantes");
                }
            });
        } catch (Exception e) {
            System.err.println("[VIDEO_CALL] Erreur reception frame: " + e.getMessage());
        }
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
                if (count <= 0 || microphoneMuted) {
                    continue;
                }

                byte[] frame = new byte[count];
                System.arraycopy(buffer, 0, frame, 0, count);
                if (!AudioFormatUtil.sameFormat(captureFormat, AudioFormatUtil.NETWORK_FORMAT)) {
                    frame = AudioFormatUtil.convert(frame, captureFormat, AudioFormatUtil.NETWORK_FORMAT);
                }

                ChatMessage msg = new ChatMessage(MessageType.VOICE_FRAME, username, otherUser, null, "vframe");
                msg.setBinaryData(frame);
                networkClient.send(msg);
            }
        } catch (Exception ignored) {
        }
    }

    public static void receiveAudio(byte[] data) {
        if (instance == null || instance.speakers == null || !instance.audioRunning || data == null) {
            return;
        }
        if (!instance.speakerEnabled) {
            return;
        }

        if (!AudioFormatUtil.sameFormat(AudioFormatUtil.NETWORK_FORMAT, instance.playbackFormat)) {
            data = AudioFormatUtil.convert(data, AudioFormatUtil.NETWORK_FORMAT, instance.playbackFormat);
        }
        instance.speakers.write(data, 0, data.length);
    }

    @FXML
    public void onToggleMic() {
        microphoneMuted = !microphoneMuted;
        updateMicButton();
        refreshStatusText();
    }

    @FXML
    public void onToggleSpeaker() {
        speakerEnabled = !speakerEnabled;
        updateSpeakerButton();
        refreshStatusText();
    }

    @FXML
    public void onToggleVideo() {
        videoEnabled = !videoEnabled;
        if (!videoEnabled) {
            localVideo.setImage(null);
            localPlaceholder.setVisible(true);
            localPlaceholder.setManaged(true);
        } else if (videoCapture == null || !videoCapture.isOpened()) {
            statusLabel.setText("Aucune camera detectee");
        }
        updateVideoButton();
        refreshStatusText();
    }

    @FXML
    public void onEndCall() {
        cleanupMedia();

        if (networkClient != null) {
            networkClient.send(new ChatMessage(
                    MessageType.VIDEO_CALL_END,
                    username,
                    otherUser,
                    null,
                    ""
            ));
        }

        VideoCallWindow.closeCurrent();
    }

    private void cleanupMedia() {
        if (cameraTimer != null) {
            cameraTimer.cancel();
            cameraTimer = null;
        }

        // ========== JAVACV - Fermeture ==========
        if (videoCapture != null && videoCapture.isOpened()) {
            videoCapture.release();
        }
        videoCapture = null;

        audioRunning = false;
        if (audioCaptureThread != null) {
            try {
                audioCaptureThread.join(300);
            } catch (InterruptedException ignored) {
            }
            audioCaptureThread = null;
        }

        if (mic != null) {
            mic.stop();
            mic.close();
            mic = null;
        }

        if (speakers != null) {
            speakers.stop();
            speakers.close();
            speakers = null;
        }

        if (timerTimeline != null) {
            timerTimeline.stop();
            timerTimeline = null;
        }

        instance = null;
    }

    private void startTimer() {
        elapsedSeconds = 0;
        timerLabel.setText("00:00");
        if (timerTimeline != null) {
            timerTimeline.stop();
        }

        timerTimeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            elapsedSeconds++;
            int minutes = elapsedSeconds / 60;
            int seconds = elapsedSeconds % 60;
            timerLabel.setText(String.format("%02d:%02d", minutes, seconds));
        }));
        timerTimeline.setCycleCount(Timeline.INDEFINITE);
        timerTimeline.play();
    }

    private void refreshStatusText() {
        if (!videoEnabled && microphoneMuted && !speakerEnabled) {
            statusLabel.setText("Camera, micro et haut-parleur coupes");
        } else if (!videoEnabled && microphoneMuted) {
            statusLabel.setText("Camera et micro coupes");
        } else if (!videoEnabled && !speakerEnabled) {
            statusLabel.setText("Camera et haut-parleur coupes");
        } else if (microphoneMuted && !speakerEnabled) {
            statusLabel.setText("Micro et haut-parleur coupes");
        } else if (!videoEnabled) {
            statusLabel.setText("Camera locale coupee");
        } else if (microphoneMuted) {
            statusLabel.setText("Micro coupe");
        } else if (!speakerEnabled) {
            statusLabel.setText("Haut-parleur coupe");
        } else if (remoteVideo.getImage() != null) {
            statusLabel.setText("Video en direct");
        } else {
            statusLabel.setText("Connexion video active");
        }
    }

    private void updateMicButton() {
        micButton.setText(microphoneMuted ? "Mic off" : "Mic");
        micButton.setStyle(
                "-fx-background-color: " + (microphoneMuted ? "#f15c6d" : "#233138") + ";" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 12px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-min-width: 58px;" +
                        "-fx-min-height: 58px;" +
                        "-fx-max-width: 58px;" +
                        "-fx-max-height: 58px;" +
                        "-fx-background-radius: 29;"
        );
    }

    private void updateSpeakerButton() {
        speakerButton.setText(speakerEnabled ? "HP" : "HP off");
        speakerButton.setStyle(
                "-fx-background-color: " + (speakerEnabled ? "#233138" : "#f15c6d") + ";" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 12px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-min-width: 58px;" +
                        "-fx-min-height: 58px;" +
                        "-fx-max-width: 58px;" +
                        "-fx-max-height: 58px;" +
                        "-fx-background-radius: 29;"
        );
    }

    private void updateVideoButton() {
        videoButton.setText(videoEnabled ? "Cam" : "Cam off");
        videoButton.setStyle(
                "-fx-background-color: " + (videoEnabled ? "#233138" : "#f15c6d") + ";" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 12px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-min-width: 58px;" +
                        "-fx-min-height: 58px;" +
                        "-fx-max-width: 58px;" +
                        "-fx-max-height: 58px;" +
                        "-fx-background-radius: 29;"
        );
        endCallButton.setStyle(
                "-fx-background-color: #f15c6d;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 12px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-min-width: 58px;" +
                        "-fx-min-height: 58px;" +
                        "-fx-max-width: 58px;" +
                        "-fx-max-height: 58px;" +
                        "-fx-background-radius: 29;"
        );
    }
}
