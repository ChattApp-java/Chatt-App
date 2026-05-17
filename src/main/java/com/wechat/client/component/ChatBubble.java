package com.wechat.client.component;

import javafx.scene.layout.HBox;
import javafx.scene.text.Text;

public class ChatBubble extends HBox {

    public ChatBubble(String message, boolean isSentByMe) {
        // TODO: Style de bulle de chat
        Text text = new Text(message);
        getChildren().add(text);
    }
}