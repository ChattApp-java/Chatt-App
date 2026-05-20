package org.example.tpchatjavafx.client.controller;

import org.bytedeco.javacv.OpenCVFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.example.tpchatjavafx.client.util.WindowSizingUtil;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.function.Consumer;

public class IncomingMeetingDialogController {

    @FXML
    private Label initiatorLabel;

    @FXML
    private Label meetingTypeLabel;

    @FXML
    private Label titleLabel;

    @FXML
    private Label sectionStatusLabel;

    @FXML
    private Button joinButton;

    @FXML
    private Button rejectButton;

    @FXML
    private ImageView cameraPreview;

    @FXML
    private Label previewStatusLabel;

    @FXML
    private StackPane previewPlaceholder;

    private Stage stage;
    private Consumer<Boolean> onDecision;
    private OpenCVFrameGrabber grabber;
    private Java2DFrameConverter converter;
    private Timeline previewTimeline;

    public void setStage(Stage stage) {
        this.stage = stage;
        if (stage != null) {
            stage.setOnShown(event -> startCameraPreview());
            stage.setOnHidden(event -> stopCameraPreview());
        }
    }

    public void setOnDecision(Consumer<Boolean> onDecision) {
        this.onDecision = onDecision;
    }

    public void setInitiator(String initiator) {
        String safeInitiator = (initiator == null || initiator.isBlank()) ? "Contact" : initiator;
        initiatorLabel.setText(safeInitiator);
        if (titleLabel != null) {
            titleLabel.setText("Invitation de " + safeInitiator);
        }
    }

    public void setMeetingType(String meetingType) {
        String safeType = (meetingType == null || meetingType.isBlank()) ? "AUDIO" : meetingType.toUpperCase();
        meetingTypeLabel.setText("Appel " + safeType.toLowerCase());
        if (sectionStatusLabel != null) {
            sectionStatusLabel.setText("En attente de votre reponse");
        }
    }

    @FXML
    private void onJoinMeeting() {
        stopCameraPreview();
        if (onDecision != null) onDecision.accept(true);
        if (stage != null) stage.close();
    }

    @FXML
    private void onRejectMeeting() {
        stopCameraPreview();
        if (onDecision != null) onDecision.accept(false);
        if (stage != null) stage.close();
    }

    private void startCameraPreview() {
        try {
            grabber = new OpenCVFrameGrabber(0);
            grabber.start();
            converter = new Java2DFrameConverter();
            if (previewStatusLabel != null) {
                previewStatusLabel.setText("Camera detectee");
            }
            previewTimeline = new Timeline(new KeyFrame(Duration.millis(120), event -> refreshPreviewFrame()));
            previewTimeline.setCycleCount(Timeline.INDEFINITE);
            previewTimeline.play();
        } catch (Exception e) {
            showPreviewUnavailable("Camera indisponible");
        }
    }

    private void refreshPreviewFrame() {
        if (grabber == null || cameraPreview == null || converter == null) {
            return;
        }
        try {
            Frame cvFrame = grabber.grab();
            if (cvFrame == null || cvFrame.image == null) {
                return;
            }
            BufferedImage bufferedImage = converter.convert(cvFrame);
            if (bufferedImage == null) {
                return;
            }
            javafx.scene.image.Image fxImage = javafx.embed.swing.SwingFXUtils.toFXImage(bufferedImage, null);
            Platform.runLater(() -> {
                cameraPreview.setImage(fxImage);
                if (previewPlaceholder != null) {
                    previewPlaceholder.setVisible(false);
                    previewPlaceholder.setManaged(false);
                }
            });
        } catch (Exception ignored) {
        }
    }

    private void showPreviewUnavailable(String message) {
        if (previewStatusLabel != null) {
            previewStatusLabel.setText(message);
        }
        if (previewPlaceholder != null) {
            previewPlaceholder.setVisible(true);
            previewPlaceholder.setManaged(true);
        }
    }

    private void stopCameraPreview() {
        if (previewTimeline != null) {
            previewTimeline.stop();
            previewTimeline = null;
        }
        if (grabber != null) {
            try {
                grabber.stop();
            } catch (Exception ignored) {
            }
        }
        grabber = null;
        converter = null;
    }

    public static void showInvite(String initiator, String meetingType, Consumer<Boolean> onDecision) throws IOException {
        URL resource = IncomingMeetingDialogController.class.getResource("/fxml/incoming-meeting-dialog.fxml");
        if (resource == null) {
            throw new IllegalStateException("incoming-meeting-dialog.fxml introuvable");
        }

        FXMLLoader loader = new FXMLLoader(resource);
        Parent root = loader.load();
        IncomingMeetingDialogController controller = loader.getController();
        controller.setInitiator(initiator);
        controller.setMeetingType(meetingType);
        controller.setOnDecision(onDecision);

        Stage stage = new Stage();
        controller.setStage(stage);
        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.setTitle("Invitation reunion");
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setResizable(true);
        WindowSizingUtil.applyResponsiveStageSize(stage, 1020, 620, 860, 520);
        stage.showAndWait();
    }
}
