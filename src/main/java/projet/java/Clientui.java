package projet.java;

import javax.swing.*;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Main chat window.
 *  - Sets up the frame with TopBar / ChatPanel / BottomBar
 *  - Wires call buttons → NotificationAppel → AudioCallUI / VideoCallUI
 */
public class Clientui {

    private final String    username    = "Lina";
    private final String    currentUser = "Alice Dupont";
    private ChatPanel       chatPanel;
    private BottomBar       bottomBar;

    public Clientui() {
        JFrame frame = new JFrame("Chat");
        frame.setSize(720, 620);
        frame.setMinimumSize(new Dimension(480, 400));
        frame.setLayout(new BorderLayout());
        frame.getContentPane().setBackground(UIConstants.BG_DARK);

        // Top bar
        TopBar topBar = new TopBar(currentUser);
        frame.add(topBar, BorderLayout.NORTH);

        // Chat area
        chatPanel = new ChatPanel();
        frame.add(chatPanel, BorderLayout.CENTER);

        // Input bar
        bottomBar = new BottomBar(this::sendMessage);
        frame.add(bottomBar, BorderLayout.SOUTH);

        // Welcome
        chatPanel.addSystemMessage("Bienvenue " + username + " 👋");

        // ── Call button wiring ───────────────────────────────

        topBar.btnVideo.addActionListener(e -> {
            NotificationAppel popup = new NotificationAppel(frame, currentUser, "Vidéo");
            popup.setVisible(true);
            if (popup.isAccepte()) {
                new VideoCallUI(username, currentUser);
            }
        });

        topBar.btnAudio.addActionListener(e -> {
            new AudioCallUI(currentUser);
        });

        // ── Frame settings ───────────────────────────────────
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        // Test messages so the UI looks populated on launch
        String now = new SimpleDateFormat("HH:mm").format(new Date());
        chatPanel.addMessage(currentUser, "Bonjour ! Comment tu vas ?", now, false);
        chatPanel.addMessage(username, "Très bien merci ! Et toi ?", now, true);
        chatPanel.addMessage(currentUser, "Super ! On se retrouve à 18h ?", now, false);
    }

    private void sendMessage() {
        String msg = bottomBar.getMessage().trim();
        if (msg.isEmpty()) return;
        String time = new SimpleDateFormat("HH:mm").format(new Date());
        chatPanel.addMessage(username, msg, time, true);
        bottomBar.clear();
    }

    public static void main(String[] args) {
        // Set system look defaults (still override with custom colours)
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception ignored) {}

        SwingUtilities.invokeLater(Clientui::new);
    }
}