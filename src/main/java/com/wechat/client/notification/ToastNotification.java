package com.wechat.client.notification;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

/**
 * Notification toast en coin haut-droite.
 * Auto-disparition après 3 secondes.
 */
public class ToastNotification {

    private static Stage currentToast;

    public static void show(String title, String message) {
        Platform.runLater(() -> {
            // Fermer l'ancien toast s'il existe
            if (currentToast != null && currentToast.isShowing()) {
                currentToast.close();
            }

            Stage toast = new Stage();
            currentToast = toast;
            toast.initStyle(StageStyle.TRANSPARENT);
            toast.setAlwaysOnTop(true);

            HBox root = new HBox();
            root.setAlignment(Pos.CENTER_LEFT);
            root.setPadding(new Insets(12, 16, 12, 16));
            root.setSpacing(10);
            root.setStyle("-fx-background-color: #2E2E2E; -fx-background-radius: 8;" +
                    "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 10, 0, 0, 4);");

            Label icon = new Label("🔔");
            icon.setFont(Font.font("Segoe UI Emoji", 18));

            VBox textBox = new VBox();
            textBox.setSpacing(2);

            Label titleLabel = new Label(title);
            titleLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
            titleLabel.setTextFill(Color.WHITE);

            Label msgLabel = new Label(message);
            msgLabel.setFont(Font.font("Segoe UI", 12));
            msgLabel.setTextFill(Color.web("#CCC"));

            textBox.getChildren().addAll(titleLabel, msgLabel);
            root.getChildren().addAll(icon, textBox);

            Scene scene = new Scene(root);
            scene.setFill(Color.TRANSPARENT);
            toast.setScene(scene);

            // Position : coin haut-droite
            var screenBounds = Screen.getPrimary().getVisualBounds();
            toast.setX(screenBounds.getMaxX() - 320);
            toast.setY(screenBounds.getMinY() + 20);

            toast.show();

            // Fade in
            root.setOpacity(0);
            FadeTransition fadeIn = new FadeTransition(Duration.millis(200), root);
            fadeIn.setToValue(1);
            fadeIn.play();

            // Auto-disparition après 3s
            PauseTransition pause = new PauseTransition(Duration.seconds(3));
            pause.setOnFinished(e -> {
                FadeTransition fadeOut = new FadeTransition(Duration.millis(300), root);
                fadeOut.setToValue(0);
                fadeOut.setOnFinished(ev -> toast.close());
                fadeOut.play();
            });
            pause.play();
        });
    }
}