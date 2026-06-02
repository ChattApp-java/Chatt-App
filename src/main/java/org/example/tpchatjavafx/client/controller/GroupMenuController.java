package org.example.tpchatjavafx.client.controller;

import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import org.example.tpchatjavafx.client.NetworkClient;

public class GroupMenuController {

    @FXML private Label groupAvatarLabel;
    @FXML private Label groupNameLabel;
    @FXML private CheckBox muteCheckBox;
    @FXML private CheckBox favoriteCheckBox;
    @FXML private Label ephemeralStatusLabel;

    private NetworkClient networkClient;
    private int groupId;
    private String groupName;
    private MainChatController mainController;

    public void init(NetworkClient client, int groupId, String groupName, MainChatController mainController) {
        this.networkClient = client;
        this.groupId = groupId;
        this.groupName = groupName;
        this.mainController = mainController;

        if (groupName != null && !groupName.isEmpty()) {
            this.groupNameLabel.setText(groupName);
            this.groupAvatarLabel.setText(groupName.substring(0, 1).toUpperCase());
        }

    }

    @FXML
    private void onToggleMute() {
        boolean isMuted = !muteCheckBox.isSelected();
        muteCheckBox.setSelected(isMuted);

    }

    @FXML
    private void onToggleFavorite() {
        boolean isFav = !favoriteCheckBox.isSelected();
        favoriteCheckBox.setSelected(isFav);

    }

    @FXML
    private void onSetupEphemeral() {

    }

    @FXML
    private void onScheduleCall() {

    }

    @FXML
    private void onGenerateCallLink() {

    }

    @FXML
    private void onShowMembers() {
        onClose();
        mainController.showGroupMembers(groupId, groupName);
    }

    @FXML
    private void onLeaveGroup() {
        if (networkClient != null) {
            networkClient.leaveGroup(groupId);
        }
        onClose();
    }

    @FXML
    private void onClose() {
        if (groupNameLabel.getScene() != null && groupNameLabel.getScene().getWindow() != null) {
            ((Stage) groupNameLabel.getScene().getWindow()).close();
        }
    }
}
