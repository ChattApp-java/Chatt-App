package org.example.tpchatjavafx.client.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.example.tpchatjavafx.client.NetworkClient;

import java.util.List;
import java.util.stream.Collectors;

public class CreateGroupDialogController {

    @FXML private TextField groupNameField;
    @FXML private TextArea  groupDescField;
    @FXML private ListView<CheckBox> contactsListView;

    private NetworkClient networkClient;
    private List<String>  allContacts;

    public void init(NetworkClient networkClient, List<String> contacts) {
        this.networkClient = networkClient;
        this.allContacts   = contacts;

        ObservableList<CheckBox> items = FXCollections.observableArrayList();
        for (String c : contacts) items.add(new CheckBox(c));
        contactsListView.setItems(items);
    }

    public String getGroupName() {
        return groupNameField != null ? groupNameField.getText().trim() : "";
    }

    public String getGroupDescription() {
        return groupDescField != null ? groupDescField.getText().trim() : "";
    }

    public List<String> getSelectedMembers() {
        if (contactsListView == null) return List.of();
        return contactsListView.getItems().stream()
                .filter(CheckBox::isSelected)
                .map(CheckBox::getText)
                .collect(Collectors.toList());
    }
}
