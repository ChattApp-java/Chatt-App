package com.wechat.client.component;

import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

public class ContactCard extends HBox {

    public ContactCard(String name, String status) {
        // TODO: Carte de contact stylisée
        Label nameLabel = new Label(name);
        Label statusLabel = new Label(status);
        getChildren().addAll(nameLabel, statusLabel);
    }
}