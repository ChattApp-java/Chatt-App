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
        initiatorLabel.setText("Invite par : " + initiator);
    }

    public void setMeetingType(String meetingType) {
        meetingTypeLabel.setText("Type : " + meetingType);
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
        WindowSizingUtil.applyResponsiveStageSize(stage, 430, 260, 360, 220);
        stage.showAndWait();
    }
}
