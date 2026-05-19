package org.example.tpchatjavafx.client.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.example.tpchatjavafx.client.ChatClientApp;
import org.example.tpchatjavafx.client.NetworkClient;

/**
 * Controleur de l'ecran de connexion / inscription.
 */
public class LoginController {

    @FXML private TextField loginHostField;
    @FXML private TextField loginPortField;
    @FXML private TextField loginUsernameField;
    @FXML private PasswordField loginPasswordField;
    @FXML private TextField loginPasswordVisibleField;
    @FXML private CheckBox loginShowPasswordCheck;
    @FXML private Label loginErrorLabel;
    @FXML private Button loginBtn;

    @FXML private TextField regHostField;
    @FXML private TextField regPortField;
    @FXML private TextField regUsernameField;
    @FXML private PasswordField regPasswordField;
    @FXML private TextField regPasswordVisibleField;
    @FXML private CheckBox regShowPasswordCheck;
    @FXML private TextField regEmailField;
    @FXML private Label regErrorLabel;
    @FXML private Button registerBtn;

    @FXML private TabPane tabPane;

    private NetworkClient client;

    @FXML
    private void initialize() {
        setupPasswordToggle(loginPasswordField, loginPasswordVisibleField, loginShowPasswordCheck);
        setupPasswordToggle(regPasswordField, regPasswordVisibleField, regShowPasswordCheck);

        if (loginHostField != null) loginHostField.setText("localhost");
        if (loginPortField != null) loginPortField.setText("5555");
        if (regHostField != null) regHostField.setText("localhost");
        if (regPortField != null) regPortField.setText("5555");

        if (loginErrorLabel != null) loginErrorLabel.setVisible(false);
        if (regErrorLabel != null) regErrorLabel.setVisible(false);
    }

    private void setupPasswordToggle(PasswordField pf, TextField tf, CheckBox cb) {
        if (pf == null || tf == null || cb == null) return;
        tf.managedProperty().bind(cb.selectedProperty());
        tf.visibleProperty().bind(cb.selectedProperty());
        pf.managedProperty().bind(cb.selectedProperty().not());
        pf.visibleProperty().bind(cb.selectedProperty().not());
        tf.textProperty().bindBidirectional(pf.textProperty());
    }

    @FXML
    private void onLogin() {
        String host = loginHostField.getText().trim();
        String portStr = loginPortField.getText().trim();
        String username = loginUsernameField.getText().trim();
        String password = loginPasswordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showError(loginErrorLabel, "Remplissez le nom d'utilisateur et le mot de passe.");
            return;
        }

        int port = parsePort(portStr, loginErrorLabel);
        if (port == -1) return;

        setButtonsDisabled(true);
        connectAndSend(host, port, loginErrorLabel, () -> client.login(username, password));
    }

    @FXML
    private void onRegister() {
        String host = regHostField.getText().trim();
        String portStr = regPortField.getText().trim();
        String username = regUsernameField.getText().trim();
        String password = regPasswordField.getText();
        String email = regEmailField.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            showError(regErrorLabel, "Nom d'utilisateur et mot de passe requis.");
            return;
        }
        if (email.isEmpty()) {
            showError(regErrorLabel, "Adresse email requise.");
            return;
        }
        if (!isValidEmail(email)) {
            showError(regErrorLabel, "Adresse email invalide.");
            return;
        }
        if (password.length() < 4) {
            showError(regErrorLabel, "Mot de passe trop court (min 4 caracteres).");
            return;
        }

        int port = parsePort(portStr, regErrorLabel);
        if (port == -1) return;

        setButtonsDisabled(true);
        connectAndSend(host, port, regErrorLabel, () -> client.register(username, password, email));
    }

    private int parsePort(String portStr, Label errorLabel) {
        try {
            return Integer.parseInt(portStr);
        } catch (NumberFormatException e) {
            showError(errorLabel, "Port invalide.");
            return -1;
        }
    }

    private void connectAndSend(String host, int port, Label errorLabel, Runnable sendAction) {
        client = new NetworkClient(host, port);

        client.setOnAuthSuccess(msg -> Platform.runLater(() -> {
            try {
                String uname = msg.getContent();
                int uId = Integer.parseInt(msg.getTo());
                ChatClientApp.showMainChat(client, uname, uId);
            } catch (Exception e) {
                showError(errorLabel, "Erreur d'ouverture : " + e.getMessage());
            }
        }));

        client.setOnAuthFail(reason -> Platform.runLater(() -> {
            showError(errorLabel, reason);
            setButtonsDisabled(false);
        }));

        client.setOnConnectionLost(() -> Platform.runLater(() -> {
            showError(errorLabel, "Connexion perdue. Serveur demarre ?");
            setButtonsDisabled(false);
        }));

        new Thread(() -> {
            try {
                client.connect();
                Platform.runLater(sendAction);
            } catch (Exception e) {
                Platform.runLater(() -> {
                    showError(errorLabel, "Impossible de se connecter : " + e.getMessage());
                    setButtonsDisabled(false);
                });
            }
        }).start();
    }

    private void showError(Label label, String msg) {
        if (label == null) return;
        label.setText(msg);
        label.setVisible(true);
        label.setManaged(true);
    }

    private boolean isValidEmail(String email) {
        return email != null && email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    }

    private void setButtonsDisabled(boolean disabled) {
        if (loginBtn != null) loginBtn.setDisable(disabled);
        if (registerBtn != null) registerBtn.setDisable(disabled);
    }
}
