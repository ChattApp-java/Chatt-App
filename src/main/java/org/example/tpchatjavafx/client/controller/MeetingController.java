package org.example.tpchatjavafx.client.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.GridPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.example.tpchatjavafx.client.NetworkClient;
import org.example.tpchatjavafx.client.util.MeetingAudioMixer;
import org.example.tpchatjavafx.client.video.MeetingVideoDisplay;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;

public class MeetingController {

    @FXML
    private Label meetingTitle;

    @FXML
    private GridPane videoGrid;

    @FXML
    private ListView<String> participantsList;

    @FXML
    private Button leaveButton;

    @FXML
    private Button muteButton;

    @FXML
    private Button cameraButton;

    private final MeetingVideoDisplay videoDisplay = new MeetingVideoDisplay();
    private final MeetingAudioMixer audioMixer = new MeetingAudioMixer();

    private boolean muted;
    private boolean cameraEnabled = true;
    private NetworkClient networkClient;
    private int meetingId;

    @FXML
    public void initialize() {
        videoDisplay.setGrid(videoGrid);
        videoGrid.getChildren().clear();
        participantsList.getItems().clear();
    }

    public void init(NetworkClient networkClient, int meetingId, String title) {
        this.networkClient = networkClient;
        this.meetingId = meetingId;
        meetingTitle.setText(Objects.requireNonNullElse(title, "Réunion"));
        
        if (networkClient != null) {
            networkClient.setActiveMeetingController(this);
            addParticipant(String.valueOf(networkClient.getUserId()), networkClient.getUsername());
        }

        try {
            audioMixer.start();
        } catch (Exception e) {
            System.err.println("Impossible de démarrer le mixeur audio de réunion : " + e.getMessage());
        }
    }

    public void addParticipant(String participantId, String displayName) {
        Platform.runLater(() -> {
            videoDisplay.addParticipant(participantId, displayName);
            if (!participantsList.getItems().contains(displayName)) {
                participantsList.getItems().add(displayName);
            }
        });
    }

    public void syncParticipants(String serialized) {
        Platform.runLater(() -> {
            videoDisplay.clearParticipants();
            participantsList.getItems().clear();
            if (serialized == null || serialized.isBlank()) return;
            for (String entry : serialized.split(",")) {
                String[] parts = entry.split(":", 2);
                if (parts.length == 2) {
                    videoDisplay.addParticipant(parts[0], parts[1]);
                    if (!participantsList.getItems().contains(parts[1])) {
                        participantsList.getItems().add(parts[1]);
                    }
                }
            }
        });
    }

    public void removeParticipant(String participantId, String displayName) {
        Platform.runLater(() -> {
            videoDisplay.removeParticipant(participantId);
            participantsList.getItems().remove(displayName);
        });
    }

    public void updateParticipantFrame(String participantId, byte[] jpegFrame) {
        Platform.runLater(() -> videoDisplay.updateFrame(participantId, jpegFrame));
    }

    public void addAudioFrame(String participantId, byte[] audioData) {
        audioMixer.addAudioFrame(participantId, audioData);
    }

    @FXML
    private void onLeaveMeeting() {
        if (networkClient != null) {
            networkClient.leaveMeeting(meetingId);
        }
        closeWindow();
    }

    @FXML
    private void onToggleMute() {
        muted = !muted;
        muteButton.setText(muted ? "Activer son" : "Muet");
    }

    @FXML
    private void onToggleCamera() {
        cameraEnabled = !cameraEnabled;
        cameraButton.setText(cameraEnabled ? "Caméra" : "Caméra off");
    }

    private void closeWindow() {
        audioMixer.stop();
        if (networkClient != null) {
            networkClient.stopMeetingMedia();
            networkClient.setActiveMeetingController(null);
        }
        Stage stage = (Stage) leaveButton.getScene().getWindow();
        stage.close();
    }

    public void handleMeetingEnded() {
        Platform.runLater(this::closeWindow);
    }

    public static void openMeetingWindow(NetworkClient networkClient, int meetingId, String title) throws IOException {
        URL resource = MeetingController.class.getResource("/fxml/meeting-window.fxml");
        if (resource == null) {
            throw new IllegalStateException("meeting-window.fxml introuvable");
        }
        FXMLLoader loader = new FXMLLoader(resource);
        Parent root = loader.load();
        MeetingController controller = loader.getController();
        controller.init(networkClient, meetingId, title);

        Stage stage = new Stage();
        Scene scene = new Scene(root, 1100, 720);
        URL css = MeetingController.class.getResource("/css/meeting.css");
        if (css != null) {
            scene.getStylesheets().add(css.toExternalForm());
        }
        stage.setScene(scene);
        stage.setTitle("Réunion — " + title);
        stage.initModality(Modality.NONE);
        stage.setMinWidth(800);
        stage.setMinHeight(600);
        stage.show();
    }
}

