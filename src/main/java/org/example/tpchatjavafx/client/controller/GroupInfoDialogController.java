package org.example.tpchatjavafx.client.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.Arrays;
import java.util.List;

public class GroupInfoDialogController {

    @FXML private Label groupAvatarLabel;
    @FXML private Label groupNameLabel;
    @FXML private Label groupDescLabel;
    @FXML private Label groupStatsLabel;
    @FXML private ListView<String> membersListView;

    private final ObservableList<String> members = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        membersListView.setItems(members);
        membersListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(String user, boolean empty) {
                super.updateItem(user, empty);
                if (empty || user == null) { setGraphic(null); setText(null); return; }

                String username = user;
                String role = "";
                if (user.contains(":")) {
                    String[] parts = user.split(":");
                    username = parts[0];
                    if (parts.length > 1) role = parts[1];
                }

                String initial = username.substring(0, 1).toUpperCase();
                Label av = new Label(initial);
                av.getStyleClass().add("avatar-letter");
                StackPane avatar = new StackPane(av);
                avatar.getStyleClass().add("avatar-circle");
                avatar.setStyle("-fx-background-color:#0284c7; -fx-background-radius:50%; -fx-min-width:36px; -fx-min-height:36px; -fx-max-width:36px; -fx-max-height:36px;");

                Label name = new Label(username);
                name.getStyleClass().add("chat-contact-name");

                Label roleLabel = new Label("ADMIN".equalsIgnoreCase(role) ? "Administrateur" : "Membre");
                roleLabel.getStyleClass().add("chat-contact-status");
                if ("ADMIN".equalsIgnoreCase(role)) roleLabel.setStyle("-fx-text-fill: #0ea5e9; -fx-font-weight: bold;");

                VBox info = new VBox(2, name, roleLabel);
                HBox row = new HBox(12, avatar, info);
                row.setAlignment(Pos.CENTER_LEFT);

                setGraphic(row);
                setText(null);
            }
        });
    }

    public void init(String groupName, String description, String rawMembersList) {
        if (groupName != null && !groupName.isEmpty()) {
            this.groupNameLabel.setText(groupName);
            this.groupAvatarLabel.setText(groupName.substring(0, 1).toUpperCase());
        }
        this.groupDescLabel.setText(description != null && !description.isBlank() ? description : "Aucune description");
        this.groupStatsLabel.setText("");

        if (rawMembersList != null && !rawMembersList.isBlank()) {
            String[] parts = rawMembersList.split(",");
            members.setAll(Arrays.asList(parts));
        }
    }

    @FXML
    private void onClose() {
        if (groupNameLabel.getScene() != null && groupNameLabel.getScene().getWindow() != null) {
            ((Stage) groupNameLabel.getScene().getWindow()).close();
        }
    }
}
