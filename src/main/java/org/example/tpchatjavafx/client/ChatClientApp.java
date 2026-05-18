package org.example.tpchatjavafx.client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.example.tpchatjavafx.client.controller.LoginController;

import java.net.URL;

/**
 * Point d'entrée JavaFX — démarre sur l'écran de login.
 */
public class ChatClientApp extends Application {

    private static Stage primaryStage;

    @Override
    public void start(Stage stage) throws Exception {
        primaryStage = stage;
        primaryStage.setTitle("WeChat");
        primaryStage.setResizable(true);
        primaryStage.setMinWidth(800);
        primaryStage.setMinHeight(600);
        showLoginView();
    }

    // ── Login ─────────────────────────────────────────────────

    public static void showLoginView() throws Exception {
        URL fxml = ChatClientApp.class.getResource("/fxml/login.fxml");
        if (fxml == null) throw new IllegalStateException("login.fxml introuvable");

        FXMLLoader loader = new FXMLLoader(fxml);
        Scene scene = new Scene(loader.load(), 900, 600);

        URL css = ChatClientApp.class.getResource("/css/styles.css");
        if (css != null) scene.getStylesheets().add(css.toExternalForm());

        primaryStage.setScene(scene);
        primaryStage.setTitle("WeChat — Connexion");
        primaryStage.show();
    }

    // ── Main chat ─────────────────────────────────────────────

    public static void showMainChat(NetworkClient networkClient, String username, int userId) throws Exception {
        URL fxml = ChatClientApp.class.getResource("/fxml/main-chat-view.fxml");
        if (fxml == null) throw new IllegalStateException("main-chat-view.fxml introuvable");

        FXMLLoader loader = new FXMLLoader(fxml);
        Scene scene = new Scene(loader.load(), 1100, 700);

        URL css = ChatClientApp.class.getResource("/css/styles.css");
        if (css != null) scene.getStylesheets().add(css.toExternalForm());

        primaryStage.setScene(scene);
        primaryStage.setTitle("WeChat — " + username);

        org.example.tpchatjavafx.client.controller.MainChatController ctrl = loader.getController();
        ctrl.init(networkClient, username, userId);
        primaryStage.widthProperty().addListener((obs, oldValue, newValue) ->
                ctrl.onWindowResize(newValue.doubleValue(), primaryStage.getHeight()));
        primaryStage.heightProperty().addListener((obs, oldValue, newValue) ->
                ctrl.onWindowResize(primaryStage.getWidth(), newValue.doubleValue()));
        ctrl.onWindowResize(primaryStage.getWidth(), primaryStage.getHeight());
    }

    public static Stage getPrimaryStage() { return primaryStage; }

    public static void main(String[] args) { launch(args); }
}
