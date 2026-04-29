package org.example.tpchatjavafx.client.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import org.example.tpchatjavafx.client.ChatClientApp;
import org.example.tpchatjavafx.client.NetworkClient;

/**
 * Legacy Profile Controller — synchronized with unified architecture.
 */
public class ProfileController {

    @FXML private TextField usernameField;
    @FXML private TextField userHostField;
    @FXML private TextField userPortField;
    @FXML private TextField serverHostField;
    @FXML private TextField serverPortField;
    @FXML private Label errorLabel;

    @FXML
    private void initialize() {
        // Default values (local dev)
        if (userHostField != null) userHostField.setText("127.0.0.1");
        if (userPortField != null) userPortField.setText("6000");
        if (serverHostField != null) serverHostField.setText("127.0.0.1");
        if (serverPortField != null) serverPortField.setText("5555");
    }

    @FXML
    private void onConnect(ActionEvent event) {
        String username = usernameField.getText().trim();
        String serverHost = serverHostField.getText().trim();
        String serverPortStr = serverPortField.getText().trim();

        if (username.isEmpty() || serverHost.isEmpty() || serverPortStr.isEmpty()) {
            errorLabel.setText("Username and Server info are required.");
            return;
        }

        int serverPort;
        try {
            serverPort = Integer.parseInt(serverPortStr);
        } catch (NumberFormatException e) {
            errorLabel.setText("Server port must be a valid number.");
            return;
        }

        try {
            NetworkClient networkClient = new NetworkClient(serverHost, serverPort);
            networkClient.connect();
            ChatClientApp.showMainChat(networkClient, username, -1);
        } catch (Exception e) {
            e.printStackTrace();
            errorLabel.setText("Cannot connect to server: " + e.getMessage());
        }
    }
}
