package com.wechat.client.dialog;

import com.wechat.client.NetworkClient;
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

/**
 * Dialog d'inscription WeChat.
 */
public class RegisterDialog {

    private static final String COLOR_ACCENT = "#07C160";
    private Stage dialog;
    private TextField nameField, emailField;
    private PasswordField passwordField, confirmField;
    private Label errorLabel;
    private boolean success = false;

    public boolean showAndWait(Stage owner) {
        dialog = new Stage();
        dialog.initOwner(owner);
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initStyle(StageStyle.UNDECORATED);

        VBox root = new VBox();
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(40));
        root.setSpacing(16);
        root.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 12;" +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 20, 0, 0, 4);");
        root.setPrefWidth(380);

        Label title = new Label("Créer un compte");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 22));

        nameField = createField("Nom d'utilisateur");
        emailField = createField("Email");
        passwordField = new PasswordField();
        passwordField.setPromptText("Mot de passe (min 6 caractères)");
        passwordField.setStyle(getFieldStyle());
        passwordField.setFont(Font.font("Segoe UI", 14));
        confirmField = new PasswordField();
        confirmField.setPromptText("Confirmer le mot de passe");
        confirmField.setStyle(getFieldStyle());
        confirmField.setFont(Font.font("Segoe UI", 14));

        errorLabel = new Label();
        errorLabel.setTextFill(Color.web("#FF4444"));
        errorLabel.setFont(Font.font("Segoe UI", 12));
        errorLabel.setVisible(false);

        Button registerBtn = new Button("S'inscrire");
        registerBtn.setStyle("-fx-background-color: " + COLOR_ACCENT + "; -fx-text-fill: white;" +
                "-fx-background-radius: 6; -fx-padding: 12 0; -fx-cursor: hand;");
        registerBtn.setMaxWidth(Double.MAX_VALUE);
        registerBtn.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        registerBtn.setOnAction(e -> attemptRegister());

        Hyperlink loginLink = new Hyperlink("Déjà un compte ? Se connecter");
        loginLink.setTextFill(Color.web(COLOR_ACCENT));
        loginLink.setOnAction(e -> {
            dialog.close();
            new LoginDialog().showAndWait(owner);
        });

        Button closeBtn = new Button("✕");
        closeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #999; -fx-cursor: hand;");
        closeBtn.setOnAction(e -> dialog.close());

        HBox topBar = new HBox(closeBtn);
        topBar.setAlignment(Pos.TOP_RIGHT);

        root.getChildren().addAll(topBar, title, nameField, emailField,
                passwordField, confirmField, errorLabel,
                registerBtn, loginLink);

        dialog.setScene(new Scene(root));
        dialog.showAndWait();
        return success;
    }

    private void attemptRegister() {
        String name = nameField.getText().trim();
        String email = emailField.getText().trim();
        String password = passwordField.getText();
        String confirm = confirmField.getText();

        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            showError("Tous les champs sont requis");
            return;
        }
        if (password.length() < 6) {
            showError("Le mot de passe doit contenir au moins 6 caractères");
            return;
        }
        if (!password.equals(confirm)) {
            showError("Les mots de passe ne correspondent pas");
            return;
        }

        NetworkClient client = NetworkClient.getInstance();
        if (!client.isConnected()) {
            client.connect();
        }

        boolean ok = client.register(name, email, password);
        if (ok) {
            success = true;
            dialog.close();
            new Alert(Alert.AlertType.INFORMATION, "Inscription réussie ! Connectez-vous.").showAndWait();
        } else {
            showError("Échec de l'inscription. L'email ou le nom d'utilisateur existe peut-être déjà.");
        }
    }

    private void showError(String msg) {
        errorLabel.setText(msg);
        errorLabel.setVisible(true);
    }

    private TextField createField(String prompt) {
        TextField f = new TextField();
        f.setPromptText(prompt);
        f.setStyle(getFieldStyle());
        f.setFont(Font.font("Segoe UI", 14));
        return f;
    }

    private String getFieldStyle() {
        return "-fx-background-color: #F5F5F5; -fx-background-radius: 6;" +
                "-fx-border-color: #E5E5E5; -fx-border-radius: 6;" +
                "-fx-border-width: 1; -fx-padding: 10 12;";
    }
}