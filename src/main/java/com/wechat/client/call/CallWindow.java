package com.wechat.client.call;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * Fenêtre d'appel active avec chronomètre et contrôles.
 */
public class CallWindow {

    private Stage window;
    private Label timerLabel;
    private Label statusLabel;
    private Timeline timer;
    private int seconds = 0;
    private boolean muted = false;
    private boolean videoEnabled = true;

    public void show(String contactName, boolean isVideo) {
        window = new Stage();
        window.setTitle("Appel avec " + contactName);

        VBox root = new VBox();
        root.setAlignment(Pos.CENTER);
        root.setSpacing(20);
        root.setPadding(new Insets(40));
        root.setStyle("-fx-background-color: #1E1E1E;");
        root.setPrefSize(400, 500);

        // Avatar
        StackPane avatar = new StackPane();
        avatar.setPrefSize(120, 120);
        Circle circle = new Circle(60);
        circle.setFill(Color.web("#07C160"));
        Label avLabel = new Label(contactName.substring(0, 1).toUpperCase());
        avLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 48));
        avLabel.setTextFill(Color.WHITE);
        avatar.getChildren().addAll(circle, avLabel);

        Label name = new Label(contactName);
        name.setFont(Font.font("Segoe UI", FontWeight.BOLD, 20));
        name.setTextFill(Color.WHITE);

        statusLabel = new Label("Appel en cours...");
        statusLabel.setFont(Font.font("Segoe UI", 14));
        statusLabel.setTextFill(Color.web("#07C160"));

        // Chronomètre
        timerLabel = new Label("00:00");
        timerLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 32));
        timerLabel.setTextFill(Color.WHITE);

        // Démarrer le chronomètre
        timer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            seconds++;
            int mins = seconds / 60;
            int secs = seconds % 60;
            timerLabel.setText(String.format("%02d:%02d", mins, secs));
        }));
        timer.setCycleCount(Timeline.INDEFINITE);
        timer.play();

        // Contrôles
        HBox controls = new HBox();
        controls.setAlignment(Pos.CENTER);
        controls.setSpacing(20);

        // Mute
        Button muteBtn = createControlButton("🎤", "#3E3E3E");
        muteBtn.setOnAction(e -> {
            muted = !muted;
            muteBtn.setStyle(muted ?
                    "-fx-background-color: #FF4444; -fx-text-fill: white; -fx-background-radius: 50%;" +
                            "-fx-min-width: 50; -fx-min-height: 50; -fx-cursor: hand; -fx-font-size: 18;" :
                    "-fx-background-color: #3E3E3E; -fx-text-fill: white; -fx-background-radius: 50%;" +
                            "-fx-min-width: 50; -fx-min-height: 50; -fx-cursor: hand; -fx-font-size: 18;"
            );
        });

        // Vidéo
        Button videoBtn = createControlButton("📹", "#3E3E3E");
        videoBtn.setOnAction(e -> {
            videoEnabled = !videoEnabled;
            videoBtn.setStyle(!videoEnabled ?
                    "-fx-background-color: #FF4444; -fx-text-fill: white; -fx-background-radius: 50%;" +
                            "-fx-min-width: 50; -fx-min-height: 50; -fx-cursor: hand; -fx-font-size: 18;" :
                    "-fx-background-color: #3E3E3E; -fx-text-fill: white; -fx-background-radius: 50%;" +
                            "-fx-min-width: 50; -fx-min-height: 50; -fx-cursor: hand; -fx-font-size: 18;"
            );
        });

        // Raccrocher (rouge)
        Button hangupBtn = createControlButton("📞", "#FF4444");
        hangupBtn.setOnAction(e -> endCall());

        controls.getChildren().addAll(muteBtn, videoBtn, hangupBtn);

        root.getChildren().addAll(avatar, name, statusLabel, timerLabel, controls);

        Scene scene = new Scene(root);
        window.setScene(scene);
        window.show();
    }

    private Button createControlButton(String emoji, String bgColor) {
        Button btn = new Button(emoji);
        btn.setStyle("-fx-background-color: " + bgColor + "; -fx-text-fill: white;" +
                "-fx-background-radius: 50%; -fx-min-width: 50; -fx-min-height: 50;" +
                "-fx-cursor: hand; -fx-font-size: 18;");
        return btn;
    }

    public void endCall() {
        if (timer != null) {
            timer.stop();
        }
        if (window != null) {
            Platform.runLater(() -> window.close());
        }
    }

    public void setConnected() {
        Platform.runLater(() -> {
            statusLabel.setText("Connecté");
            statusLabel.setTextFill(Color.web("#07C160"));
        });
    }
}