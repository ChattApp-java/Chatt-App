package com.wechat.client.dialog;

import com.wechat.client.NetworkClient;
import com.wechat.model.User;
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
 * Dialog de connexion WeChat.
 */
public class LoginDialog {

    private static final String COLOR_ACCENT = "#07C160";
    private static final String COLOR_BG = "#FFFFFF";
    private static final String COLOR_TEXT = "#000000";
    private static final String COLOR_SECONDARY = "#999999";
    private static final String COLOR_BORDER = "#E5E5E5";

    private Stage dialog;
    private TextField emailField;
    private PasswordField passwordField;
    private Label errorLabel;
    private User authenticatedUser;

    public User showAndWait(Stage owner) {
        dialog = new Stage();
        dialog.initOwner(owner);
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initStyle(StageStyle.UNDECORATED);
        dialog.setTitle("Connexion WeChat");

        VBox root = new VBox();
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(40));
        root.setSpacing(20);
        root.setStyle("-fx-background-color: " + COLOR_BG + ";" +
                "-fx-background-radius: 12;" +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 20, 0, 0, 4);");
        root.setPrefWidth(380);

        // Logo
        Label logo = new Label("💬");
        logo.setFont(Font.font("Segoe UI Emoji", 48));

        Label title = new Label("Connexion WeChat");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 22));
        title.setTextFill(Color.web(COLOR_TEXT));

        // Email
        emailField = createField("Email ou nom d'utilisateur");

        // Password
        passwordField = new PasswordField();
        passwordField.setPromptText("Mot de passe");
        passwordField.setStyle(getFieldStyle());
        passwordField.setFont(Font.font("Segoe UI", 14));

        // Error
        errorLabel = new Label();
        errorLabel.setFont(Font.font("Segoe UI", 12));
        errorLabel.setTextFill(Color.web("#FF4444"));
        errorLabel.setVisible(false);

        // Bouton connexion
        Button loginBtn = new Button("Se connecter");
        loginBtn.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        loginBtn.setStyle("-fx-background-color: " + COLOR_ACCENT + ";" +
                "-fx-text-fill: white; -fx-background-radius: 6;" +
                "-fx-padding: 12 0; -fx-cursor: hand;");
        loginBtn.setMaxWidth(Double.MAX_VALUE);
        loginBtn.setOnAction(e -> attemptLogin());

        // Lien inscription
        HBox linkBox = new HBox();
        linkBox.setAlignment(Pos.CENTER);
        linkBox.setSpacing(4);

        Label noAccount = new Label("Pas de compte ?");
        noAccount.setFont(Font.font("Segoe UI", 12));
        noAccount.setTextFill(Color.web(COLOR_SECONDARY));

        Hyperlink registerLink = new Hyperlink("S'inscrire");
        registerLink.setFont(Font.font("Segoe UI", 12));
        registerLink.setTextFill(Color.web(COLOR_ACCENT));
        registerLink.setOnAction(e -> {
            dialog.close();
            new RegisterDialog().showAndWait(owner);
        });

        linkBox.getChildren().addAll(noAccount, registerLink);

        // Fermer
        Button closeBtn = new Button("✕");
        closeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #999; -fx-cursor: hand;");
        closeBtn.setOnAction(e -> dialog.close());

        HBox topBar = new HBox();
        topBar.setAlignment(Pos.TOP_RIGHT);
        topBar.getChildren().add(closeBtn);

        root.getChildren().addAll(topBar, logo, title, emailField, passwordField,
                errorLabel, loginBtn, linkBox);

        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        dialog.setScene(scene);

        // Connexion avec Enter
        passwordField.setOnAction(e -> attemptLogin());

        dialog.showAndWait();
        return authenticatedUser;
    }

    private void attemptLogin() {
        String email = emailField.getText().trim();
        String password = passwordField.getText();

        if (email.isEmpty() || password.isEmpty()) {
            showError("Veuillez remplir tous les champs");
            return;
        }

        NetworkClient client = NetworkClient.getInstance();
        if (!client.isConnected()) {
            client.connect();
        }

        User user = client.authenticate(email, password);
        if (user != null) {
            authenticatedUser = user;
            dialog.close();
        } else {
            showError("Nom d'utilisateur ou mot de passe incorrect");
        }
    }

    private void showError(String msg) {
        errorLabel.setText(msg);
        errorLabel.setVisible(true);
    }

    private TextField createField(String prompt) {
        TextField field = new TextField();
        field.setPromptText(prompt);
        field.setStyle(getFieldStyle());
        field.setFont(Font.font("Segoe UI", 14));
        return field;
    }

    private String getFieldStyle() {
        return "-fx-background-color: #F5F5F5; -fx-background-radius: 6;" +
                "-fx-border-color: " + COLOR_BORDER + "; -fx-border-radius: 6;" +
                "-fx-border-width: 1; -fx-padding: 10 12;";
    }
}