package com.wechat.client.controller;

import com.wechat.client.NetworkClient;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.util.Duration;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Contrôleur de la zone de chat.
 * Gère l'affichage des messages, l'envoi, les indicateurs de lecture.
 */
public class ChatController {

    // Palette
    private static final String COLOR_ACCENT = "#07C160";
    private static final String COLOR_BG_CHAT = "#F5F5F5";
    private static final String COLOR_BG_WHITE = "#FFFFFF";
    private static final String COLOR_TEXT_PRIMARY = "#000000";
    private static final String COLOR_TEXT_SECOND = "#999999";
    private static final String COLOR_BORDER = "#E5E5E5";
    private static final String COLOR_SENT_BG = "#95EC69";  // Vert WeChat envoyé
    private static final String COLOR_RECEIVED_BG = "#FFFFFF";
    private static final String COLOR_READ_CHECK = "#07C160";

    private VBox chatContainer;
    private ScrollPane scrollPane;
    private TextArea inputArea;
    private Label typingLabel;
    private Label headerName;
    private Label headerStatus;

    private Long currentConversationId;
    private Long currentContactId;
    private String currentContactName;
    private boolean isGroup;

    private final List<ChatMessage> messages = new ArrayList<>();
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

    public VBox createChatView() {
        VBox chatView = new VBox();
        chatView.setStyle("-fx-background-color: " + COLOR_BG_CHAT + ";");
        VBox.setVgrow(chatView, Priority.ALWAYS);

        // ─── HEADER ───
        HBox header = createHeader();

        // ─── ZONE MESSAGES ───
        VBox messagesArea = createMessagesArea();

        // ─── TYPING INDICATOR ───
        typingLabel = new Label();
        typingLabel.setFont(Font.font("Segoe UI", 12));
        typingLabel.setTextFill(Color.web(COLOR_TEXT_SECOND));
        typingLabel.setPadding(new Insets(4, 16, 4, 16));
        typingLabel.setVisible(false);

        // ─── BARRE D'INPUT ───
        HBox inputBar = createInputBar();

        chatView.getChildren().addAll(header, messagesArea, typingLabel, inputBar);
        VBox.setVgrow(messagesArea, Priority.ALWAYS);

        return chatView;
    }

    private HBox createHeader() {
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(12, 16, 12, 16));
        header.setSpacing(12);
        header.setStyle("-fx-background-color: " + COLOR_BG_WHITE + ";" +
                "-fx-border-width: 0 0 1 0; -fx-border-color: " + COLOR_BORDER + ";");

        // Avatar
        StackPane avatar = new StackPane();
        avatar.setPrefSize(40, 40);
        Circle circle = new Circle(20);
        circle.setFill(Color.web(COLOR_ACCENT));
        Label avatarLabel = new Label("👤");
        avatarLabel.setFont(Font.font("Segoe UI Emoji", 18));
        avatar.getChildren().addAll(circle, avatarLabel);

        // Info
        VBox info = new VBox();
        info.setSpacing(2);

        headerName = new Label("Sélectionnez une conversation");
        headerName.setFont(Font.font("Segoe UI", FontWeight.BOLD, 15));
        headerName.setTextFill(Color.web(COLOR_TEXT_PRIMARY));

        headerStatus = new Label("");
        headerStatus.setFont(Font.font("Segoe UI", 12));
        headerStatus.setTextFill(Color.web(COLOR_TEXT_SECOND));

        info.getChildren().addAll(headerName, headerStatus);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Boutons
        Button callBtn = createHeaderButton("📞");
        callBtn.setOnAction(e -> initiateCall(false));

        Button videoBtn = createHeaderButton("📹");
        videoBtn.setOnAction(e -> initiateCall(true));

        Button moreBtn = createHeaderButton("⋮");
        moreBtn.setOnAction(e -> showConversationMenu());

        header.getChildren().addAll(avatar, info, spacer, callBtn, videoBtn, moreBtn);
        return header;
    }

    private Button createHeaderButton(String emoji) {
        Button btn = new Button(emoji);
        btn.setFont(Font.font("Segoe UI Emoji", 16));
        btn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 8;");
        return btn;
    }

    private VBox createMessagesArea() {
        chatContainer = new VBox();
        chatContainer.setSpacing(8);
        chatContainer.setPadding(new Insets(16));
        chatContainer.setStyle("-fx-background-color: " + COLOR_BG_CHAT + ";");

        scrollPane = new ScrollPane(chatContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setStyle("-fx-background: " + COLOR_BG_CHAT + "; -fx-background-color: transparent;");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        VBox container = new VBox(scrollPane);
        VBox.setVgrow(container, Priority.ALWAYS);
        return container;
    }

    private HBox createInputBar() {
        HBox inputBar = new HBox();
        inputBar.setAlignment(Pos.CENTER_LEFT);
        inputBar.setPadding(new Insets(12, 16, 12, 16));
        inputBar.setSpacing(8);
        inputBar.setStyle("-fx-background-color: " + COLOR_BG_WHITE + ";" +
                "-fx-border-width: 1 0 0 0; -fx-border-color: " + COLOR_BORDER + ";");

        // Bouton fichier
        Button attachBtn = new Button("📎");
        attachBtn.setFont(Font.font("Segoe UI Emoji", 16));
        attachBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");
        attachBtn.setOnAction(e -> attachFile());

        // TextArea input
        inputArea = new TextArea();
        inputArea.setPromptText("Écrivez un message... (Shift+Enter pour saut de ligne)");
        inputArea.setFont(Font.font("Segoe UI", 13));
        inputArea.setWrapText(true);
        inputArea.setPrefRowCount(1);
        inputArea.setMaxHeight(100);
        inputArea.setStyle("-fx-background-color: " + COLOR_BG_CHAT + ";" +
                "-fx-background-radius: 18; -fx-border-radius: 18;" +
                "-fx-border-color: " + COLOR_BORDER + ";" +
                "-fx-border-width: 1; -fx-padding: 8 12;");
        HBox.setHgrow(inputArea, Priority.ALWAYS);

        // Enter = envoi, Shift+Enter = saut de ligne
        inputArea.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER && !e.isShiftDown()) {
                e.consume();
                sendMessage();
            }
        });

        // Bouton envoyer
        Button sendBtn = new Button("▶");
        sendBtn.setFont(Font.font("Segoe UI", 16));
        sendBtn.setStyle("-fx-background-color: " + COLOR_ACCENT + ";" +
                "-fx-text-fill: white; -fx-background-radius: 50%;" +
                "-fx-min-width: 40; -fx-min-height: 40; -fx-cursor: hand;");
        sendBtn.setOnAction(e -> sendMessage());

        inputBar.getChildren().addAll(attachBtn, inputArea, sendBtn);
        return inputBar;
    }

    // ═══════════════════════════════════════════════════════════
    // ENVOI DE MESSAGES
    // ═══════════════════════════════════════════════════════════

    public void sendMessage() {
        String text = inputArea.getText().trim();
        if (text.isEmpty()) return;

        // ─── Optimistic UI ───
        // Afficher immédiatement la bulle verte (avant confirmation serveur)
        ChatMessage optimisticMsg = new ChatMessage(null, null, text, true, LocalDateTime.now(), MessageStatus.SENDING);
        addMessageBubble(optimisticMsg);
        messages.add(optimisticMsg);

        inputArea.clear();

        // ─── Envoi réseau ───
        NetworkClient client = NetworkClient.getInstance();
        if (client.isAuthenticated()) {
            if (isGroup && currentConversationId != null) {
                client.sendGroupMessage(currentConversationId, text);
            } else if (currentContactId != null) {
                client.sendTextMessage(currentContactId, text);
            }

            // Simuler confirmation (dans un vrai app, on attend la réponse serveur)
            Platform.runLater(() -> {
                optimisticMsg.status = MessageStatus.SENT;
                updateMessageStatus(optimisticMsg);
            });
        }
    }

    public void receiveMessage(Long senderId, String senderName, String text) {
        Platform.runLater(() -> {
            ChatMessage msg = new ChatMessage(senderId, senderName, text, false, LocalDateTime.now(), MessageStatus.READ);
            addMessageBubble(msg);
            messages.add(msg);
        });
    }

    // ═══════════════════════════════════════════════════════════
    // AFFICHAGE DES BULLES
    // ═══════════════════════════════════════════════════════════

    private void addMessageBubble(ChatMessage msg) {
        HBox bubbleRow = new HBox();
        bubbleRow.setPadding(new Insets(4, 0, 4, 0));

        if (msg.isSent) {
            // Message envoyé (aligné à droite, bulle verte)
            bubbleRow.setAlignment(Pos.CENTER_RIGHT);

            VBox bubbleContainer = new VBox();
            bubbleContainer.setAlignment(Pos.CENTER_RIGHT);
            bubbleContainer.setSpacing(2);

            // Bulle
            TextFlow bubble = createBubble(msg.text, COLOR_SENT_BG, true);

            // Métadonnées (heure + checkmarks)
            HBox meta = new HBox();
            meta.setAlignment(Pos.CENTER_RIGHT);
            meta.setSpacing(4);

            Label time = new Label(msg.time.format(timeFormatter));
            time.setFont(Font.font("Segoe UI", 10));
            time.setTextFill(Color.web(COLOR_TEXT_SECOND));

            Label checks = new Label(getStatusIcon(msg.status));
            checks.setFont(Font.font("Segoe UI", 10));
            checks.setTextFill(Color.web(msg.status == MessageStatus.READ ? COLOR_READ_CHECK : COLOR_TEXT_SECOND));

            meta.getChildren().addAll(time, checks);
            bubbleContainer.getChildren().addAll(bubble, meta);

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            bubbleRow.getChildren().addAll(spacer, bubbleContainer);

        } else {
            // Message reçu (aligné à gauche, bulle blanche)
            bubbleRow.setAlignment(Pos.CENTER_LEFT);

            // Avatar petit
            StackPane avatar = new StackPane();
            avatar.setPrefSize(32, 32);
            Circle circle = new Circle(16);
            circle.setFill(Color.web("#E0E0E0"));
            Label avLabel = new Label(msg.senderName != null ? msg.senderName.substring(0, 1).toUpperCase() : "?");
            avLabel.setFont(Font.font("Segoe UI", 12));
            avatar.getChildren().addAll(circle, avLabel);

            VBox bubbleContainer = new VBox();
            bubbleContainer.setSpacing(2);
            bubbleContainer.setPadding(new Insets(0, 0, 0, 8));

            // Nom (pour groupes)
            if (isGroup && msg.senderName != null) {
                Label name = new Label(msg.senderName);
                name.setFont(Font.font("Segoe UI", 10));
                name.setTextFill(Color.web(COLOR_TEXT_SECOND));
                bubbleContainer.getChildren().add(name);
            }

            TextFlow bubble = createBubble(msg.text, COLOR_RECEIVED_BG, false);

            Label time = new Label(msg.time.format(timeFormatter));
            time.setFont(Font.font("Segoe UI", 10));
            time.setTextFill(Color.web(COLOR_TEXT_SECOND));

            bubbleContainer.getChildren().addAll(bubble, time);

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            bubbleRow.getChildren().addAll(avatar, bubbleContainer, spacer);
        }

        // Animation fade-in
        bubbleRow.setOpacity(0);
        chatContainer.getChildren().add(bubbleRow);

        FadeTransition fade = new FadeTransition(Duration.millis(200), bubbleRow);
        fade.setToValue(1);
        fade.play();

        // Scroll en bas
        Platform.runLater(() -> scrollPane.setVvalue(1.0));
    }

    private TextFlow createBubble(String text, String bgColor, boolean sent) {
        TextFlow bubble = new TextFlow();
        bubble.setPadding(new Insets(10, 14, 10, 14));
        bubble.setStyle("-fx-background-color: " + bgColor + ";" +
                "-fx-background-radius: " + (sent ? "18 18 4 18" : "18 18 18 4") + ";" +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 2, 0, 0, 1);");
        bubble.setMaxWidth(400);

        Text content = new Text(text);
        content.setFont(Font.font("Segoe UI", 14));
        content.setFill(Color.web(COLOR_TEXT_PRIMARY));
        bubble.getChildren().add(content);

        return bubble;
    }

    private String getStatusIcon(MessageStatus status) {
        return switch (status) {
            case SENDING -> "○";
            case SENT -> "✓";
            case DELIVERED -> "✓✓";
            case READ -> "✓✓";
        };
    }

    private void updateMessageStatus(ChatMessage msg) {
        // TODO: Mettre à jour l'affichage des checkmarks
    }

    // ═══════════════════════════════════════════════════════════
    // NAVIGATION
    // ═══════════════════════════════════════════════════════════

    public void openConversation(Long id, String name, boolean isGroup, String status) {
        this.currentConversationId = id;
        this.currentContactName = name;
        this.isGroup = isGroup;

        headerName.setText(name);
        headerStatus.setText(status);

        // Vider les messages et charger (mock pour l'instant)
        chatContainer.getChildren().clear();
        messages.clear();

        // Messages mock pour démo
        if (!isGroup) {
            addMockConversation(name);
        }
    }

    private void addMockConversation(String name) {
        receiveMessage(2L, name, "Salut ! Ça va ?");
        receiveMessage(2L, name, "Tu es dispo pour un appel cet après-midi ?");

        ChatMessage myMsg = new ChatMessage(null, null, "Oui super ! À quelle heure ?", true, LocalDateTime.now().minusMinutes(5), MessageStatus.READ);
        addMessageBubble(myMsg);
        messages.add(myMsg);
    }

    // ═══════════════════════════════════════════════════════════
    // ACTIONS
    // ═══════════════════════════════════════════════════════════

    private void initiateCall(boolean video) {
        // TODO: Ouvrir dialog d'appel
        System.out.println("Appel " + (video ? "vidéo" : "audio") + " vers " + currentContactName);
    }

    private void showConversationMenu() {
        ContextMenu menu = new ContextMenu();
        menu.getItems().addAll(
                new MenuItem("Marquer comme lu"),
                new MenuItem("Supprimer la conversation"),
                new MenuItem("Bloquer"),
                new SeparatorMenuItem(),
                new MenuItem("Vider l'historique")
        );
        // TODO: Afficher le menu
    }

    private void attachFile() {
        // TODO: Ouvrir file chooser
        System.out.println("Joindre un fichier...");
    }

    public void showTyping(String userName) {
        Platform.runLater(() -> {
            typingLabel.setText(userName + " écrit...");
            typingLabel.setVisible(true);

            // Disparaît après 3 secondes
            new Thread(() -> {
                try {
                    Thread.sleep(3000);
                    Platform.runLater(() -> typingLabel.setVisible(false));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
        });
    }

    // ═══════════════════════════════════════════════════════════
    // RECORDS
    // ═══════════════════════════════════════════════════════════

    private enum MessageStatus { SENDING, SENT, DELIVERED, READ }

    private static class ChatMessage {
        public Long senderId;
        public String senderName;
        public String text;
        public boolean isSent;
        public LocalDateTime time;
        public MessageStatus status;

        ChatMessage(Long senderId, String senderName, String text,
                    boolean isSent, LocalDateTime time, MessageStatus status) {
            this.senderId = senderId;
            this.senderName = senderName;
            this.text = text;
            this.isSent = isSent;
            this.time = time;
            this.status = status;
        }
    }
}