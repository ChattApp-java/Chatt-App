import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Main chat window (JavaFX).
 *  - Sets up the Stage with TopBar / ChatPanel / BottomBar
 *  - Wires call buttons → NotificationAppel → AudioCallUI / VideoCallUI
 */
public class Clientui extends Application {

    private final String username    = "Lina";
    private final String currentUser = "Utilis";

    private ChatPanel chatPanel;
    private BottomBar bottomBar;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Chat");
        primaryStage.setMinWidth(480);
        primaryStage.setMinHeight(400);

        // ── Root layout ──────────────────────────────────────
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: " + UIConstants.toHex(UIConstants.BG_DARK) + ";");

        // ── Top bar ──────────────────────────────────────────
        TopBar topBar = new TopBar(currentUser);
        root.setTop(topBar);

        // ── Chat area ────────────────────────────────────────
        chatPanel = new ChatPanel();
        root.setCenter(chatPanel);

        // ── Input bar ────────────────────────────────────────
        bottomBar = new BottomBar(this::sendMessage);
        root.setBottom(bottomBar);

        // ── Welcome message ──────────────────────────────────
        chatPanel.addSystemMessage("Bienvenue " + username + " 👋");

        // ── Call button wiring ───────────────────────────────
        topBar.btnVideo.setOnAction(e -> {
            NotificationAppel popup = new NotificationAppel(primaryStage, currentUser, "Vidéo");
            popup.showAndWait();
            if (popup.isAccepte()) {
                new VideoCallUI(username, currentUser);
            }
        });

        topBar.btnAudio.setOnAction(e -> new AudioCallUI(currentUser));

        // ── Scene ────────────────────────────────────────────
        Scene scene = new Scene(root, 720, 620);
        primaryStage.setScene(scene);
        primaryStage.show();

        // ── Test messages ────────────────────────────────────
        String now = new SimpleDateFormat("HH:mm").format(new Date());
        chatPanel.addMessage(currentUser, "Bonjour ! Comment tu vas ?",   now, false);
        chatPanel.addMessage(username,    "Très bien merci ! Et toi ?",    now, true);
        chatPanel.addMessage(currentUser, "Super ! On se retrouve à 18h ?", now, false);
    }

    // ── Send message ─────────────────────────────────────────
    private void sendMessage() {
        String msg = bottomBar.getMessage().trim();
        if (msg.isEmpty()) return;
        String time = new SimpleDateFormat("HH:mm").format(new Date());
        chatPanel.addMessage(username, msg, time, true);
        bottomBar.clear();
    }

    // ── Entry point ──────────────────────────────────────────
    public static void main(String[] args) {
        launch(args);
    }

    // ── Helper ───────────────────────────────────────────────
    private static String toHex(java.awt.Color c) {
        return String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
    }
}