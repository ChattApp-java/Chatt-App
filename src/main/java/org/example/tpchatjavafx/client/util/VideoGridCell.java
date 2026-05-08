package org.example.tpchatjavafx.client.util;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

public class VideoGridCell {

    private final VBox root = new VBox(6);
    private final ImageView imageView;
    private final Label nameLabel;
    private final Label micLabel;

    public VideoGridCell(String displayName) {
        imageView = new ImageView();
        imageView.setFitWidth(320);
        imageView.setFitHeight(240);
        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);
        imageView.setStyle("-fx-background-color: #111;");

        Rectangle placeholder = new Rectangle(320, 240, Color.web("#222"));
        placeholder.setArcWidth(12);
        placeholder.setArcHeight(12);
        imageView.setImage(null);

        nameLabel = new Label(displayName);
        nameLabel.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 14px; -fx-font-weight: bold;");

        micLabel = new Label("🎤");
        micLabel.setStyle("-fx-text-fill: #00ff00; -fx-font-size: 16px;");

        VBox infoBox = new VBox(5, nameLabel, micLabel);
        infoBox.setAlignment(Pos.CENTER);

        root.setAlignment(Pos.CENTER);
        root.getChildren().addAll(imageView, infoBox);
        root.setStyle("-fx-background-color: rgba(255,255,255,0.05); -fx-padding: 10; -fx-border-radius: 15; -fx-background-radius: 15; -fx-border-color: rgba(255,255,255,0.1);");
    }

    public VBox getRoot() {
        return root;
    }

    public void setImage(Image image) {
        imageView.setImage(image);
    }

    public void setMuted(boolean muted) {
        micLabel.setText(muted ? "🔇" : "🎤");
        micLabel.setStyle(muted ? "-fx-text-fill: #ff4444;" : "-fx-text-fill: #00ff00;");
    }
}

