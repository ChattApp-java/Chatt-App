package org.example.tpchatjavafx.client.voice;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

public class VoiceCallWindow {

    private static Stage currentStage;
    private static Timeline timeline;
    private static int seconds = 0;
    private static Label timerLabel;
    private static Runnable hangupHandler;

    /**
     * Open the voice call window.
     *
     * @param localUser  your username (not really used now but you can show it)
     * @param remoteUser other user's username
     * @param onHangup   callback executed when user ends the call (button or X)
     */
    public static void open(String localUser, String remoteUser, Runnable onHangup) {
        hangupHandler = onHangup;

        if (currentStage != null) {
            currentStage.toFront();
            return;
        }

        Platform.runLater(() -> {
            currentStage = new Stage();
            currentStage.setTitle("Voice Call");

            Label title = new Label("Voice call with " + remoteUser);
            title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

            timerLabel = new Label("00:00");
            timerLabel.setStyle("-fx-font-size: 14px;");

            Button endButton = new Button("End Call");
            endButton.setStyle(
                    "-fx-background-color: #ef4444;" +
                            "-fx-text-fill: white;" +
                            "-fx-font-weight: bold;" +
                            "-fx-padding: 8 16 8 16;" +
                            "-fx-background-radius: 20;"
            );
            endButton.setOnAction(e -> {
                if (hangupHandler != null) {
                    hangupHandler.run();
                }
                closeInternal();
            });

            VBox root = new VBox(15, title, timerLabel, endButton);
            root.setAlignment(Pos.CENTER);
            root.setPadding(new Insets(20));
            root.setPrefSize(260, 150);

            Scene scene = new Scene(root);
            currentStage.setScene(scene);
            currentStage.initModality(Modality.NONE);
            currentStage.setResizable(false);
            currentStage.setOnCloseRequest(e -> {
                // Treat window X as hangup too
                if (hangupHandler != null) {
                    hangupHandler.run();
                }
                closeInternal();
                e.consume();
            });

            currentStage.show();
            startTimer();
        });
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
        seconds = 0;
        hangupHandler = null;
    }

    /** Close the window from outside (remote ended the call) */
    public static void close() {
        Platform.runLater(VoiceCallWindow::closeInternal);
    }
}
