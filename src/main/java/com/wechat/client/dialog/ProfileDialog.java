package com.wechat.client.dialog;

import com.wechat.model.User;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 * Dialog de profil utilisateur.
 */
public class ProfileDialog {

    private static final String COLOR_ACCENT = "#07C160";

    public void showAndWait(Stage owner, User user) {
        Stage dialog = new Stage();
        dialog.initOwner(owner);
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initStyle(StageStyle.UNDECORATED);

        VBox root = new VBox();
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(40));
        root.setSpacing(20);
        root.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 12;" +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 20, 0, 0, 4);");
        root.setPrefWidth(360);

        // Avatar
        StackPane avatar = new StackPane();
        avatar.setPrefSize(80, 80);
        Circle circle = new Circle(40);
        circle.setFill(Color.web(COLOR_ACCENT));
        Label avLabel = new Label(user != null ? user.getUsername().substring(0, 1).toUpperCase() : "?");
        avLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 32));
        avLabel.setTextFill(Color.WHITE);
        avatar.getChildren().addAll(circle, avLabel);

        Label name = new Label(user != null ? user.getNickname() : "Utilisateur");
        name.setFont(Font.font("Segoe UI", FontWeight.BOLD, 20));

        Label username = new Label("@" + (user != null ? user.getUsername() : "unknown"));
        username.setFont(Font.font("Segoe UI", 13));
        username.setTextFill(Color.web("#999"));

        Label email = new Label(user != null ? user.getEmail() : "");
        email.setFont(Font.font("Segoe UI", 13));
        email.setTextFill(Color.web("#666"));

        Label status = new Label("🟢 En ligne");
        status.setFont(Font.font("Segoe UI", 12));
        status.setTextFill(Color.web(COLOR_ACCENT));

        // Bouton déconnexion
        Button logoutBtn = new Button("Se déconnecter");
        logoutBtn.setStyle("-fx-background-color: #FF4444; -fx-text-fill: white;" +
                "-fx-background-radius: 6; -fx-padding: 10 30; -fx-cursor: hand;");
        logoutBtn.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        logoutBtn.setOnAction(e -> {
            dialog.close();
            // TODO: Déconnexion
        });

        Button closeBtn = new Button("✕");
        closeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #999; -fx-cursor: hand;");
        closeBtn.setOnAction(e -> dialog.close());

        HBox topBar = new HBox(closeBtn);
        topBar.setAlignment(Pos.TOP_RIGHT);

        root.getChildren().addAll(topBar, avatar, name, username, email, status, logoutBtn);

        dialog.setScene(new Scene(root));
        dialog.showAndWait();
    }
}