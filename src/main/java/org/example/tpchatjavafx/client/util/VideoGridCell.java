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
        nameLabel.getStyleClass().add("video-label");

        micLabel = new Label("🎤");
        micLabel.getStyleClass().add("mic-indicator");

        root.getStyleClass().add("video-grid-cell");
        root.setAlignment(Pos.CENTER);
        root.getChildren().addAll(imageView, nameLabel, micLabel);
    }

    public VBox getRoot() {
        return root;
    }

    public void setImage(Image image) {
        imageView.setImage(image);
    }

    public void resize(double width, double height) {
        double safeWidth = Math.max(220, width);
        double safeHeight = Math.max(160, height);
        imageView.setFitWidth(safeWidth);
        imageView.setFitHeight(safeHeight - 48);
        root.setMinWidth(safeWidth);
        root.setMaxWidth(safeWidth);
    }

    public void setMuted(boolean muted) {
        micLabel.setText(muted ? "🔇" : "🎤");
        micLabel.setStyle(muted ? "-fx-text-fill: #ff4444;" : "-fx-text-fill: #00ff00;");
    }
}

