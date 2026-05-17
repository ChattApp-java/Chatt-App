package com.wechat.client.component;

import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.util.Duration;

/**
 * Barre de déconnexion rouge en haut de l'interface.
 */
public class ConnectionBar extends HBox {

    private final Label label;

    public ConnectionBar() {
        setAlignment(Pos.CENTER);
        setPrefHeight(32);
        setStyle("-fx-background-color: #FF4444;");
        setVisible(false);

        label = new Label("⚠️ Connexion perdue - Tentative de reconnexion...");
        label.setFont(Font.font("Segoe UI", 12));
        label.setTextFill(Color.WHITE);

        getChildren().add(label);
    }

    public void showDisconnected() {
        Platform.runLater(() -> {
            setVisible(true);
            FadeTransition fade = new FadeTransition(Duration.millis(200), this);
            fade.setToValue(1);
            fade.play();
        });
    }

    public void showConnected() {
        Platform.runLater(() -> {
            FadeTransition fade = new FadeTransition(Duration.millis(300), this);
            fade.setToValue(0);
            fade.setOnFinished(e -> setVisible(false));
            fade.play();
        });
    }
}