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
import org.example.tpchatjavafx.dao.ContactDAO;
import org.example.tpchatjavafx.dao.UtilisateurDAO;
import org.example.tpchatjavafx.dao.MessageDAO;
import org.example.tpchatjavafx.dao.ConversationDAO;
import org.example.tpchatjavafx.dao.FichierMediaDAO;
import org.example.tpchatjavafx.model.Conversation;
import org.example.tpchatjavafx.model.FichierMedia;
import org.example.tpchatjavafx.model.Message;
import org.example.tpchatjavafx.model.Utilisateur;

import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.*;
import java.util.stream.Collectors;

public class MainChatController {

    @FXML private Label     chatTitleLabel;
    @FXML private Label     chatStatusLabel;
    @FXML private Label     chatAvatarLabel;
    @FXML private ListView<String>    privateListView;
    @FXML private Tab       privateTab;
    @FXML private ListView<UiMessage> messagesListView;
    @FXML private TextField messageField;
    @FXML private Button    emojiButton;
    @FXML private Button    recordAudioButton;
    @FXML private Button    voiceCallButton;
    @FXML private Button    videoCallButton;
    @FXML private TabPane   tabPane;
    
    @FXML private TextField searchContactField;

    private NetworkClient networkClient;
    private String username;
    private Utilisateur currentUser;

    private String currentPrivateTarget = null;
    private String currentConversationId = null;

    private final Map<String, ObservableList<UiMessage>> privateConversations = new HashMap<>();
    private final Map<String, String> userStatuses = new HashMap<>(); // username -> EN_LIGNE/NON_CONNECTE
    private final Set<Integer> processedMessageIds = new HashSet<>();
    private final ObservableList<String> allContacts = FXCollections.observableArrayList();

    private final ContactDAO contactDAO = new ContactDAO();
    private final UtilisateurDAO utilisateurDAO = new UtilisateurDAO();
    private final MessageDAO messageDAO = new MessageDAO();
    private final ConversationDAO conversationDAO = new ConversationDAO();
    private final FichierMediaDAO fichierMediaDAO = new FichierMediaDAO();

    private ContextMenu emojiPicker;
    private static final List<String> EMOJIS = Arrays.asList(
            "😀","😁","😂","🤣","😊","😍","😘","😎","🤩","😇",
            "😅","😢","😭","🤔","😴","😡","👍","🙏","👏","🔥",
            "🥳","🤗","💡","✅","❤️","💬","🎉","⚡","🍀","☕"
    );

    private volatile boolean recordingAudio = false;
    private TargetDataLine targetDataLine;
    private Thread recordingThread;

    private VoiceCallSession currentVoiceCall;
    private String voiceCallPeer = null;

    @FXML
    private void initialize() {
        buildEmojiPicker();
        updateRecordButtonState();
        setupMessageBubbles();
        setupContactCellFactory();
        
        // Remove groups tab if it exists
        if (tabPane != null && tabPane.getTabs().size() > 1) {
            tabPane.getTabs().remove(1);
        }
    }

    public void init(NetworkClient networkClient, String username, int userId) {
        this.networkClient = networkClient;
        this.username = username;
        this.currentUser = new Utilisateur();
        this.currentUser.setId(userId);
        this.currentUser.setUsername(username);
        
        networkClient.setOnMessageReceived(this::onMessageReceived);
        networkClient.setOnUserListReceived(this::updateOnlineUsers);
        networkClient.setOnContactListReceived(this::updateContactList);
        networkClient.setOnHistoryReceived(this::onHistoryReceived);
        networkClient.setOnUserStatusChanged(this::onUserStatusChanged);
        networkClient.setOnError(this::showInfo);

        privateListView.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            if (n != null) openPrivateChat(n);
        });
        
        setupSearchContactAutoCompletion();
        
        // Request current online users and contact list
        networkClient.requestUserList();
        networkClient.requestContacts();
    }

    private void updateContactList(List<String> contacts) {
        Platform.runLater(() -> {
            allContacts.clear();
            for (String contactName : contacts) {
                if (!allContacts.contains(contactName)) {
                    allContacts.add(contactName);
                    // Status will be updated by STATUS_UPDATE or requestUserList
                    if (!userStatuses.containsKey(contactName)) {
                        userStatuses.put(contactName, "NON_CONNECTE");
                    }
                }
            }
            privateListView.setItems(allContacts);
            privateListView.refresh();
        });
    }

    private void setupSearchContactAutoCompletion() {
        if (searchContactField == null) return;
        
        searchContactField.textProperty().addListener((observable, oldValue, newValue) -> {
            String query = newValue.trim().toLowerCase();
            if (query.isEmpty()) {
                privateListView.setItems(allContacts);
                return;
            }
            
            // Filter the master list of contacts
            ObservableList<String> filtered = FXCollections.observableArrayList();
            for (String contact : allContacts) {
                if (contact.toLowerCase().contains(query)) {
                    filtered.add(contact);
                }
            }
            privateListView.setItems(filtered);
        });
    }

    @FXML
    private void onAddContact() {
        String contactName = (searchContactField != null) ? searchContactField.getText().trim() : "";
        
        if (contactName.isEmpty()) {
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("Ajouter un contact");
            dialog.setHeaderText("Entrez le nom d'utilisateur du contact à ajouter");
            dialog.setContentText("Nom d'utilisateur :");
            Optional<String> result = dialog.showAndWait();
            if (result.isPresent()) {
                contactName = result.get().trim();
            } else {
                return;
            }
        }
        
        if (contactName.isEmpty()) return;
        
        if (contactName.equals(username)) {
            showInfo("Vous ne pouvez pas vous ajouter vous-même.");
            return;
        }

        // Request server to add contact
        networkClient.addContact(contactName);
        searchContactField.clear();
    }

    private void setupContactCellFactory() {
        privateListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(String user, boolean empty) {
                super.updateItem(user, empty);
                if (empty || user == null) { setGraphic(null); setText(null); return; }
                String initial = user.substring(0, 1).toUpperCase();
                
                Label av = new Label(initial);
                av.getStyleClass().add("avatar-letter");
                StackPane avatar = new StackPane(av);
                avatar.getStyleClass().add("avatar-circle");
                
                String[] colors = {"#00A884","#2196F3","#9C27B0","#FF5722","#FF9800"};
                avatar.setStyle("-fx-background-color:" + colors[Math.abs(user.hashCode()) % colors.length] + "; -fx-background-radius:50%; -fx-min-width:42px; -fx-min-height:42px; -fx-max-width:42px; -fx-max-height:42px;");
                
                Label name = new Label(user);
                name.setStyle("-fx-text-fill:#E9EDEF; -fx-font-size:14px; -fx-font-weight:bold;");
                
                String statut = userStatuses.getOrDefault(user, "NON_CONNECTE");
                Label sub = new Label(statut.equals("EN_LIGNE") ? "● En ligne" : "● Non connecté");
                sub.setStyle(statut.equals("EN_LIGNE") ? "-fx-text-fill:#25D366; -fx-font-size:11px;" : "-fx-text-fill:#8e8e8e; -fx-font-size:11px;");
                
                VBox info = new VBox(3, name, sub);
                HBox row = new HBox(12, avatar, info);
                row.setAlignment(Pos.CENTER_LEFT);
                row.setStyle("-fx-padding: 8 12;");
                setGraphic(row);
                setText(null);
            }
        });
    }

    private void onUserStatusChanged(ChatMessage msg) {
        String targetUser = msg.getFrom();
        String status = msg.getContent();
        userStatuses.put(targetUser, status);
        
        Platform.runLater(() -> {
            privateListView.refresh();
            if (targetUser.equals(currentPrivateTarget)) {
                updateChatHeaderStatus(status);
            }
        });
    }

    private void updateOnlineUsers(List<String> users) {
        // Initial bulk update
        for (String u : users) {
            if (!u.equals(username)) {
                userStatuses.put(u, "EN_LIGNE");
            }
        }
        Platform.runLater(() -> privateListView.refresh());
    }

    @FXML
    private void onLogout() {
        if (networkClient != null) networkClient.close();
        try { ChatClientApp.showLoginView(); }
        catch (Exception e) { showInfo("Erreur retour login : " + e.getMessage()); }
    }

    private void openPrivateChat(String other) {
        currentPrivateTarget = other;
        if (chatTitleLabel  != null) chatTitleLabel.setText(other);
        if (chatAvatarLabel != null) chatAvatarLabel.setText(other.substring(0,1).toUpperCase());
        
        String status = userStatuses.getOrDefault(other, "NON_CONNECTE");
        updateChatHeaderStatus(status);
        
        ObservableList<UiMessage> msgs = privateConversations.get(other);
        if (msgs == null) {
            msgs = FXCollections.observableArrayList();
            privateConversations.put(other, msgs);
            loadConversationHistory(other);
        }
        
        messagesListView.setItems(msgs);
        updateCallButtonsVisibility();
    }

    private void loadConversationHistory(String otherName) {
        networkClient.requestHistory(otherName);
    }
    
    private void onHistoryReceived(ChatMessage msg) {
        Platform.runLater(() -> {
            boolean own = msg.getFrom().equals(username);
            String other = own ? msg.getTo() : msg.getFrom();
            
            ObservableList<UiMessage> uiMsgs = privateConversations.computeIfAbsent(other, k -> FXCollections.observableArrayList());
            
            // Avoid duplicates
            if (msg.getMessageId() != -1) {
                if (processedMessageIds.contains(msg.getMessageId())) return;
                processedMessageIds.add(msg.getMessageId());
            }
            
            UiMessage uiMsg = new UiMessage(UiMessage.Kind.TEXT, own, msg.getContent(), null);
            uiMsgs.add(uiMsg);
            
            // Auto-scroll if it's the current view
            if (other.equals(currentPrivateTarget)) {
                messagesListView.scrollTo(uiMsgs.size() - 1);
            }
        });
    }
    
    private void updateChatHeaderStatus(String status) {
        if (chatStatusLabel != null) {
            chatStatusLabel.setText(status.equals("EN_LIGNE") ? "● En ligne" : "● Non connecté");
            chatStatusLabel.setStyle(status.equals("EN_LIGNE") ? "-fx-text-fill: #25D366;" : "-fx-text-fill: #8e8e8e;");
        }
    }

    private void onMessageReceived(ChatMessage msg) {
        // Avoid duplicates by tracking message ID
        if (msg.getMessageId() != -1) {
            if (processedMessageIds.contains(msg.getMessageId())) {
                return;
            }
            processedMessageIds.add(msg.getMessageId());
        }

        switch (msg.getType()) {
            case SYSTEM       -> addSystemMessage(msg);
            case PRIVATE      -> addPrivateMessage(msg);
            case PRIVATE_AUDIO -> handleIncomingPrivateAudio(msg);
            case PRIVATE_IMAGE -> handleIncomingPrivateImage(msg);
            case PRIVATE_FILE -> handleIncomingPrivateFile(msg);
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
            case VOICE_FRAME        -> Platform.runLater(() -> { 
                if (currentVoiceCall != null) currentVoiceCall.playRemoteAudio(msg.getBinaryData()); 
                else org.example.tpchatjavafx.client.video.VideoCallController.receiveAudio(msg.getBinaryData());
            });
            default -> {}
        }
    }

    private void addSystemMessage(ChatMessage msg) {
        String key = "SYSTEM";
        privateConversations.putIfAbsent(key, FXCollections.observableArrayList());
        privateConversations.get(key).add(new UiMessage(UiMessage.Kind.TEXT, false, "[SYSTEM] " + msg.getContent(), null));

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
        
        UiMessage uiMsg = new UiMessage(UiMessage.Kind.TEXT, own, msg.getContent(), null);
        privateConversations.get(other).add(uiMsg);

        if (other.equals(currentPrivateTarget)) {
            messagesListView.setItems(privateConversations.get(other));
        }
        if (!privateListView.getItems().contains(other)) {
            privateListView.getItems().add(other);
        }
    }

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
                networkClient.send(new ChatMessage(MessageType.VIDEO_CALL_ACCEPT, username, msg.getFrom(), null, ""));
                startVideoWindow(msg.getFrom(), false);
            } else {
                networkClient.send(new ChatMessage(MessageType.VIDEO_CALL_REJECT, username, msg.getFrom(), null, ""));
            }
        });
    }

    private void startVideoWindow(String otherUser, boolean caller) {
        VideoCallWindow.open(networkClient, username, otherUser, caller);
    }

    private void endVideo() {
        VideoCallWindow.closeCurrent();
    }

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
                networkClient.send(new ChatMessage(MessageType.VOICE_CALL_ACCEPT, username, msg.getFrom(), null, ""));
                startVoiceSession(msg.getFrom());
            } else {
                networkClient.send(new ChatMessage(MessageType.VOICE_CALL_REJECT, username, msg.getFrom(), null, ""));
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

            VoiceCallWindow.open(username, otherUser, () -> {
                if (voiceCallPeer != null) {
                    networkClient.send(new ChatMessage(MessageType.VOICE_CALL_END, username, voiceCallPeer, null, ""));
                }
                endVoiceCall();
            });
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
    }

    private void showInfo(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg);
        a.show();
    }

    @FXML
    private void onSend() {
        String text = messageField.getText().trim();
        if (text.isEmpty()) return;

        if (currentPrivateTarget != null) {
            ChatMessage msg = new ChatMessage(MessageType.PRIVATE, username, currentPrivateTarget, null, text);
            addPrivateMessage(msg);
            networkClient.send(msg);
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
        boolean isImage = name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".gif");

        try {
            byte[] data = Files.readAllBytes(file.toPath());

            if (currentPrivateTarget != null) {
                if (isImage) {
                    String tempPath = saveTempFile("img-", name.substring(name.lastIndexOf('.') + 1), data);
                    ChatMessage msg = new ChatMessage(MessageType.PRIVATE_IMAGE, username, currentPrivateTarget, null, file.getName());
                    msg.setBinaryData(data);
                    addLocalImageMessage(true, tempPath);
                    networkClient.send(msg);
                } else {
                    ChatMessage msg = new ChatMessage(MessageType.PRIVATE_FILE, username, currentPrivateTarget, null, file.getName());
                    msg.setBinaryData(data);
                    addLocalFileMessage(true, file.getName(), file.getAbsolutePath());
                    networkClient.send(msg);
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
        if (emojiPicker.isShowing()) emojiPicker.hide();
        else emojiPicker.show(emojiButton, Side.TOP, 0, 0);
    }

    @FXML
    private void onStartVideoCall() {
        if (currentPrivateTarget == null) {
            showInfo("Select a private contact first.");
            return;
        }
        networkClient.send(new ChatMessage(MessageType.VIDEO_CALL_REQUEST, username, currentPrivateTarget, null, "call_request"));
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
        networkClient.send(new ChatMessage(MessageType.VOICE_CALL_REQUEST, username, currentPrivateTarget, null, "voice_call_request"));
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

    private Window getWindow() {
        return messageField != null && messageField.getScene() != null ? messageField.getScene().getWindow() : null;
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
                if (count > 0) out.write(buffer, 0, count);
            }
            byte[] data = out.toByteArray();
            Platform.runLater(() -> handleRecordedAudio(format, data));
        } catch (Exception e) {
            Platform.runLater(() -> showInfo("Erreur pendant l'enregistrement audio."));
        } finally {
            try { out.close(); } catch (IOException ignored) {}
        }
    }

    private void handleRecordedAudio(AudioFormat format, byte[] audioData) {
        if (audioData == null || audioData.length == 0) return;
        try {
            Path tempFile = Files.createTempFile("audio-message-", ".wav");
            try (AudioInputStream stream = new AudioInputStream(new ByteArrayInputStream(audioData), format, audioData.length / format.getFrameSize())) {
                AudioSystem.write(stream, AudioFileFormat.Type.WAVE, tempFile.toFile());
            }

            if (currentPrivateTarget != null) {
                ChatMessage msg = new ChatMessage(MessageType.PRIVATE_AUDIO, username, currentPrivateTarget, null, "Audio");
                msg.setBinaryData(Files.readAllBytes(tempFile));
                addLocalAudioMessage(true, tempFile.toString());
                networkClient.send(msg);
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
                        try { imageView.setImage(new Image(new File(item.getFilePath()).toURI().toString(), 200, 0, true, true)); } catch (Exception ignored) {}
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
        boolean inPrivate = currentPrivateTarget != null;
        if (voiceCallButton != null) {
            voiceCallButton.setVisible(inPrivate);
            voiceCallButton.setManaged(inPrivate);
        }
        if (videoCallButton != null) {
            videoCallButton.setVisible(inPrivate);
            videoCallButton.setManaged(inPrivate);
        }
    }

    private String saveTempFile(String prefix, String extension, byte[] data) throws IOException {
        Path tmp = Files.createTempFile(prefix, "." + extension);
        try (FileOutputStream fos = new FileOutputStream(tmp.toFile())) { fos.write(data); }
        return tmp.toString();
    }

    private void addLocalAudioMessage(boolean own, String path) {
        if (currentPrivateTarget == null) return;
        privateConversations.putIfAbsent(currentPrivateTarget, FXCollections.observableArrayList());
        privateConversations.get(currentPrivateTarget).add(new UiMessage(UiMessage.Kind.AUDIO, own, "Audio", path));
        messagesListView.setItems(privateConversations.get(currentPrivateTarget));
    }

    private void handleIncomingPrivateAudio(ChatMessage msg) {
        if (msg.getBinaryData() == null) return;
        try {
            String path = saveTempFile("audio-in-", "wav", msg.getBinaryData());
            String other = msg.getFrom().equals(username) ? msg.getTo() : msg.getFrom();
            privateConversations.putIfAbsent(other, FXCollections.observableArrayList());
            privateConversations.get(other).add(new UiMessage(UiMessage.Kind.AUDIO, msg.getFrom().equals(username), "Audio", path));
            if (other.equals(currentPrivateTarget)) messagesListView.setItems(privateConversations.get(other));
            if (!privateListView.getItems().contains(other)) privateListView.getItems().add(other);
        } catch (IOException e) { showInfo("Impossible de sauvegarder l'audio reçu."); }
    }

    private void addLocalImageMessage(boolean own, String path) {
        if (currentPrivateTarget == null) return;
        privateConversations.putIfAbsent(currentPrivateTarget, FXCollections.observableArrayList());
        privateConversations.get(currentPrivateTarget).add(new UiMessage(UiMessage.Kind.IMAGE, own, "", path));
        messagesListView.setItems(privateConversations.get(currentPrivateTarget));
    }

    private void handleIncomingPrivateImage(ChatMessage msg) {
        if (msg.getBinaryData() == null) return;
        try {
            String ext = "png";
            String name = msg.getContent();
            if (name != null && name.contains(".")) ext = name.substring(name.lastIndexOf('.') + 1);
            String path = saveTempFile("img-in-", ext, msg.getBinaryData());
            String other = msg.getFrom().equals(username) ? msg.getTo() : msg.getFrom();
            privateConversations.putIfAbsent(other, FXCollections.observableArrayList());
            privateConversations.get(other).add(new UiMessage(UiMessage.Kind.IMAGE, msg.getFrom().equals(username), "", path));
            if (other.equals(currentPrivateTarget)) messagesListView.setItems(privateConversations.get(other));
            if (!privateListView.getItems().contains(other)) privateListView.getItems().add(other);
        } catch (IOException e) { showInfo("Impossible de sauvegarder l'image reçue."); }
    }

    private void handleIncomingPrivateFile(ChatMessage msg) {
        if (msg.getBinaryData() == null) return;
        try {
            String ext = "bin";
            String name = msg.getContent();
            if (name != null && name.contains(".")) ext = name.substring(name.lastIndexOf('.') + 1);
            String path = saveTempFile("file-in-", ext, msg.getBinaryData());
            String other = msg.getFrom().equals(username) ? msg.getTo() : msg.getFrom();
            privateConversations.putIfAbsent(other, FXCollections.observableArrayList());
            privateConversations.get(other).add(new UiMessage(UiMessage.Kind.FILE, msg.getFrom().equals(username), name != null ? name : "Fichier", path));
            if (other.equals(currentPrivateTarget)) messagesListView.setItems(privateConversations.get(other));
            if (!privateListView.getItems().contains(other)) privateListView.getItems().add(other);
        } catch (IOException e) { showInfo("Impossible de sauvegarder le fichier reçu."); }
    }

    private void playAudio(String path) {
        if (path == null) return;
        new Thread(() -> {
            try (AudioInputStream ais = AudioSystem.getAudioInputStream(new File(path))) {
                Clip clip = AudioSystem.getClip();
                clip.open(ais);
                clip.start();
            } catch (Exception e) { Platform.runLater(() -> showInfo("Impossible de lire l'audio.")); }
        }).start();
    }

    private void addLocalFileMessage(boolean own, String fileName, String path) {
        if (currentPrivateTarget == null) return;
        privateConversations.putIfAbsent(currentPrivateTarget, FXCollections.observableArrayList());
        privateConversations.get(currentPrivateTarget).add(new UiMessage(UiMessage.Kind.FILE, own, fileName, path));
        messagesListView.setItems(privateConversations.get(currentPrivateTarget));
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
        } catch (IOException e) { showInfo("Erreur lors de la sauvegarde : " + e.getMessage()); }
    }
}
