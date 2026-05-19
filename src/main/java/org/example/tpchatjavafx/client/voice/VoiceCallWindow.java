package org.example.tpchatjavafx.client.voice;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.example.tpchatjavafx.client.util.WindowSizingUtil;

public class VoiceCallWindow {

    private static Stage currentStage;
    private static Timeline timeline;
    private static int seconds = 0;
    private static Label timerLabel;
    private static Label statusLabel;
    private static Runnable hangupHandler;
    private static Runnable toggleMicHandler;
    private static Runnable toggleSpeakerHandler;
    private static Button micButton;
    private static Button speakerButton;
    private static boolean micMuted;
    private static boolean speakerEnabled = true;

    public static void open(String localUser, String remoteUser, Runnable onHangup,
                            Runnable onToggleMic, Runnable onToggleSpeaker) {
        hangupHandler = onHangup;
        toggleMicHandler = onToggleMic;
        toggleSpeakerHandler = onToggleSpeaker;
        micMuted = false;
        speakerEnabled = true;

        if (currentStage != null) {
            currentStage.toFront();
            return;
        }

        Platform.runLater(() -> {
            currentStage = new Stage();
            currentStage.setTitle("Appel vocal - " + remoteUser);

            Label callType = new Label("Appel vocal prive");
            callType.setStyle("-fx-font-size: 15px; -fx-text-fill: #0f6aa6; -fx-font-weight: bold;");

            Circle outerRing = new Circle(58, Color.web("#c9ecff"));
            Circle avatarCircle = new Circle(48, Color.web("#36a9e1"));
            Label avatarLetter = new Label(remoteUser != null && !remoteUser.isBlank()
                    ? remoteUser.substring(0, 1).toUpperCase()
                    : "?");
            avatarLetter.setStyle("-fx-font-size: 34px; -fx-font-weight: bold; -fx-text-fill: white;");
            StackPane avatar = new StackPane(outerRing, avatarCircle, avatarLetter);

            Label title = new Label(remoteUser);
            title.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: #17324d;");

            statusLabel = new Label("Connexion audio active");
            statusLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #5f7c95;");

            timerLabel = new Label("00:00");
            timerLabel.setStyle("-fx-font-size: 26px; -fx-font-weight: bold; -fx-text-fill: #17324d;");

            Label hint = new Label("Parlez librement, vous pouvez couper le micro ou le haut-parleur.");
            hint.setWrapText(true);
            hint.setMaxWidth(280);
            hint.setAlignment(Pos.CENTER);
            hint.setStyle("-fx-font-size: 12px; -fx-text-fill: #7390a8;");

            micButton = buildRoundButton("Micro", "#f2f7fb", "#17324d");
            micButton.setOnAction(e -> {
                micMuted = !micMuted;
                updateMicButton();
                updateStatusLabel();
                if (toggleMicHandler != null) {
                    toggleMicHandler.run();
                }
            });

            speakerButton = buildRoundButton("Haut-parleur", "#ffffff", "#0f6aa6");
            speakerButton.setOnAction(e -> {
                speakerEnabled = !speakerEnabled;
                updateSpeakerButton();
                updateStatusLabel();
                if (toggleSpeakerHandler != null) {
                    toggleSpeakerHandler.run();
                }
            });

            Button endButton = new Button("Raccrocher");
            endButton.setStyle(
                    "-fx-background-color: #ff315a;" +
                            "-fx-text-fill: white;" +
                            "-fx-font-size: 14px;" +
                            "-fx-font-weight: bold;" +
                            "-fx-padding: 12 26 12 26;" +
                            "-fx-background-radius: 26;"
            );
            endButton.setOnAction(e -> {
                if (hangupHandler != null) {
                    hangupHandler.run();
                }
                closeInternal();
            });

            HBox controls = new HBox(14, micButton, speakerButton, endButton);
            controls.setAlignment(Pos.CENTER);

            VBox root = new VBox(16, callType, avatar, title, statusLabel, timerLabel, hint, controls);
            root.setAlignment(Pos.CENTER);
            root.setPadding(new Insets(30));
            root.setStyle(
                    "-fx-background-color: linear-gradient(to bottom, #eef8ff, #dceefe);" +
                            "-fx-border-color: #b9def6;" +
                            "-fx-border-width: 1;"
            );

            Scene scene = new Scene(root, 420, 500);
            currentStage.setScene(scene);
            currentStage.initModality(Modality.NONE);
            currentStage.setResizable(true);
            WindowSizingUtil.applyResponsiveStageSize(currentStage, 420, 500, 320, 420);
            currentStage.setOnCloseRequest(e -> {
                if (hangupHandler != null) {
                    hangupHandler.run();
                }
                closeInternal();
                e.consume();
            });

            updateMicButton();
            updateSpeakerButton();
            updateStatusLabel();
            currentStage.show();
            startTimer();
        });
    }

    private static Button buildRoundButton(String text, String background, String textColor) {
        Button button = new Button(text);
        button.setStyle(
                "-fx-background-color: " + background + ";" +
                        "-fx-text-fill: " + textColor + ";" +
                        "-fx-font-size: 13px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 12 18 12 18;" +
                        "-fx-background-radius: 24;"
        );
        return button;
    }

    private static void updateMicButton() {
        if (micButton == null) return;
        micButton.setText(micMuted ? "Micro coupe" : "Micro actif");
        micButton.setStyle(
                "-fx-background-color: " + (micMuted ? "#ffd9df" : "#f2f7fb") + ";" +
                        "-fx-text-fill: " + (micMuted ? "#c21f45" : "#17324d") + ";" +
                        "-fx-font-size: 13px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 12 18 12 18;" +
                        "-fx-background-radius: 24;"
        );
    }

    private static void updateSpeakerButton() {
        if (speakerButton == null) return;
        speakerButton.setText(speakerEnabled ? "Haut-parleur actif" : "Haut-parleur coupe");
        speakerButton.setStyle(
                "-fx-background-color: " + (speakerEnabled ? "#ffffff" : "#ffd9df") + ";" +
                        "-fx-text-fill: " + (speakerEnabled ? "#0f6aa6" : "#c21f45") + ";" +
                        "-fx-font-size: 13px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 12 18 12 18;" +
                        "-fx-background-radius: 24;"
        );
    }

    private static void updateStatusLabel() {
        if (statusLabel == null) return;
        if (micMuted && !speakerEnabled) {
            statusLabel.setText("Micro et haut-parleur coupes");
        } else if (micMuted) {
            statusLabel.setText("Micro coupe");
        } else if (!speakerEnabled) {
            statusLabel.setText("Haut-parleur coupe");
        } else {
            statusLabel.setText("Connexion audio active");
        }
    }

    private static void startTimer() {
        seconds = 0;
        if (timeline != null) {
            timeline.stop();
        }

        timeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            seconds++;
            if (timerLabel != null) {
                int m = seconds / 60;
                int s = seconds % 60;
                timerLabel.setText(String.format("%02d:%02d", m, s));
            }
        }));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }

    private static void closeInternal() {
        if (timeline != null) {
            timeline.stop();
            timeline = null;
        }
        if (currentStage != null) {
            currentStage.close();
            currentStage = null;
        }
        timerLabel = null;
        statusLabel = null;
        micButton = null;
        speakerButton = null;
        seconds = 0;
        hangupHandler = null;
        toggleMicHandler = null;
        toggleSpeakerHandler = null;
    }

    public static void close() {
        Platform.runLater(VoiceCallWindow::closeInternal);
    }
}
