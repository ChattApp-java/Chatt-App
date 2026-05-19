package org.example.tpchatjavafx.client.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.example.tpchatjavafx.client.util.WindowSizingUtil;

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

    private Stage stage;
    private Consumer<Boolean> onDecision;

    public void setStage(Stage stage) {
        this.stage = stage;
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
        if (onDecision != null) onDecision.accept(true);
        if (stage != null) stage.close();
    }

    @FXML
    private void onRejectMeeting() {
        if (onDecision != null) onDecision.accept(false);
        if (stage != null) stage.close();
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
