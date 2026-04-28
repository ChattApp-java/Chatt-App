package org.example.tpchatjavafx.client.controller;

import javafx.application.Platform;
import org.example.tpchatjavafx.client.ChatClientApp;
import org.example.tpchatjavafx.client.voice.VoiceCallWindow;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import org.example.tpchatjavafx.client.NetworkClient;
import org.example.tpchatjavafx.client.model.ChatMessage;
import org.example.tpchatjavafx.client.util.UiMessage;
import org.example.tpchatjavafx.client.video.VideoCallController;
import org.example.tpchatjavafx.client.video.VideoCallWindow;
import org.example.tpchatjavafx.client.voice.VoiceCallSession;
import org.example.tpchatjavafx.common.MessageType;

import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainChatController {

    @FXML private Label     chatTitleLabel;
    @FXML private Label     chatStatusLabel;
    @FXML private Label     chatAvatarLabel;
    @FXML private ListView<String>    privateListView;
    @FXML private ListView<String>    groupListView;
    @FXML private Tab       privateTab;
    @FXML private Tab       groupTab;
    @FXML private ListView<UiMessage> messagesListView;
    @FXML private TextField messageField;
    @FXML private Button    emojiButton;
    @FXML private Button    recordAudioButton;
    @FXML private Button    voiceCallButton;
    @FXML private Button    videoCallButton;
    @FXML private TabPane   tabPane;

    private NetworkClient networkClient;
    private String username;

    private String currentPrivateTarget = null;
    private String currentGroupId = null;

    private final Map<String, ObservableList<UiMessage>> privateConversations = new HashMap<>();
    private final Map<String, ObservableList<UiMessage>> groupConversations = new HashMap<>();

    private ContextMenu emojiPicker;
    private static final List<String> EMOJIS = Arrays.asList(
            "😀","😁","😂","🤣","😊","😍","😘","😎","🤩","😇",
            "😅","😢","😭","🤔","😴","😡","👍","🙏","👏","🔥",
            "🥳","🤗","💡","✅","❤️","💬","🎉","⚡","🍀","☕"
    );

    private volatile boolean recordingAudio = false;
    private TargetDataLine targetDataLine;
    private Thread recordingThread;

    // 🔊 Voice call session
    private VoiceCallSession currentVoiceCall;
    private String voiceCallPeer = null;

    @FXML
    private void initialize() {
        buildEmojiPicker();
        updateRecordButtonState();
        setupMessageBubbles();
        setupContactCellFactory();
        if (tabPane != null && groupTab != null) {
            tabPane.getTabs().remove(groupTab);
        }
    }

    public void init(NetworkClient networkClient, String username) {
        this.networkClient = networkClient;
        this.username = username;

        networkClient.setOnMessageReceived(this::onMessageReceived);
        networkClient.setOnUserListReceived(this::updateOnlineUsers);

        privateListView.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            if (n != null) openPrivateChat(n);
        });
        groupListView.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            if (n != null) openGroupChat(n);
        });
    }

    // ── Sidebar cell factory WhatsApp-like ─────────────────────
    private void setupContactCellFactory() {
        privateListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(String user, boolean empty) {
                super.updateItem(user, empty);
                if (empty || user == null) { setGraphic(null); setText(null); return; }
                String initial = user.substring(0, 1).toUpperCase();
                // Avatar
                Label av = new Label(initial);
                av.getStyleClass().add("avatar-letter");
                StackPane avatar = new StackPane(av);
                avatar.getStyleClass().add("avatar-circle");
                // Avatar couleur selon hash
                String[] colors = {"#00A884","#2196F3","#9C27B0","#FF5722","#FF9800"};
                avatar.setStyle("-fx-background-color:" + colors[Math.abs(user.hashCode()) % colors.length] + "; -fx-background-radius:50%; -fx-min-width:42px; -fx-min-height:42px; -fx-max-width:42px; -fx-max-height:42px;");
                // Infos
                Label name = new Label(user);
                name.setStyle("-fx-text-fill:#E9EDEF; -fx-font-size:14px; -fx-font-weight:bold;");
                Label sub  = new Label("● En ligne");
                sub.setStyle("-fx-text-fill:#25D366; -fx-font-size:11px;");
                VBox info = new VBox(3, name, sub);
                HBox row = new HBox(12, avatar, info);
                row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                row.setStyle("-fx-padding: 8 12;");
                setGraphic(row);
                setText(null);
            }
        });
    }

    // ── Mise à jour liste utilisateurs connectés ───────────────
    private void updateOnlineUsers(List<String> users) {
        Platform.runLater(() -> {
            ObservableList<String> items = privateListView.getItems();
            for (String u : users) {
                if (!u.equals(username) && !u.isEmpty() && !items.contains(u)) {
                    items.add(u);
                }
            }
            // Retirer ceux qui ne sont plus connectés (sauf les conversations existantes)
            items.removeIf(u -> !users.contains(u) && !privateConversations.containsKey(u));
        });
    }

    // ── Déconnexion ────────────────────────────────────────────
    @FXML
    private void onLogout() {
        if (networkClient != null) networkClient.close();
        try { ChatClientApp.showLoginView(); }
        catch (Exception e) { showInfo("Erreur retour login : " + e.getMessage()); }
    }

    private void openPrivateChat(String other) {
        currentPrivateTarget = other;
        currentGroupId = null;
        tabPane.getSelectionModel().select(0);
        if (chatTitleLabel  != null) chatTitleLabel.setText(other);
        if (chatStatusLabel != null) chatStatusLabel.setText("● En ligne");
        if (chatAvatarLabel != null) chatAvatarLabel.setText(other.substring(0,1).toUpperCase());
        ObservableList<UiMessage> msgs =
                privateConversations.computeIfAbsent(other, k -> FXCollections.observableArrayList());
        messagesListView.setItems(msgs);
        updateCallButtonsVisibility();
    }

    private void openGroupChat(String groupId) {
        currentGroupId = groupId;
        currentPrivateTarget = null;
        tabPane.getSelectionModel().select(1); // Groups tab
        if (chatTitleLabel  != null) chatTitleLabel.setText("Groupe: " + groupId);
        if (chatStatusLabel != null) chatStatusLabel.setText("Membres connectés");
        if (chatAvatarLabel != null) chatAvatarLabel.setText("G");
        ObservableList<UiMessage> msgs =
                groupConversations.computeIfAbsent(groupId, k -> FXCollections.observableArrayList());
        messagesListView.setItems(msgs);
        updateCallButtonsVisibility();
    }

    private void onMessageReceived(ChatMessage msg) {
        switch (msg.getType()) {
            case SYSTEM       -> addSystemMessage(msg);
            case PRIVATE      -> addPrivateMessage(msg);
            case GROUP        -> addGroupMessage(msg);
            case PRIVATE_AUDIO -> handleIncomingPrivateAudio(msg);
            case GROUP_AUDIO  -> handleIncomingGroupAudio(msg);
            case PRIVATE_IMAGE -> handleIncomingPrivateImage(msg);
            case GROUP_IMAGE  -> handleIncomingGroupImage(msg);
            case PRIVATE_FILE -> handleIncomingPrivateFile(msg);
            case GROUP_FILE   -> handleIncomingGroupFile(msg);
            // Appel vidéo
            case VIDEO_CALL_REQUEST -> Platform.runLater(() -> handleCallRequest(msg));
            case VIDEO_CALL_ACCEPT  -> Platform.runLater(() -> startVideoWindow(msg.getFrom(), true));
            case VIDEO_CALL_REJECT  -> Platform.runLater(() -> showInfo("Appel refusé par " + msg.getFrom()));
            case VIDEO_CALL_END     -> Platform.runLater(this::endVideo);
            case VIDEO_FRAME        -> Platform.runLater(() -> VideoCallController.receiveFrame(msg.getBinaryData()));
            // Appel vocal
            case VOICE_CALL_REQUEST -> Platform.runLater(() -> handleVoiceCallRequest(msg));
            case VOICE_CALL_ACCEPT  -> Platform.runLater(() -> startVoiceSession(msg.getFrom()));
            case VOICE_CALL_REJECT  -> Platform.runLater(() -> showInfo("Appel vocal refusé par " + msg.getFrom()));
            case VOICE_CALL_END     -> Platform.runLater(this::endVoiceCall);
            case VOICE_FRAME        -> Platform.runLater(() -> { if (currentVoiceCall != null) currentVoiceCall.playRemoteAudio(msg.getBinaryData()); });
            default -> {}
        }
    }

    private void addSystemMessage(ChatMessage msg) {
        String key = "SYSTEM";

        privateConversations.putIfAbsent(key, FXCollections.observableArrayList());
        privateConversations.get(key).add(new UiMessage(
                UiMessage.Kind.TEXT,
                false,
                "[SYSTEM] " + msg.getContent(),
                null
        ));

        if (key.equals(currentPrivateTarget)) {
            messagesListView.setItems(privateConversations.get(key));
        }
        if (!privateListView.getItems().contains(key)) {
            privateListView.getItems().add(key);
        }
    }

    private void addPrivateMessage(ChatMessage msg) {
        String other = msg.getFrom().equals(username) ? msg.getTo() : msg.getFrom();

        privateConversations.putIfAbsent(other, FXCollections.observableArrayList());
        boolean own = msg.getFrom().equals(username);
        privateConversations.get(other).add(new UiMessage(
                UiMessage.Kind.TEXT,
                own,
                msg.getContent(),
                null
        ));

        if (other.equals(currentPrivateTarget) && isPrivateTabSelected()) {
            messagesListView.setItems(privateConversations.get(other));
        }
        if (!privateListView.getItems().contains(other)) {
            privateListView.getItems().add(other);
        }
    }

    private void addGroupMessage(ChatMessage msg) {
        String groupId = msg.getGroupId();

        groupConversations.putIfAbsent(groupId, FXCollections.observableArrayList());
        boolean own = msg.getFrom().equals(username);
        groupConversations.get(groupId).add(new UiMessage(
                UiMessage.Kind.TEXT,
                own,
                (own ? "" : msg.getFrom() + ": ") + msg.getContent(),
                null
        ));

        if (groupId.equals(currentGroupId) && isGroupTabSelected()) {
            messagesListView.setItems(groupConversations.get(groupId));
        }
        if (!groupListView.getItems().contains(groupId)) {
            groupListView.getItems().add(groupId);
        }
    }

    // ---------------- VIDEO CALL ----------------

    private void handleCallRequest(ChatMessage msg) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Incoming Call");
        alert.setHeaderText("Video call from " + msg.getFrom());
        alert.setContentText("Accept the call?");

        ButtonType accept = new ButtonType("Accept");
        ButtonType reject = new ButtonType("Reject");

        alert.getButtonTypes().setAll(accept, reject);

        alert.showAndWait().ifPresent(r -> {
            if (r == accept) {
                networkClient.send(new ChatMessage(
                        MessageType.VIDEO_CALL_ACCEPT,
                        username,
                        msg.getFrom(),
                        null,
                        ""
                ));
                startVideoWindow(msg.getFrom(), false);
            } else {
                networkClient.send(new ChatMessage(
                        MessageType.VIDEO_CALL_REJECT,
                        username,
                        msg.getFrom(),
                        null,
                        ""
                ));
            }
        });
    }

    private void startVideoWindow(String otherUser, boolean caller) {
        VideoCallWindow.open(networkClient, username, otherUser, caller);
    }

    private void endVideo() {
        VideoCallWindow.closeCurrent();
    }

    // ---------------- VOICE CALL ----------------

    private void handleVoiceCallRequest(ChatMessage msg) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Incoming Voice Call");
        alert.setHeaderText("Voice call from " + msg.getFrom());
        alert.setContentText("Accept the call?");

        ButtonType accept = new ButtonType("Accept");
        ButtonType reject = new ButtonType("Reject");

        alert.getButtonTypes().setAll(accept, reject);

        alert.showAndWait().ifPresent(result -> {
            if (result == accept) {
                networkClient.send(new ChatMessage(
                        MessageType.VOICE_CALL_ACCEPT,
                        username,
                        msg.getFrom(),
                        null,
                        ""
                ));
                startVoiceSession(msg.getFrom());
            } else {
                networkClient.send(new ChatMessage(
                        MessageType.VOICE_CALL_REJECT,
                        username,
                        msg.getFrom(),
                        null,
                        ""
                ));
            }
        });
    }

    private void startVoiceSession(String otherUser) {
        if (currentVoiceCall != null) {
            showInfo("A voice call is already in progress.");
            return;
        }

        try {
            currentVoiceCall = new VoiceCallSession(networkClient, username, otherUser);
            currentVoiceCall.start();
            voiceCallPeer = otherUser;

            // 🔲 Open call window with timer + hangup callback
            VoiceCallWindow.open(username, otherUser, () -> {
                // This runs when YOU end the call (button or X)
                if (voiceCallPeer != null) {
                    networkClient.send(new ChatMessage(
                            MessageType.VOICE_CALL_END,
                            username,
                            voiceCallPeer,
                            null,
                            ""
                    ));
                }
                endVoiceCall(); // stop local session + close window
            });

            // Optional toast
            // showInfo("Voice call started with " + otherUser);
        } catch (Exception e) {
            showInfo("Unable to start voice call: " + e.getMessage());
        }
    }


    private void endVoiceCall() {
        if (currentVoiceCall != null) {
            currentVoiceCall.stop();
            currentVoiceCall = null;
        }
        voiceCallPeer = null;
        VoiceCallWindow.close();
        showInfo("Voice call ended.");
    }


    private boolean isPrivateTabSelected() {
        return tabPane.getSelectionModel().getSelectedIndex() == 0;
    }

    private boolean isGroupTabSelected() {
        return tabPane.getSelectionModel().getSelectedIndex() == 1;
    }

    private void showInfo(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg);
        a.show();
    }

    @FXML
    private void onNewPrivate() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("New Private Chat");
        dialog.setHeaderText("Start a private chat");
        dialog.setContentText("Enter username:");
        dialog.showAndWait().ifPresent(name -> {
            name = name.trim();
            if (!name.isEmpty()) {
                privateConversations.putIfAbsent(name, FXCollections.observableArrayList());
                if (!privateListView.getItems().contains(name)) {
                    privateListView.getItems().add(name);
                }
                openPrivateChat(name);
            }
        });
    }

    @FXML
    private void onJoinGroup() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Join Group");
        dialog.setHeaderText("Join or create a group");
        dialog.setContentText("Group ID:");
        dialog.showAndWait().ifPresent(groupId -> {
            groupId = groupId.trim();
            if (!groupId.isEmpty()) {
                networkClient.send(new ChatMessage(
                        MessageType.JOIN_GROUP,
                        username,
                        null,
                        groupId,
                        ""
                ));

                groupConversations.putIfAbsent(groupId, FXCollections.observableArrayList());
                if (!groupListView.getItems().contains(groupId)) {
                    groupListView.getItems().add(groupId);
                }
                ensureGroupTabVisible();
                openGroupChat(groupId);
            }
        });
    }

    @FXML
    private void onNewGroup() {
        onJoinGroup();
    }

    @FXML
    private void onShowUnreadPrivate() {
        showInfo("Filtre des messages privés non lus à implémenter.");
    }

    @FXML
    private void onShowUnreadGroups() {
        showInfo("Filtre des messages de groupe non lus à implémenter.");
    }

    @FXML
    private void onSend() {
        String text = messageField.getText().trim();
        if (text.isEmpty()) return;

        if (isPrivateTabSelected() && currentPrivateTarget != null) {
            ChatMessage msg = new ChatMessage(
                    MessageType.PRIVATE,
                    username,
                    currentPrivateTarget,
                    null,
                    text
            );
            addPrivateMessage(msg);
            networkClient.send(msg);
        } else if (isGroupTabSelected() && currentGroupId != null) {
            networkClient.send(new ChatMessage(
                    MessageType.GROUP,
                    username,
                    null,
                    currentGroupId,
                    text
            ));
        }

        messageField.clear();
    }

    @FXML
    private void onUploadFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choisir un fichier");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif"),
                new FileChooser.ExtensionFilter("Tous les fichiers", "*.*")
        );
        File file = chooser.showOpenDialog(getWindow());
        if (file == null) return;

        String name = file.getName().toLowerCase();
        boolean isImage = name.endsWith(".png") || name.endsWith(".jpg")
                || name.endsWith(".jpeg") || name.endsWith(".gif");

        try {
            byte[] data = Files.readAllBytes(file.toPath());

            if (isImage && (isPrivateTabSelected() && currentPrivateTarget != null
                    || isGroupTabSelected() && currentGroupId != null)) {

                String tempPath = saveTempFile("img-", name.substring(name.lastIndexOf('.') + 1), data);

                if (isPrivateTabSelected() && currentPrivateTarget != null) {
                    ChatMessage msg = new ChatMessage(
                            MessageType.PRIVATE_IMAGE,
                            username,
                            currentPrivateTarget,
                            null,
                            file.getName()
                    );
                    msg.setBinaryData(data);
                    addLocalImageMessage(true, tempPath);
                    networkClient.send(msg);
                } else if (isGroupTabSelected() && currentGroupId != null) {
                    ChatMessage msg = new ChatMessage(
                            MessageType.GROUP_IMAGE,
                            username,
                            null,
                            currentGroupId,
                            file.getName()
                    );
                    msg.setBinaryData(data);
                    addLocalGroupImageMessage(true, tempPath);
                    networkClient.send(msg);
                }
            } else {
                // generic file
                if (isPrivateTabSelected() && currentPrivateTarget != null) {
                    ChatMessage msg = new ChatMessage(
                            MessageType.PRIVATE_FILE,
                            username,
                            currentPrivateTarget,
                            null,
                            file.getName()
                    );
                    msg.setBinaryData(data);
                    addLocalFileMessage(true, file.getName(), file.getAbsolutePath());
                    networkClient.send(msg);
                } else if (isGroupTabSelected() && currentGroupId != null) {
                    ChatMessage msg = new ChatMessage(
                            MessageType.GROUP_FILE,
                            username,
                            null,
                            currentGroupId,
                            file.getName()
                    );
                    msg.setBinaryData(data);
                    addLocalGroupFileMessage(true, file.getName(), file.getAbsolutePath());
                    networkClient.send(msg);
                } else {
                    appendToMessageField(formatAttachmentLabel("📎", file));
                }
            }
        } catch (IOException e) {
            showInfo("Impossible de lire le fichier: " + e.getMessage());
        }
    }

    @FXML
    private void onRecordAudio() {
        if (recordingAudio) {
            stopAudioRecording();
        } else {
            startAudioRecording();
        }
    }

    @FXML
    private void onToggleEmojiPicker() {
        if (emojiPicker == null || emojiButton == null) return;

        if (emojiPicker.isShowing()) {
            emojiPicker.hide();
        } else {
            emojiPicker.show(emojiButton, Side.TOP, 0, 0);
        }
    }

    @FXML
    private void onStartVideoCall() {
        if (currentPrivateTarget == null) {
            showInfo("Select a private contact first.");
            return;
        }

        networkClient.send(new ChatMessage(
                MessageType.VIDEO_CALL_REQUEST,
                username,
                currentPrivateTarget,
                null,
                "call_request"
        ));
    }

    @FXML
    private void onStartVoiceCall() {
        if (currentPrivateTarget == null) {
            showInfo("Select a private contact first.");
            return;
        }

        if (currentVoiceCall != null) {
            showInfo("A voice call is already in progress.");
            return;
        }

        // send request; real session starts on ACCEPT
        networkClient.send(new ChatMessage(
                MessageType.VOICE_CALL_REQUEST,
                username,
                currentPrivateTarget,
                null,
                "voice_call_request"
        ));

        showInfo("Voice call request sent to " + currentPrivateTarget + "...");
    }

    private void buildEmojiPicker() {
        emojiPicker = new ContextMenu();
        FlowPane emojisPane = new FlowPane();
        emojisPane.setPadding(new Insets(8));
        emojisPane.setHgap(6);
        emojisPane.setVgap(6);
        emojisPane.setPrefWrapLength(120);

        for (String emoji : EMOJIS) {
            Button btn = new Button(emoji);
            btn.getStyleClass().add("btn-icon");
            btn.setOnAction(e -> {
                insertEmoji(emoji);
                emojiPicker.hide();
            });
            emojisPane.getChildren().add(btn);
        }

        CustomMenuItem content = new CustomMenuItem(emojisPane, false);
        emojiPicker.getItems().add(content);
    }

    private void insertEmoji(String emoji) {
        if (messageField == null) return;
        messageField.insertText(messageField.getCaretPosition(), emoji);
        messageField.requestFocus();
    }

    private void appendToMessageField(String addition) {
        if (messageField == null || addition == null || addition.isEmpty()) return;
        if (!messageField.getText().isEmpty() && !messageField.getText().endsWith(" ")) {
            messageField.appendText(" ");
        }
        messageField.appendText(addition + " ");
        messageField.requestFocus();
    }

    private Window getWindow() {
        return messageField != null && messageField.getScene() != null ? messageField.getScene().getWindow() : null;
    }

    private String formatAttachmentLabel(String icon, File file) {
        return icon + " " + file.getName() + " (" + humanReadableSize(file.length()) + ")";
    }

    private String humanReadableSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        double kb = bytes / 1024.0;
        if (kb < 1024) return new DecimalFormat("#.##").format(kb) + " KB";
        double mb = kb / 1024.0;
        if (mb < 1024) return new DecimalFormat("#.##").format(mb) + " MB";
        double gb = mb / 1024.0;
        return new DecimalFormat("#.##").format(gb) + " GB";
    }

    private void startAudioRecording() {
        AudioFormat format = new AudioFormat(16000, 16, 1, true, false);
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
        if (!AudioSystem.isLineSupported(info)) {
            showInfo("L'enregistrement audio n'est pas supporté sur cet appareil.");
            return;
        }
        try {
            targetDataLine = (TargetDataLine) AudioSystem.getLine(info);
            targetDataLine.open(format);
            targetDataLine.start();
            recordingAudio = true;
            updateRecordButtonState();
            recordingThread = new Thread(() -> captureAudio(format));
            recordingThread.setDaemon(true);
            recordingThread.start();
        } catch (LineUnavailableException e) {
            showInfo("Impossible d'accéder au micro: " + e.getMessage());
        }
    }

    private void stopAudioRecording() {
        recordingAudio = false;
        if (targetDataLine != null) {
            targetDataLine.stop();
            targetDataLine.close();
        }
        updateRecordButtonState();
    }

    private void captureAudio(AudioFormat format) {
        byte[] buffer = new byte[4096];
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            while (recordingAudio) {
                int count = targetDataLine.read(buffer, 0, buffer.length);
                if (count > 0) {
                    out.write(buffer, 0, count);
                }
            }
            byte[] data = out.toByteArray();
            Platform.runLater(() -> handleRecordedAudio(format, data));
        } catch (Exception e) {
            Platform.runLater(() -> showInfo("Erreur pendant l'enregistrement audio."));
        } finally {
            try {
                out.close();
            } catch (IOException ignored) {}
        }
    }

    private void handleRecordedAudio(AudioFormat format, byte[] audioData) {
        if (audioData == null || audioData.length == 0) {
            showInfo("Aucun audio capturé.");
            return;
        }
        try {
            Path tempFile = Files.createTempFile("audio-message-", ".wav");
            try (AudioInputStream stream =
                         new AudioInputStream(new ByteArrayInputStream(audioData),
                                 format,
                                 audioData.length / format.getFrameSize())) {
                AudioSystem.write(stream, AudioFileFormat.Type.WAVE, tempFile.toFile());
            }

            if (isPrivateTabSelected() && currentPrivateTarget != null) {
                ChatMessage msg = new ChatMessage(
                        MessageType.PRIVATE_AUDIO,
                        username,
                        currentPrivateTarget,
                        null,
                        "Audio"
                );
                msg.setBinaryData(Files.readAllBytes(tempFile));
                addLocalAudioMessage(true, tempFile.toString());
                networkClient.send(msg);
            } else if (isGroupTabSelected() && currentGroupId != null) {
                ChatMessage msg = new ChatMessage(
                        MessageType.GROUP_AUDIO,
                        username,
                        null,
                        currentGroupId,
                        "Audio"
                );
                msg.setBinaryData(Files.readAllBytes(tempFile));
                addLocalGroupAudioMessage(true, tempFile.toString());
                networkClient.send(msg);
            } else {
                appendToMessageField(formatAttachmentLabel("🎙️", tempFile.toFile()));
            }
        } catch (IOException e) {
            showInfo("Impossible d'enregistrer l'audio: " + e.getMessage());
        }
    }

    private void updateRecordButtonState() {
        if (recordAudioButton == null) return;
        if (recordingAudio) {
            recordAudioButton.setText("■");
            recordAudioButton.setStyle("-fx-text-fill: #f87171;");
        } else {
            recordAudioButton.setText("🎙");
            recordAudioButton.setStyle("");
        }
    }

    private void setupMessageBubbles() {
        messagesListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(UiMessage item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }

                boolean isOwn = item.isOwn();
                HBox row = new HBox();
                row.getStyleClass().add("bubble-row");
                row.setAlignment(isOwn ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

                switch (item.getKind()) {
                    case TEXT -> {
                        Label bubble = new Label(item.getText());
                        bubble.setWrapText(true);
                        bubble.setMaxWidth(420);
                        bubble.getStyleClass().add(isOwn ? "bubble-sent" : "bubble-received");
                        row.getChildren().add(bubble);
                    }
                    case IMAGE -> {
                        ImageView imageView = new ImageView();
                        try {
                            imageView.setImage(new Image(new File(item.getFilePath()).toURI().toString(), 200, 0, true, true));
                        } catch (Exception ignored) {}
                        imageView.getStyleClass().add(isOwn ? "bubble-sent" : "bubble-received");
                        row.getChildren().add(imageView);
                    }
                    case AUDIO -> {
                        Button play = new Button("▶");
                        play.getStyleClass().add("btn-icon");
                        play.setOnAction(e -> playAudio(item.getFilePath()));
                        Label label = new Label(" Message vocal");
                        label.getStyleClass().add(isOwn ? "bubble-sent" : "bubble-received");
                        HBox inner = new HBox(6, play, label);
                        row.getChildren().add(inner);
                    }
                    case FILE -> {
                        Label nameLabel = new Label("📎 " + item.getText());
                        nameLabel.getStyleClass().add(isOwn ? "bubble-sent" : "bubble-received");
                        Button downloadBtn = new Button("💾");
                        downloadBtn.getStyleClass().add("btn-icon");
                        downloadBtn.setOnAction(e -> downloadFile(item.getFilePath(), item.getText()));
                        HBox inner = new HBox(8, nameLabel, downloadBtn);
                        inner.setAlignment(isOwn ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
                        row.getChildren().add(inner);
                    }
                }

                setText(null);
                setGraphic(row);
            }
        });
    }

    private void updateCallButtonsVisibility() {
        boolean inPrivate = currentPrivateTarget != null && isPrivateTabSelected();
        if (voiceCallButton != null) {
            voiceCallButton.setVisible(inPrivate);
            voiceCallButton.setManaged(inPrivate);
        }
        if (videoCallButton != null) {
            videoCallButton.setVisible(inPrivate);
            videoCallButton.setManaged(inPrivate);
        }
    }

    private void ensureGroupTabVisible() {
        if (tabPane == null || groupTab == null) return;
        if (!tabPane.getTabs().contains(groupTab)) {
            tabPane.getTabs().add(groupTab);
        }
    }

    private String saveTempFile(String prefix, String extension, byte[] data) throws IOException {
        Path tmp = Files.createTempFile(prefix, "." + extension);
        try (FileOutputStream fos = new FileOutputStream(tmp.toFile())) {
            fos.write(data);
        }
        return tmp.toString();
    }

    private void addLocalAudioMessage(boolean own, String path) {
        if (currentPrivateTarget == null) return;
        privateConversations.putIfAbsent(currentPrivateTarget, FXCollections.observableArrayList());
        privateConversations.get(currentPrivateTarget).add(new UiMessage(
                UiMessage.Kind.AUDIO,
                own,
                "Audio",
                path
        ));
        if (isPrivateTabSelected()) {
            messagesListView.setItems(privateConversations.get(currentPrivateTarget));
        }
    }

    private void addLocalGroupAudioMessage(boolean own, String path) {
        if (currentGroupId == null) return;
        groupConversations.putIfAbsent(currentGroupId, FXCollections.observableArrayList());
        groupConversations.get(currentGroupId).add(new UiMessage(
                UiMessage.Kind.AUDIO,
                own,
                "Audio",
                path
        ));
        if (isGroupTabSelected()) {
            messagesListView.setItems(groupConversations.get(currentGroupId));
        }
    }

    private void handleIncomingPrivateAudio(ChatMessage msg) {
        if (msg.getBinaryData() == null) return;
        try {
            String path = saveTempFile("audio-in-", "wav", msg.getBinaryData());
            String other = msg.getFrom().equals(username) ? msg.getTo() : msg.getFrom();
            privateConversations.putIfAbsent(other, FXCollections.observableArrayList());
            privateConversations.get(other).add(new UiMessage(
                    UiMessage.Kind.AUDIO,
                    msg.getFrom().equals(username),
                    "Audio",
                    path
            ));
            if (other.equals(currentPrivateTarget) && isPrivateTabSelected()) {
                messagesListView.setItems(privateConversations.get(other));
            }
            if (!privateListView.getItems().contains(other)) {
                privateListView.getItems().add(other);
            }
        } catch (IOException e) {
            showInfo("Impossible de sauvegarder l'audio reçu.");
        }
    }

    private void handleIncomingGroupAudio(ChatMessage msg) {
        if (msg.getBinaryData() == null) return;
        String gid = msg.getGroupId();
        if (gid == null) return;
        try {
            String path = saveTempFile("audio-g-", "wav", msg.getBinaryData());
            groupConversations.putIfAbsent(gid, FXCollections.observableArrayList());
            groupConversations.get(gid).add(new UiMessage(
                    UiMessage.Kind.AUDIO,
                    msg.getFrom().equals(username),
                    "Audio",
                    path
            ));
            if (gid.equals(currentGroupId) && isGroupTabSelected()) {
                messagesListView.setItems(groupConversations.get(gid));
            }
            if (!groupListView.getItems().contains(gid)) {
                groupListView.getItems().add(gid);
            }
        } catch (IOException e) {
            showInfo("Impossible de sauvegarder l'audio reçu.");
        }
    }

    private void addLocalImageMessage(boolean own, String path) {
        if (currentPrivateTarget == null) return;
        privateConversations.putIfAbsent(currentPrivateTarget, FXCollections.observableArrayList());
        privateConversations.get(currentPrivateTarget).add(new UiMessage(
                UiMessage.Kind.IMAGE,
                own,
                "",
                path
        ));
        if (isPrivateTabSelected()) {
            messagesListView.setItems(privateConversations.get(currentPrivateTarget));
        }
    }

    private void addLocalGroupImageMessage(boolean own, String path) {
        if (currentGroupId == null) return;
        groupConversations.putIfAbsent(currentGroupId, FXCollections.observableArrayList());
        groupConversations.get(currentGroupId).add(new UiMessage(
                UiMessage.Kind.IMAGE,
                own,
                "",
                path
        ));
        if (isGroupTabSelected()) {
            messagesListView.setItems(groupConversations.get(currentGroupId));
        }
    }

    private void handleIncomingPrivateImage(ChatMessage msg) {
        if (msg.getBinaryData() == null) return;
        try {
            String ext = "png";
            String name = msg.getContent();
            if (name != null && name.contains(".")) {
                ext = name.substring(name.lastIndexOf('.') + 1);
            }
            String path = saveTempFile("img-in-", ext, msg.getBinaryData());
            String other = msg.getFrom().equals(username) ? msg.getTo() : msg.getFrom();
            privateConversations.putIfAbsent(other, FXCollections.observableArrayList());
            privateConversations.get(other).add(new UiMessage(
                    UiMessage.Kind.IMAGE,
                    msg.getFrom().equals(username),
                    "",
                    path
            ));
            if (other.equals(currentPrivateTarget) && isPrivateTabSelected()) {
                messagesListView.setItems(privateConversations.get(other));
            }
            if (!privateListView.getItems().contains(other)) {
                privateListView.getItems().add(other);
            }
        } catch (IOException e) {
            showInfo("Impossible de sauvegarder l'image reçue.");
        }
    }

    private void handleIncomingGroupImage(ChatMessage msg) {
        if (msg.getBinaryData() == null) return;
        String gid = msg.getGroupId();
        if (gid == null) return;
        try {
            String ext = "png";
            String name = msg.getContent();
            if (name != null && name.contains(".")) {
                ext = name.substring(name.lastIndexOf('.') + 1);
            }
            String path = saveTempFile("img-g-", ext, msg.getBinaryData());
            groupConversations.putIfAbsent(gid, FXCollections.observableArrayList());
            groupConversations.get(gid).add(new UiMessage(
                    UiMessage.Kind.IMAGE,
                    msg.getFrom().equals(username),
                    "",
                    path
            ));
            if (gid.equals(currentGroupId) && isGroupTabSelected()) {
                messagesListView.setItems(groupConversations.get(gid));
            }
            if (!groupListView.getItems().contains(gid)) {
                groupListView.getItems().add(gid);
            }
        } catch (IOException e) {
            showInfo("Impossible de sauvegarder l'image reçue.");
        }
    }

    private void handleIncomingPrivateFile(ChatMessage msg) {
        if (msg.getBinaryData() == null) return;
        try {
            String originalName = msg.getContent();
            String ext = "bin";
            if (originalName != null && originalName.contains(".")) {
                ext = originalName.substring(originalName.lastIndexOf('.') + 1);
            }
            String path = saveTempFile("file-in-", ext, msg.getBinaryData());
            String other = msg.getFrom().equals(username) ? msg.getTo() : msg.getFrom();

            privateConversations.putIfAbsent(other, FXCollections.observableArrayList());
            privateConversations.get(other).add(new UiMessage(
                    UiMessage.Kind.FILE,
                    msg.getFrom().equals(username),
                    originalName != null ? originalName : "Fichier",
                    path
            ));

            if (other.equals(currentPrivateTarget) && isPrivateTabSelected()) {
                messagesListView.setItems(privateConversations.get(other));
            }
            if (!privateListView.getItems().contains(other)) {
                privateListView.getItems().add(other);
            }
        } catch (IOException e) {
            showInfo("Impossible de sauvegarder le fichier reçu.");
        }
    }

    private void handleIncomingGroupFile(ChatMessage msg) {
        if (msg.getBinaryData() == null) return;
        String gid = msg.getGroupId();
        if (gid == null) return;
        try {
            String originalName = msg.getContent();
            String ext = "bin";
            if (originalName != null && originalName.contains(".")) {
                ext = originalName.substring(originalName.lastIndexOf('.') + 1);
            }
            String path = saveTempFile("file-g-", ext, msg.getBinaryData());

            groupConversations.putIfAbsent(gid, FXCollections.observableArrayList());
            groupConversations.get(gid).add(new UiMessage(
                    UiMessage.Kind.FILE,
                    msg.getFrom().equals(username),
                    originalName != null ? originalName : "Fichier",
                    path
            ));

            if (gid.equals(currentGroupId) && isGroupTabSelected()) {
                messagesListView.setItems(groupConversations.get(gid));
            }
            if (!groupListView.getItems().contains(gid)) {
                groupListView.getItems().add(gid);
            }
        } catch (IOException e) {
            showInfo("Impossible de sauvegarder le fichier reçu.");
        }
    }

    private void playAudio(String path) {
        if (path == null) return;
        new Thread(() -> {
            try (AudioInputStream ais = AudioSystem.getAudioInputStream(new File(path))) {
                Clip clip = AudioSystem.getClip();
                clip.open(ais);
                clip.start();
            } catch (Exception e) {
                Platform.runLater(() -> showInfo("Impossible de lire l'audio."));
            }
        }).start();
    }

    private void addLocalFileMessage(boolean own, String fileName, String path) {
        if (currentPrivateTarget == null) return;
        privateConversations.putIfAbsent(currentPrivateTarget, FXCollections.observableArrayList());
        privateConversations.get(currentPrivateTarget).add(new UiMessage(
                UiMessage.Kind.FILE,
                own,
                fileName,
                path
        ));
        if (isPrivateTabSelected()) {
            messagesListView.setItems(privateConversations.get(currentPrivateTarget));
        }
    }

    private void addLocalGroupFileMessage(boolean own, String fileName, String path) {
        if (currentGroupId == null) return;
        groupConversations.putIfAbsent(currentGroupId, FXCollections.observableArrayList());
        groupConversations.get(currentGroupId).add(new UiMessage(
                UiMessage.Kind.FILE,
                own,
                fileName,
                path
        ));
        if (isGroupTabSelected()) {
            messagesListView.setItems(groupConversations.get(currentGroupId));
        }
    }

    private void downloadFile(String path, String suggestedName) {
        if (path == null) return;

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save file");
        chooser.setInitialFileName(suggestedName != null ? suggestedName : "file");
        File dest = chooser.showSaveDialog(getWindow());
        if (dest == null) return;

        try {
            Files.copy(Path.of(path), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
            showInfo("Fichier sauvegardé : " + dest.getAbsolutePath());
        } catch (IOException e) {
            showInfo("Erreur lors de la sauvegarde du fichier : " + e.getMessage());
        }
    }
}
