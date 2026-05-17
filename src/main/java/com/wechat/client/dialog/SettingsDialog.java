package com.wechat.client.dialog;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 * Dialog des paramètres avec TabPane.
 */
public class SettingsDialog {

    public void showAndWait(Stage owner) {
        Stage dialog = new Stage();
        dialog.initOwner(owner);
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initStyle(StageStyle.UNDECORATED);

        VBox root = new VBox();
        root.setPadding(new Insets(20));
        root.setSpacing(10);
        root.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 12;" +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 20, 0, 0, 4);");
        root.setPrefWidth(500);
        root.setPrefHeight(450);

        Label title = new Label("Paramètres");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));

        // TabPane
        TabPane tabPane = new TabPane();
        VBox.setVgrow(tabPane, Priority.ALWAYS);
        tabPane.setStyle("-fx-background-color: transparent;");

        // Onglet Général
        Tab generalTab = new Tab("Général");
        generalTab.setClosable(false);
        VBox generalContent = new VBox(12);
        generalContent.setPadding(new Insets(16));
        generalContent.getChildren().addAll(
                createToggle("Notifications", true),
                createToggle("Son des messages", true),
                createToggle("Vibration", false),
                createToggle("Démarrage automatique", false)
        );
        generalTab.setContent(generalContent);

        // Onglet Confidentialité
        Tab privacyTab = new Tab("Confidentialité");
        privacyTab.setClosable(false);
        VBox privacyContent = new VBox(12);
        privacyContent.setPadding(new Insets(16));
        privacyContent.getChildren().addAll(
                createToggle("Lecture confirmée", true),
                createToggle("Statut en ligne visible", true),
                createToggle("Photo de profil publique", true),
                createToggle("Bloquer les inconnus", false)
        );
        privacyTab.setContent(privacyContent);

        // Onglet Compte
        Tab accountTab = new Tab("Compte");
        accountTab.setClosable(false);
        VBox accountContent = new VBox(12);
        accountContent.setPadding(new Insets(16));

        Button changePasswordBtn = new Button("Changer le mot de passe");
        changePasswordBtn.setStyle("-fx-background-color: #F5F5F5; -fx-background-radius: 6;" +
                "-fx-padding: 10 20; -fx-cursor: hand;");

        Button deleteAccountBtn = new Button("Supprimer le compte");
        deleteAccountBtn.setStyle("-fx-background-color: #FF4444; -fx-text-fill: white;" +
                "-fx-background-radius: 6; -fx-padding: 10 20; -fx-cursor: hand;");

        accountContent.getChildren().addAll(
                new Label("Email : user@example.com"),
                new Label("Compte créé le : 2024-01-15"),
                new Separator(),
                changePasswordBtn,
                deleteAccountBtn
        );
        accountTab.setContent(accountContent);

        tabPane.getTabs().addAll(generalTab, privacyTab, accountTab);

        Button closeBtn = new Button("Fermer");
        closeBtn.setStyle("-fx-background-color: #F5F5F5; -fx-background-radius: 6;" +
                "-fx-padding: 10 30; -fx-cursor: hand;");
        closeBtn.setOnAction(e -> dialog.close());

        Button closeX = new Button("✕");
        closeX.setStyle("-fx-background-color: transparent; -fx-text-fill: #999; -fx-cursor: hand;");
        closeX.setOnAction(e -> dialog.close());

        HBox topBar = new HBox(closeX);
        topBar.setAlignment(Pos.TOP_RIGHT);

        HBox bottomBar = new HBox(closeBtn);
        bottomBar.setAlignment(Pos.CENTER_RIGHT);

        root.getChildren().addAll(topBar, title, tabPane, bottomBar);

        dialog.setScene(new Scene(root));
        dialog.showAndWait();
    }

    private HBox createToggle(String label, boolean initial) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        row.setSpacing(10);

        Label lbl = new Label(label);
        lbl.setFont(Font.font("Segoe UI", 13));
        HBox.setHgrow(lbl, Priority.ALWAYS);

        ToggleButton toggle = new ToggleButton(initial ? "ON" : "OFF");
        toggle.setSelected(initial);
        toggle.setStyle("-fx-background-color: " + (initial ? "#07C160" : "#CCC") + ";" +
                "-fx-text-fill: white; -fx-background-radius: 12;" +
                "-fx-padding: 4 12; -fx-cursor: hand;");
        toggle.setOnAction(e -> {
            boolean on = toggle.isSelected();
            toggle.setText(on ? "ON" : "OFF");
            toggle.setStyle("-fx-background-color: " + (on ? "#07C160" : "#CCC") + ";" +
                    "-fx-text-fill: white; -fx-background-radius: 12;" +
                    "-fx-padding: 4 12; -fx-cursor: hand;");
        });

        row.getChildren().addAll(lbl, toggle);
        return row;
    }
}