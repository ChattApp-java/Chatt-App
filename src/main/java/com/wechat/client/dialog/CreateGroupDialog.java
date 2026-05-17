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

import java.util.ArrayList;
import java.util.List;

/**
 * Dialog de création de groupe.
 */
public class CreateGroupDialog {

    private static final String COLOR_ACCENT = "#07C160";
    private Stage dialog;
    private TextField nameField, descField;
    private VBox membersList;
    private List<CheckBox> memberCheckboxes = new ArrayList<>();
    private boolean confirmed = false;
    private String groupName;
    private List<Long> selectedMembers = new ArrayList<>();

    public boolean showAndWait(Stage owner, List<MockUser> availableUsers) {
        dialog = new Stage();
        dialog.initOwner(owner);
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initStyle(StageStyle.UNDECORATED);

        VBox root = new VBox();
        root.setPadding(new Insets(30));
        root.setSpacing(16);
        root.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 12;" +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 20, 0, 0, 4);");
        root.setPrefWidth(400);

        Label title = new Label("Nouveau groupe");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 20));

        nameField = new TextField();
        nameField.setPromptText("Nom du groupe");
        nameField.setStyle(getFieldStyle());
        nameField.setFont(Font.font("Segoe UI", 14));

        descField = new TextField();
        descField.setPromptText("Description (optionnel)");
        descField.setStyle(getFieldStyle());
        descField.setFont(Font.font("Segoe UI", 14));

        Label membersLabel = new Label("Ajouter des membres");
        membersLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));

        membersList = new VBox();
        membersList.setSpacing(8);

        for (MockUser user : availableUsers) {
            HBox row = new HBox();
            row.setAlignment(Pos.CENTER_LEFT);
            row.setSpacing(10);

            CheckBox cb = new CheckBox();
            cb.setUserData(user.id());
            memberCheckboxes.add(cb);

            Label name = new Label(user.name());
            name.setFont(Font.font("Segoe UI", 13));

            row.getChildren().addAll(cb, name);
            membersList.getChildren().add(row);
        }

        ScrollPane scroll = new ScrollPane(membersList);
        scroll.setFitToWidth(true);
        scroll.setPrefHeight(200);
        scroll.setStyle("-fx-background: transparent;");

        HBox buttons = new HBox();
        buttons.setSpacing(10);
        buttons.setAlignment(Pos.CENTER_RIGHT);

        Button cancelBtn = new Button("Annuler");
        cancelBtn.setStyle("-fx-background-color: #F5F5F5; -fx-background-radius: 6;" +
                "-fx-padding: 10 20; -fx-cursor: hand;");
        cancelBtn.setOnAction(e -> dialog.close());

        Button createBtn = new Button("Créer");
        createBtn.setStyle("-fx-background-color: " + COLOR_ACCENT + "; -fx-text-fill: white;" +
                "-fx-background-radius: 6; -fx-padding: 10 20; -fx-cursor: hand;");
        createBtn.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        createBtn.setOnAction(e -> confirm());

        buttons.getChildren().addAll(cancelBtn, createBtn);

        Button closeBtn = new Button("✕");
        closeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #999; -fx-cursor: hand;");
        closeBtn.setOnAction(e -> dialog.close());

        HBox topBar = new HBox(closeBtn);
        topBar.setAlignment(Pos.TOP_RIGHT);

        root.getChildren().addAll(topBar, title, nameField, descField,
                membersLabel, scroll, buttons);

        dialog.setScene(new Scene(root));
        dialog.showAndWait();
        return confirmed;
    }

    private void confirm() {
        groupName = nameField.getText().trim();
        if (groupName.isEmpty()) {
            return;
        }

        selectedMembers.clear();
        for (CheckBox cb : memberCheckboxes) {
            if (cb.isSelected()) {
                selectedMembers.add((Long) cb.getUserData());
            }
        }

        if (selectedMembers.isEmpty()) {
            return;
        }

        confirmed = true;
        dialog.close();
    }

    public String getGroupName() { return groupName; }
    public List<Long> getSelectedMembers() { return selectedMembers; }

    private String getFieldStyle() {
        return "-fx-background-color: #F5F5F5; -fx-background-radius: 6;" +
                "-fx-border-color: #E5E5E5; -fx-border-radius: 6;" +
                "-fx-border-width: 1; -fx-padding: 10 12;";
    }

    public record MockUser(Long id, String name) {}
}