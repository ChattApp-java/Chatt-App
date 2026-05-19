package org.example.tpchatjavafx.client.voice;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
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
    private static Runnable hangupHandler;

    public static void open(String localUser, String remoteUser, Runnable onHangup) {
        hangupHandler = onHangup;

        if (currentStage != null) {
            currentStage.toFront();
            return;
        }

        Platform.runLater(() -> {
            currentStage = new Stage();
            currentStage.setTitle("Appel vocal - " + remoteUser);

            Label callType = new Label("Appel vocal");
            callType.setStyle("-fx-font-size: 14px; -fx-text-fill: #0f6aa6; -fx-font-weight: bold;");

            Circle avatarCircle = new Circle(42, Color.web("#36a9e1"));
            Label avatarLetter = new Label(remoteUser != null && !remoteUser.isBlank()
                    ? remoteUser.substring(0, 1).toUpperCase()
                    : "?");
            avatarLetter.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: white;");
            StackPane avatar = new StackPane(avatarCircle, avatarLetter);

            Label title = new Label(remoteUser);
            title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #17324d;");

            Label subtitle = new Label("Connexion audio en cours");
            subtitle.setStyle("-fx-font-size: 13px; -fx-text-fill: #5f7c95;");

            timerLabel = new Label("00:00");
            timerLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #17324d;");

            Button endButton = new Button("Raccrocher");
            endButton.setStyle(
                    "-fx-background-color: #ff315a;" +
                            "-fx-text-fill: white;" +
                            "-fx-font-size: 14px;" +
                            "-fx-font-weight: bold;" +
                            "-fx-padding: 12 28 12 28;" +
                            "-fx-background-radius: 26;"
            );
            endButton.setOnAction(e -> {
                if (hangupHandler != null) {
                    hangupHandler.run();
                }
                closeInternal();
            });

            VBox root = new VBox(14, callType, avatar, title, subtitle, timerLabel, endButton);
            root.setAlignment(Pos.CENTER);
            root.setPadding(new Insets(28));
            root.setStyle("-fx-background-color: linear-gradient(to bottom, #eef8ff, #dceefe);");

            Scene scene = new Scene(root, 340, 420);
            currentStage.setScene(scene);
            currentStage.initModality(Modality.NONE);
            currentStage.setResizable(true);
            WindowSizingUtil.applyResponsiveStageSize(currentStage, 340, 420, 300, 360);
            currentStage.setOnCloseRequest(e -> {
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

    public static void close() {
        Platform.runLater(VoiceCallWindow::closeInternal);
    }
}
