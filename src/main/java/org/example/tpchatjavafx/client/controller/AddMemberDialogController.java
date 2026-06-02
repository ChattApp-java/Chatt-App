package org.example.tpchatjavafx.client.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.util.List;
import java.util.stream.Collectors;

public class AddMemberDialogController {

    @FXML private ListView<CheckBox> availableContactsList;

    public void init(List<String> nonMembers) {
        ObservableList<CheckBox> items = FXCollections.observableArrayList();
        for (String c : nonMembers) items.add(new CheckBox(c));
        availableContactsList.setItems(items);
    }

    public List<String> getSelectedMembers() {
        if (availableContactsList == null) return List.of();
        return availableContactsList.getItems().stream()
                .filter(CheckBox::isSelected)
                .map(CheckBox::getText)
                .collect(Collectors.toList());
    }
}
