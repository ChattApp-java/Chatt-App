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
import javafx.scene.Node;
import javafx.scene.layout.BorderPane;
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
import org.example.tpchatjavafx.client.audio.AudioCaptureService;
import org.example.tpchatjavafx.client.audio.AudioPlaybackService;
import org.example.tpchatjavafx.client.audio.AudioTransmissionService;
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
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class MainChatController {

    @FXML private BorderPane rootPane;
    @FXML private VBox       privateChatPane;
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
    private final java.util.Map<String, Integer> unreadCounts = new java.util.HashMap<>();
    private final java.time.format.DateTimeFormatter timeFormatter = java.time.format.DateTimeFormatter.ofPattern("HH:mm");
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
            "ðŸ˜€","ðŸ˜","ðŸ˜‚","ðŸ¤£","ðŸ˜Š","ðŸ˜","ðŸ˜˜","ðŸ˜Ž","ðŸ¤©","ðŸ˜‡",
            "ðŸ˜…","ðŸ˜¢","ðŸ˜­","ðŸ¤”","ðŸ˜´","ðŸ˜¡","ðŸ‘","ðŸ™","ðŸ‘","ðŸ”¥",
            "ðŸ¥³","ðŸ¤—","ðŸ’¡","âœ…","â¤ï¸","ðŸ’¬","ðŸŽ‰","âš¡","ðŸ€","â˜•"
    );

    private volatile boolean recordingAudio = false;
    private TargetDataLine targetDataLine;
    private Thread recordingThread;

    private VoiceCallSession currentVoiceCall;
    private String voiceCallPeer = null;

    // ===== SERVICES AUDIO =====
    private AudioCaptureService audioCapture;
    private AudioPlaybackService audioPlayback;
    private AudioTransmissionService audioTransmission;
    private String currentCallType = null; // "AUDIO" ou "VIDEO"
    private String remoteHost = null;
    private int remotePort = 0;

    private Consumer<ChatMessage> onIncomingCall;
    private Consumer<ChatMessage> onCallAnswered;
    private Consumer<ChatMessage> onCallRejected;
    private AudioTransmissionService audioService;
    private String incomingCallFrom = null;
    private GroupController groupController;
    private Tab groupTab;

    @FXML
    private void initialize() {
        buildEmojiPicker();
        updateRecordButtonState();
        setupMessageBubbles();
        setupContactCellFactory();
// Ajouter l'onglet Groupes
        groupController = new GroupController();
        groupTab = new Tab("Groupes");
        groupTab.setClosable(false);
        groupTab.setContent(buildGroupSidebarHint());
        if (tabPane != null) {
            tabPane.getTabs().add(groupTab);
            tabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
                if (newTab == groupTab) {
                    showGroupCenter();
                } else if (newTab == privateTab) {
                    showPrivateCenter();
                }
            });
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

        // ===== CALLBACKS RÃ‰SEAU =====
        networkClient.setOnIncomingCall(msg -> {
            handleIncomingCall(msg);
        });

        networkClient.setOnCallAnswered(this::handleCallAccepted);

        networkClient.setOnCallRejected(msg -> {
            handleCallRejected(msg);
        });

        networkClient.setOnMeetingStarted(msg -> {
            Platform.runLater(() -> {
                try {
                    MeetingController.openMeetingWindow(networkClient, msg.getMeetingId(), "Ma Réunion (" + msg.getMeetingType() + ")");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        });

        networkClient.setOnMeetingInvite(msg -> {
            Platform.runLater(() -> {
                try {
                    IncomingMeetingDialogController.showInvite(msg.getFrom(), msg.getMeetingType(), accepted -> {
                        if (accepted) {
                            networkClient.joinMeeting(msg.getMeetingId());
                            try {
                                MeetingController.openMeetingWindow(networkClient, msg.getMeetingId(), "RÃ©union de " + msg.getFrom());
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        });

        networkClient.setOnMeetingInfo(msg -> {
            try {
                // Le serveur envoie les infos UDP pour se connecter au relais
                networkClient.startMeetingAudio(msg.getMeetingId(), 0, msg.getServerUdpAudioPort(), msg.getServerHost());
                networkClient.startMeetingVideo(msg.getMeetingId(), 0, msg.getServerUdpVideoPort(), msg.getServerHost());
            } catch (Exception e) {
                System.err.println("Erreur dÃ©marrage services UDP rÃ©union : " + e.getMessage());
            }
        });

        privateListView.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            if (n != null) openPrivateChat(n);
        });

        setupSearchContactAutoCompletion();

        // Request current online users and contact list
        networkClient.requestUserList();
        networkClient.requestContacts();
        groupController.init(networkClient, username, allContacts);
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

    private Node buildGroupSidebarHint() {
        Label title = new Label("Groupes");
        title.getStyleClass().add("group-tab-hint-title");

        Label subtitle = new Label("La liste des groupes et le chat s'affichent a droite.");
        subtitle.setWrapText(true);
        subtitle.getStyleClass().add("group-tab-hint-text");

        VBox box = new VBox(8, title, subtitle);
        box.getStyleClass().add("group-tab-hint");
        box.setAlignment(Pos.CENTER);
        return box;
    }

    private void showGroupCenter() {
        if (rootPane != null && groupController != null) {
            rootPane.setCenter(groupController);
        }
    }

    private void showPrivateCenter() {
        if (rootPane != null && privateChatPane != null) {
            rootPane.setCenter(privateChatPane);
        }
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
            dialog.setHeaderText("Entrez le nom d'utilisateur du contact Ã  ajouter");
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
            showInfo("Vous ne pouvez pas vous ajouter vous-mÃªme.");
            return;
        }

        // Request server to add contact
        networkClient.addContact(contactName);
        searchContactField.clear();
    }

    @FXML
    private void onSend() {
        if (currentPrivateTarget == null) {
            showInfo("Sélectionnez un contact pour envoyer un message.");
            return;
        }
        String text = messageField.getText().trim();
        if (text.isEmpty()) return;

        ChatMessage msg = new ChatMessage(MessageType.PRIVATE, username, currentPrivateTarget, currentConversationId, text);
        networkClient.send(msg);
        addPrivateMessage(msg);
        messageField.clear();
    }

    @FXML
    private void onVoiceCall() {
        if (currentPrivateTarget == null) {
            showAlert("SÃ©lectionnez un contact d'abord");
            return;
        }

        // VÃ©rifier que le contact est en ligne
        if (!userStatuses.getOrDefault(currentPrivateTarget, "NON_CONNECTE").equals("EN_LIGNE")) {
            showAlert("Utilisateur hors ligne");
            return;
        }

        // Envoyer demande d'appel
        ChatMessage callRequest = new ChatMessage();
        callRequest.setType("CALL_REQUEST");
        callRequest.setFrom(username);
        callRequest.setTo(currentPrivateTarget);
        callRequest.setCallType("AUDIO");

        networkClient.send(callRequest);

        // UI: afficher Ã©tat "Appel en cours..."
        showCallPending(currentPrivateTarget, "Appel vocal en cours...");
    }

    @FXML
    private void onVideoCall() {
        // Similaire Ã  voice call mais avec callType = "VIDEO"
        if (currentPrivateTarget == null) {
            showAlert("SÃ©lectionnez un contact d'abord");
            return;
        }

        ChatMessage callRequest = new ChatMessage();
        callRequest.setType("CALL_REQUEST");
        callRequest.setFrom(username);
        callRequest.setTo(currentPrivateTarget);
        callRequest.setCallType("VIDEO");

        networkClient.send(callRequest);
        showCallPending(currentPrivateTarget, "Appel vidÃ©o en cours...");
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

                avatar.setStyle("-fx-background-color:" + pickAvatarColor(user) + "; -fx-background-radius:50%; -fx-min-width:42px; -fx-min-height:42px; -fx-max-width:42px; -fx-max-height:42px;");

                Label name = new Label(user);
                name.getStyleClass().add("chat-contact-name");

                String statut = userStatuses.getOrDefault(user, "NON_CONNECTE");
                Label sub = new Label(statut.equals("EN_LIGNE") ? "â— En ligne" : "â— Non connectÃ©");
                sub.getStyleClass().add("chat-contact-status");
                if (statut.equals("EN_LIGNE")) sub.setStyle("-fx-text-fill: #25D366;");

                VBox info = new VBox(3, name, sub);
                HBox row = new HBox(12, avatar, info);
                row.setAlignment(Pos.CENTER_LEFT);
                HBox.setHgrow(info, javafx.scene.layout.Priority.ALWAYS);

                // Unread Badge
                int unread = unreadCounts.getOrDefault(user, 0);
                if (unread > 0) {
                    Label badgeText = new Label(String.valueOf(unread));
                    badgeText.getStyleClass().add("unread-badge-text");
                    StackPane badge = new StackPane(badgeText);
                    badge.getStyleClass().add("unread-badge");
                    row.getChildren().add(badge);
                }

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

    private String pickAvatarColor(String user) {
        return switch (Math.abs(user.hashCode()) % 5) {
            case 0 -> "#00A884";
            case 1 -> "#2196F3";
            case 2 -> "#9C27B0";
            case 3 -> "#FF5722";
            default -> "#FF9800";
        };
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

        // Reset unread count
        unreadCounts.put(other, 0);
        privateListView.refresh();
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

            String time = msg.getTimestamp();
            if (time == null || time.isEmpty()) time = java.time.LocalDateTime.now().format(timeFormatter);

            UiMessage uiMsg = null;
            String typeStr = msg.getConversationId();
            if (typeStr == null || typeStr.isEmpty()) typeStr = "TEXTE";

            if (typeStr.contains("AUDIO") || typeStr.contains("IMAGE") || typeStr.contains("FILE")) {
                UiMessage.Kind kind = typeStr.contains("IMAGE") ? UiMessage.Kind.IMAGE :
                        typeStr.contains("AUDIO") ? UiMessage.Kind.AUDIO : UiMessage.Kind.FILE;

                String localPath = null;
                if (msg.getBinaryData() != null) {
                    try {
                        java.io.File temp = java.io.File.createTempFile("chat_", "_" + msg.getContent());
                        try (java.io.FileOutputStream fos = new java.io.FileOutputStream(temp)) {
                            fos.write(msg.getBinaryData());
                        }
                        localPath = temp.getAbsolutePath();
                    } catch (java.io.IOException e) {
                        System.err.println("Erreur sauvegarde mÃ©dia historique: " + e.getMessage());
                    }
                }
                uiMsg = new UiMessage(kind, own, msg.getContent(), localPath, time);
            } else {
                uiMsg = new UiMessage(UiMessage.Kind.TEXT, own, msg.getContent(), null, time);
            }

            if (uiMsg != null) {
                uiMsgs.add(uiMsg);
            }

            // Auto-scroll if it's the current view
            if (other.equals(currentPrivateTarget)) {
                messagesListView.scrollTo(uiMsgs.size() - 1);
            }
        });
    }

    private void updateChatHeaderStatus(String status) {
        if (chatStatusLabel != null) {
            chatStatusLabel.setText(status.equals("EN_LIGNE") ? "â— En ligne" : "â— Non connectÃ©");
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
            // Appel vidÃ©o
            case VIDEO_CALL_REQUEST -> Platform.runLater(() -> handleCallRequest(msg));
            case VIDEO_CALL_ACCEPT  -> Platform.runLater(() -> startVideoWindow(msg.getFrom(), true));
            case VIDEO_CALL_REJECT  -> Platform.runLater(() -> showInfo("Appel refusÃ© par " + msg.getFrom()));
            case VIDEO_CALL_END     -> Platform.runLater(this::endVideo);
            case VIDEO_FRAME        -> Platform.runLater(() -> VideoCallController.receiveFrame(msg.getBinaryData()));
            // Appel vocal
            case VOICE_CALL_REQUEST -> Platform.runLater(() -> handleVoiceCallRequest(msg));
            case VOICE_CALL_ACCEPT  -> Platform.runLater(() -> startVoiceSession(msg.getFrom()));
            case CALL_INCOMING      -> Platform.runLater(() -> handleIncomingCall(msg));
            case CALL_ANSWER        -> Platform.runLater(() -> handleCallAccepted(msg));
            case CALL_REJECT        -> Platform.runLater(() -> handleCallRejected(msg));
            case CALL_INFO          -> Platform.runLater(() -> handleCallInfo(msg));
            case VOICE_CALL_REJECT  -> Platform.runLater(() -> showInfo("Appel vocal refusÃ© par " + msg.getFrom()));
            case VOICE_CALL_END     -> Platform.runLater(this::endVoiceCall);
            case VOICE_FRAME        -> Platform.runLater(() -> {
                if (currentVoiceCall != null) currentVoiceCall.playRemoteAudio(msg.getBinaryData());
                else org.example.tpchatjavafx.client.video.VideoCallController.receiveAudio(msg.getBinaryData());
            });
            default -> {}
        }

        // Increment unread count if not in current chat
        if (!msg.getType().name().contains("CALL") && msg.getType() != MessageType.SYSTEM) {
            String sender = msg.getFrom();
            if (!sender.equals(username) && !sender.equals(currentPrivateTarget)) {
                unreadCounts.put(sender, unreadCounts.getOrDefault(sender, 0) + 1);
                Platform.runLater(() -> privateListView.refresh());
            }
        }
    }

    private void addSystemMessage(ChatMessage msg) {
        String key = "SYSTEM";
        privateConversations.putIfAbsent(key, FXCollections.observableArrayList());
        privateConversations.get(key).add(new UiMessage(UiMessage.Kind.TEXT, false, "[SYSTEM] " + msg.getContent(), null, java.time.LocalDateTime.now().format(timeFormatter)));

        if (key.equals(currentPrivateTarget)) {
            messagesListView.setItems(privateConversations.get(key));
        }
        if (!privateListView.getItems().contains(key)) {
            privateListView.getItems().add(key);
        }
    }

    private void addPrivateMessage(ChatMessage msg) {
        String other = msg.getFrom().equals(username) ? msg.getTo() : msg.getFrom();
        boolean isOwn = msg.getFrom().equals(username);

        Platform.runLater(() -> {
            ObservableList<UiMessage> msgs = privateConversations.computeIfAbsent(other, k -> FXCollections.observableArrayList());

            String time = msg.getTimestamp();
            if (time == null || time.isEmpty()) time = java.time.LocalDateTime.now().format(timeFormatter);

            UiMessage uiMsg = new UiMessage(UiMessage.Kind.TEXT, isOwn, msg.getContent(), null, time);
            msgs.add(uiMsg);

            if (other.equals(currentPrivateTarget)) {
                messagesListView.scrollTo(msgs.size() - 1);
            }
            if (!privateListView.getItems().contains(other)) {
                privateListView.getItems().add(other);
            }
            privateListView.refresh();
        });
    }

    private void handleCallRequest(ChatMessage msg) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("WeChat - Appel VidÃ©o");
        alert.setHeaderText("Appel vidÃ©o entrant de " + msg.getFrom());
        alert.setContentText("Souhaitez-vous accepter l'appel ?");

        ButtonType accept = new ButtonType("Accepter", ButtonBar.ButtonData.OK_DONE);
        ButtonType reject = new ButtonType("Refuser", ButtonBar.ButtonData.CANCEL_CLOSE);
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
        alert.setTitle("WeChat - Appel Vocal");
        alert.setHeaderText("Appel vocal entrant de " + msg.getFrom());
        alert.setContentText("Souhaitez-vous accepter l'appel ?");

        ButtonType accept = new ButtonType("Accepter", ButtonBar.ButtonData.OK_DONE);
        ButtonType reject = new ButtonType("Refuser", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(accept, reject);

        alert.showAndWait().ifPresent(result -> {
            if (result == accept) {
                // Utiliser les nouveaux types de messages
                ChatMessage answerMsg = new ChatMessage(MessageType.CALL_ANSWER, username, msg.getFrom(), null, "Appel acceptÃ©");
                answerMsg.setCallType("AUDIO");
                networkClient.send(answerMsg);
                // Attendre CALL_INFO avant de dÃ©marrer la session
            } else {
                ChatMessage rejectMsg = new ChatMessage(MessageType.CALL_REJECT, username, msg.getFrom(), null, "Appel refusÃ©");
                networkClient.send(rejectMsg);
            }
        });
    }

    // ===== NOUVELLES MÃ‰THODES POUR LES APPELS =====

    private void handleIncomingCall(ChatMessage msg) {
        if (currentCallType != null) {
            // Refuser automatiquement si dÃ©jÃ  en appel
            ChatMessage rejectMsg = new ChatMessage(MessageType.CALL_REJECT, username, msg.getFrom(), null, "OccupÃ©");
            networkClient.send(rejectMsg);
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("WeChat - Appel " + msg.getCallType());
        alert.setHeaderText("Appel " + msg.getCallType().toLowerCase() + " entrant de " + msg.getFrom());
        alert.setContentText("Souhaitez-vous accepter l'appel ?");

        ButtonType accept = new ButtonType("Accepter", ButtonBar.ButtonData.OK_DONE);
        ButtonType reject = new ButtonType("Refuser", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(accept, reject);

        alert.showAndWait().ifPresent(result -> {
            if (result == accept) {
                ChatMessage answerMsg = new ChatMessage(MessageType.CALL_ANSWER, username, msg.getFrom(), null, "Appel acceptÃ©");
                answerMsg.setCallType(msg.getCallType());
                networkClient.send(answerMsg);
            } else {
                ChatMessage rejectMsg = new ChatMessage(MessageType.CALL_REJECT, username, msg.getFrom(), null, "Appel refusÃ©");
                networkClient.send(rejectMsg);
            }
        });
    }

    private void handleCallAccepted(ChatMessage msg) {
        // L'appel a Ã©tÃ© acceptÃ©, dÃ©marrer la session audio
        startAudioCall(msg.getFrom(), "AUDIO");
    }

    private void handleCallRejected(ChatMessage msg) {
        showInfo("Appel refusÃ© par " + msg.getFrom());
    }

    private void handleCallInfo(ChatMessage msg) {
        // Recevoir les infos P2P pour la connexion directe
        remoteHost = msg.getRemoteHost();
        remotePort = msg.getRemotePort();
        currentCallType = msg.getCallType();

        // DÃ©marrer la transmission audio P2P
        if ("AUDIO".equals(currentCallType) && remoteHost != null) {
            startAudioTransmission(msg.getFrom());
        }
    }

    private void startVoiceSession(String otherUser) {
        // Ancienne mÃ©thode - maintenant dÃ©lÃ©guÃ©e Ã  startAudioCall
        startAudioCall(otherUser, "AUDIO");
    }

    private void startAudioCall(String otherUser, String callType) {
        if (currentCallType != null) {
            showInfo("Un appel est dÃ©jÃ  en cours.");
            return;
        }

        try {
            currentCallType = callType;
            voiceCallPeer = otherUser;

            // Initialiser les services audio
            audioCapture = new AudioCaptureService();
            audioPlayback = new AudioPlaybackService();

            // Ouvrir la fenÃªtre d'appel
            VoiceCallWindow.open(username, otherUser, () -> {
                endAudioCall();
            });

            showInfo("Appel " + callType.toLowerCase() + " dÃ©marrÃ© avec " + otherUser);

        } catch (Exception e) {
            showInfo("Impossible de dÃ©marrer l'appel: " + e.getMessage());
            endAudioCall();
        }
    }

    private void startAudioTransmission(String otherUser) {
        if (audioTransmission != null) {
            audioTransmission.stop();
        }

        try {
            // DÃ©marrer la transmission P2P
            audioTransmission = new AudioTransmissionService();
            audioTransmission.initiate(remoteHost, remotePort);

            showInfo("Connexion audio Ã©tablie avec " + otherUser);

        } catch (Exception e) {
            showInfo("Erreur de connexion audio: " + e.getMessage());
            endAudioCall();
        }
    }

    private void endVoiceCall() {
        // Ancienne mÃ©thode - dÃ©lÃ©guer Ã  endAudioCall
        endAudioCall();
    }


    private void endAudioCall() {
        if (audioTransmission != null) {
            audioTransmission.stop();
            audioTransmission = null;
        }
        if (audioService != null) {
            audioService.stop();
            audioService = null;
        }
        if (audioCapture != null) {
            audioCapture.stop();
            audioCapture = null;
        }
        if (audioPlayback != null) {
            audioPlayback.stop();
            audioPlayback = null;
        }
        if (voiceCallPeer != null && networkClient != null) {
            ChatMessage endMsg = new ChatMessage(MessageType.CALL_END, username, voiceCallPeer, null, "Appel termine");
            networkClient.send(endMsg);
        }
        currentCallType = null;
        voiceCallPeer = null;
        incomingCallFrom = null;
    }

    private void showInfo(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, msg);
        alert.show();
    }

    private void showAlert(String msg) {
        showInfo(msg);
    }

    private void showCallPending(String user, String message) {
        if (chatStatusLabel != null) {
            chatStatusLabel.setText(message);
        }
        showInfo(message);
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
            showInfo("SÃ©lectionnez un contact privÃ© d'abord.");
            return;
        }
        if (currentCallType != null) {
            showInfo("Un appel est dÃ©jÃ  en cours.");
            return;
        }

        // Envoyer une demande d'appel audio
        ChatMessage callRequest = new ChatMessage(MessageType.CALL_REQUEST, username, currentPrivateTarget, null, "Demande d'appel audio");
        callRequest.setCallType("AUDIO");
        networkClient.send(callRequest);

        showInfo("Appel audio demandÃ© Ã  " + currentPrivateTarget);
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
            showInfo("L'enregistrement audio n'est pas supportÃ© sur cet appareil.");
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
            showInfo("Impossible d'accÃ©der au micro: " + e.getMessage());
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
            recordAudioButton.setText("â– ");
            recordAudioButton.setStyle("-fx-text-fill: #f87171;");
        } else {
            recordAudioButton.setText("ðŸŽ™");
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

                        Label time = new Label(item.getTimestamp());
                        time.getStyleClass().add(isOwn ? "timestamp-sent" : "timestamp-received");

                        VBox content = new VBox(2, bubble, time);
                        content.setAlignment(isOwn ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
                        row.getChildren().add(content);
                    }
                    case IMAGE -> {
                        ImageView imageView = new ImageView();
                        try { imageView.setImage(new Image(new File(item.getFilePath()).toURI().toString(), 200, 0, true, true)); } catch (Exception ignored) {}
                        imageView.getStyleClass().add(isOwn ? "bubble-sent" : "bubble-received");

                        Label time = new Label(item.getTimestamp());
                        time.getStyleClass().add(isOwn ? "timestamp-sent" : "timestamp-received");

                        VBox content = new VBox(2, imageView, time);
                        content.setAlignment(isOwn ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
                        row.getChildren().add(content);
                    }
                    case AUDIO -> {
                        Button play = new Button("â–¶");
                        play.getStyleClass().add("btn-icon");
                        play.setOnAction(e -> playAudio(item.getFilePath()));
                        Label label = new Label(" Message vocal");
                        label.getStyleClass().add(isOwn ? "bubble-sent" : "bubble-received");

                        Label time = new Label(item.getTimestamp());
                        time.getStyleClass().add(isOwn ? "timestamp-sent" : "timestamp-received");

                        HBox inner = new HBox(6, play, label);
                        VBox content = new VBox(2, inner, time);
                        content.setAlignment(isOwn ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
                        row.getChildren().add(content);
                    }
                    case FILE -> {
                        Label nameLabel = new Label("ðŸ“Ž " + item.getText());
                        nameLabel.getStyleClass().add(isOwn ? "bubble-sent" : "bubble-received");
                        Button downloadBtn = new Button("ðŸ’¾");
                        downloadBtn.getStyleClass().add("btn-icon");
                        downloadBtn.setOnAction(e -> downloadFile(item.getFilePath(), item.getText()));

                        Label time = new Label(item.getTimestamp());
                        time.getStyleClass().add(isOwn ? "timestamp-sent" : "timestamp-received");

                        HBox inner = new HBox(8, nameLabel, downloadBtn);
                        inner.setAlignment(isOwn ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
                        VBox content = new VBox(2, inner, time);
                        content.setAlignment(isOwn ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
                        row.getChildren().add(content);
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
        String time = java.time.LocalDateTime.now().format(timeFormatter);
        privateConversations.get(currentPrivateTarget).add(new UiMessage(UiMessage.Kind.AUDIO, own, "Audio", path, time));
        messagesListView.setItems(privateConversations.get(currentPrivateTarget));
    }

    private void handleIncomingPrivateAudio(ChatMessage msg) {
        if (msg.getBinaryData() == null) return;
        try {
            String path = saveTempFile("audio-in-", "wav", msg.getBinaryData());
            String other = msg.getFrom().equals(username) ? msg.getTo() : msg.getFrom();
            privateConversations.putIfAbsent(other, FXCollections.observableArrayList());
            String time = java.time.LocalDateTime.now().format(timeFormatter);
            privateConversations.get(other).add(new UiMessage(UiMessage.Kind.AUDIO, msg.getFrom().equals(username), "Audio", path, time));
            if (other.equals(currentPrivateTarget)) messagesListView.setItems(privateConversations.get(other));
            if (!privateListView.getItems().contains(other)) privateListView.getItems().add(other);
        } catch (IOException e) { showInfo("Impossible de sauvegarder l'audio reÃ§u."); }
    }

    private void addLocalImageMessage(boolean own, String path) {
        if (currentPrivateTarget == null) return;
        privateConversations.putIfAbsent(currentPrivateTarget, FXCollections.observableArrayList());
        String time = java.time.LocalDateTime.now().format(timeFormatter);
        privateConversations.get(currentPrivateTarget).add(new UiMessage(UiMessage.Kind.IMAGE, own, "", path, time));
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
            String time = java.time.LocalDateTime.now().format(timeFormatter);
            privateConversations.get(other).add(new UiMessage(UiMessage.Kind.IMAGE, msg.getFrom().equals(username), "", path, time));
            if (other.equals(currentPrivateTarget)) messagesListView.setItems(privateConversations.get(other));
            if (!privateListView.getItems().contains(other)) privateListView.getItems().add(other);
        } catch (IOException e) { showInfo("Impossible de sauvegarder l'image reÃ§ue."); }
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
            String time = java.time.LocalDateTime.now().format(timeFormatter);
            privateConversations.get(other).add(new UiMessage(UiMessage.Kind.FILE, msg.getFrom().equals(username), name != null ? name : "Fichier", path, time));
            if (other.equals(currentPrivateTarget)) messagesListView.setItems(privateConversations.get(other));
            if (!privateListView.getItems().contains(other)) privateListView.getItems().add(other);
        } catch (IOException e) { showInfo("Impossible de sauvegarder le fichier reÃ§u."); }
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
        String time = java.time.LocalDateTime.now().format(timeFormatter);
        privateConversations.get(currentPrivateTarget).add(new UiMessage(UiMessage.Kind.FILE, own, fileName, path, time));
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
            showInfo("Fichier sauvegardÃ© : " + dest.getAbsolutePath());
        } catch (IOException e) { showInfo("Erreur lors de la sauvegarde : " + e.getMessage()); }
    }

}
