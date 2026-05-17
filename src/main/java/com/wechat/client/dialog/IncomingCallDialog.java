package com.wechat.client.dialog;

import javafx.animation.FadeTransition;
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
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

/**
 * Dialog d'appel entrant (overlay urgent).
 */
public class IncomingCallDialog {

    private Boolean accepted = null; // true=accepté, false=rejeté, null=timeout

    public Boolean showAndWait(Stage owner, String callerName, boolean isVideo) {
        Stage dialog = new Stage();
        dialog.initOwner(owner);
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initStyle(StageStyle.TRANSPARENT);

        VBox root = new VBox();
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(40));
        root.setSpacing(24);
        root.setStyle("-fx-background-color: rgba(30, 30, 30, 0.95); -fx-background-radius: 20;" +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 30, 0, 0, 8);");
        root.setPrefWidth(350);

        // Avatar
        StackPane avatar = new StackPane();
        avatar.setPrefSize(100, 100);
        Circle circle = new Circle(50);
        circle.setFill(Color.web("#07C160"));
        circle.setStroke(Color.web("#95EC69"));
        circle.setStrokeWidth(3);

        // Animation pulse
        FadeTransition pulse = new FadeTransition(Duration.millis(1000), circle);
        pulse.setFromValue(1);
        pulse.setToValue(0.6);
        pulse.setAutoReverse(true);
        pulse.setCycleCount(FadeTransition.INDEFINITE);
        pulse.play();

        Label avLabel = new Label(callerName.substring(0, 1).toUpperCase());
        avLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 40));
        avLabel.setTextFill(Color.WHITE);
        avatar.getChildren().addAll(circle, avLabel);

        Label name = new Label(callerName);
        name.setFont(Font.font("Segoe UI", FontWeight.BOLD, 22));
        name.setTextFill(Color.WHITE);

        Label type = new Label(isVideo ? "📹 Appel vidéo entrant..." : "📞 Appel vocal entrant...");
        type.setFont(Font.font("Segoe UI", 14));
        type.setTextFill(Color.web("#CCC"));

        // Boutons
        HBox buttons = new HBox();
        buttons.setAlignment(Pos.CENTER);
        buttons.setSpacing(30);

        // Refuser (rouge)
        VBox rejectBox = new VBox();
        rejectBox.setAlignment(Pos.CENTER);
        rejectBox.setSpacing(8);

        Button rejectBtn = new Button("✕");
        rejectBtn.setFont(Font.font("Segoe UI", 24));
        rejectBtn.setStyle("-fx-background-color: #FF4444; -fx-text-fill: white;" +
                "-fx-background-radius: 50%; -fx-min-width: 60; -fx-min-height: 60;" +
                "-fx-cursor: hand;");
        rejectBtn.setOnAction(e -> {
            accepted = false;
            dialog.close();
        });

        Label rejectLbl = new Label("Refuser");
        rejectLbl.setTextFill(Color.web("#CCC"));
        rejectLbl.setFont(Font.font("Segoe UI", 12));
        rejectBox.getChildren().addAll(rejectBtn, rejectLbl);

        // Accepter (vert)
        VBox acceptBox = new VBox();
        acceptBox.setAlignment(Pos.CENTER);
        acceptBox.setSpacing(8);

        Button acceptBtn = new Button("📞");
        acceptBtn.setFont(Font.font("Segoe UI Emoji", 24));
        acceptBtn.setStyle("-fx-background-color: #07C160; -fx-text-fill: white;" +
                "-fx-background-radius: 50%; -fx-min-width: 60; -fx-min-height: 60;" +
                "-fx-cursor: hand;");
        acceptBtn.setOnAction(e -> {
            accepted = true;
            dialog.close();
        });

        Label acceptLbl = new Label("Accepter");
        acceptLbl.setTextFill(Color.web("#CCC"));
        acceptLbl.setFont(Font.font("Segoe UI", 12));
        acceptBox.getChildren().addAll(acceptBtn, acceptLbl);

        buttons.getChildren().addAll(rejectBox, acceptBox);

        root.getChildren().addAll(avatar, name, type, buttons);

        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        dialog.setScene(scene);

        // Auto-close après 30s
        new Thread(() -> {
            try {
                Thread.sleep(30000);
                Platform.runLater(() -> {
                    if (dialog.isShowing()) {
                        dialog.close();
                    }
                });
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();

        dialog.showAndWait();
        return accepted;
    }
}