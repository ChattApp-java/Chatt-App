package com.wechat.client.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.media.MediaView;

public class CallController {

    @FXML
    private Label callerNameLabel;

    @FXML
    private MediaView localVideo;

    @FXML
    private MediaView remoteVideo;

    @FXML
    private void acceptCall() {
        // TODO: Accepter appel
    }

    @FXML
    private void declineCall() {
        // TODO: Refuser appel
    }

    @FXML
    private void endCall() {
        // TODO: Terminer appel
    }
}