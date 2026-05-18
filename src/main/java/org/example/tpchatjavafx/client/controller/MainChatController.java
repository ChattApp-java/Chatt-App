package org.example.tpchatjavafx.client.controller;

import javafx.application.Platform;
import org.example.tpchatjavafx.client.ChatClientApp;
import org.example.tpchatjavafx.client.voice.VoiceCallWindow;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
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
    @FXML private Button    infoButton;
    @FXML private TabPane   tabPane;
    @FXML private HBox emojiBar;
    @FXML private FlowPane emojiGrid;
    @FXML private Label usernameLabel;

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
    private static final List<String> EMOJIS = Arrays.asList(
            "😂", "🤣", "😊", "😅", "🥺", "😎", "🤔", "🙄", "😭", "❤️",
            "🔥", "💯", "👍", "✅", "🙏", "💪", "👏", "👌", "❌", "👀",
            "🤝", "🫶", "🧡", "💙", "💔", "💕", "🤍", "👎", "😍", "😘",
            "😔", "😢", "😡"
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
    private Tab groupTab;
    private ListView<String> groupTabListView;
    private Tab contactsTab;
    private ListView<String> contactsTabListView;
    private final List<int[]> groupIds = new ArrayList<>();
    private final ObservableList<String> groupNames = FXCollections.observableArrayList();
    private final Map<Integer, ObservableList<UiMessage>> groupConversations = new HashMap<>();
    private final Map<Integer, String> groupMembersById = new HashMap<>();
    private int currentGroupId = -1;
    private String currentGroupName = null;
    private Alert groupInfoAlert;

    @FXML
    private void initialize() {
        updateRecordButtonState();
        setupMessageBubbles();
        setupContactCellFactory();
        groupTab = new Tab("Groupes");
        groupTab.setClosable(false);
        groupTab.setContent(buildGroupTabContent());
        contactsTab = new Tab("Contacts");
        contactsTab.setClosable(false);
        contactsTab.setContent(buildContactsTabContent());
        if (tabPane != null) {
            tabPane.getTabs().addAll(groupTab, contactsTab);
            tabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
                if (newTab == groupTab) {
                    showPrivateCenter();
                } else if (newTab == privateTab || newTab == contactsTab) {
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
        registerGroupCallbacks();

        // ===== CALLBACKS RÃƒâ€°SEAU =====
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
                    MeetingController.openMeetingWindow(networkClient, msg.getMeetingId(), "Ma RÃ©union (" + msg.getMeetingType() + ")");
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
                                MeetingController.openMeetingWindow(networkClient, msg.getMeetingId(), "RÃƒÂ©union de " + msg.getFrom());
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
                if ("VIDEO".equalsIgnoreCase(msg.getMeetingType())) {
                    networkClient.startMeetingVideo(msg.getMeetingId(), 0, msg.getServerUdpVideoPort(), msg.getServerHost());
                }
            } catch (Exception e) {
                System.err.println("Erreur dÃƒÂ©marrage services UDP rÃƒÂ©union : " + e.getMessage());
            }
        });

        privateListView.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            if (n != null) openPrivateChat(n);
        });

        setupSearchContactAutoCompletion();

        // Request current online users and contact list
        networkClient.requestUserList();
        networkClient.requestContacts();
        networkClient.requestGroupList();
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
            if (contactsTabListView != null) {
                contactsTabListView.setItems(allContacts);
            }
            privateListView.refresh();
            if (contactsTabListView != null) {
                contactsTabListView.refresh();
            }
        });
    }

    private Node buildGroupTabContent() {
        Button createButton = new Button("+ Nouveau groupe");
        createButton.getStyleClass().add("group-create-btn");
        createButton.setMaxWidth(Double.MAX_VALUE);
        createButton.setOnAction(e -> openCreateGroupDialog());

        groupTabListView = new ListView<>(groupNames);
        groupTabListView.getStyleClass().addAll("contact-list", "group-list");
        groupTabListView.setPlaceholder(new Label("Aucun groupe pour le moment"));
        groupTabListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(String group, boolean empty) {
                super.updateItem(group, empty);
                if (empty || group == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }

                Label initial = new Label(group.substring(0, 1).toUpperCase());
                initial.getStyleClass().add("avatar-letter");
                StackPane avatar = new StackPane(initial);
                avatar.getStyleClass().add("avatar-circle");
                avatar.setStyle("-fx-background-color:#38bdf8; -fx-background-radius:12; -fx-min-width:42px; -fx-min-height:42px; -fx-max-width:42px; -fx-max-height:42px;");

                Label name = new Label(group);
                name.getStyleClass().add("chat-contact-name");
                Label sub = new Label("Groupe de discussion");
                sub.getStyleClass().add("chat-contact-status");

                HBox row = new HBox(12, avatar, new VBox(3, name, sub));
                row.setAlignment(Pos.CENTER_LEFT);
                setGraphic(row);
                setText(null);
            }
        });
        groupTabListView.getSelectionModel().selectedIndexProperty().addListener((obs, oldIndex, newIndex) -> {
            int i = newIndex.intValue();
            if (i >= 0 && i < groupIds.size()) {
                openGroupChat(groupIds.get(i)[0], groupNames.get(i));
            }
        });

        Label title = new Label("GROUPES");
        title.getStyleClass().add("section-label");
        VBox content = new VBox(10, createButton, title, groupTabListView);
        content.setPadding(new Insets(10, 14, 0, 14));
        VBox.setVgrow(groupTabListView, javafx.scene.layout.Priority.ALWAYS);
        return content;
    }

    private Node buildContactsTabContent() {
        contactsTabListView = new ListView<>();
        contactsTabListView.setItems(allContacts);
        contactsTabListView.getStyleClass().add("contact-list");
        contactsTabListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(String contact, boolean empty) {
                super.updateItem(contact, empty);
                if (empty || contact == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }

                Label initial = new Label(contact.substring(0, 1).toUpperCase());
                initial.getStyleClass().add("avatar-letter");
                StackPane avatar = new StackPane(initial);
                avatar.getStyleClass().add("avatar-circle");
                avatar.setStyle("-fx-background-color:" + pickAvatarColor(contact) + "; -fx-background-radius:50%; -fx-min-width:42px; -fx-min-height:42px; -fx-max-width:42px; -fx-max-height:42px;");

                Label name = new Label(contact);
                name.getStyleClass().add("chat-contact-name");
                String status = userStatuses.getOrDefault(contact, "NON_CONNECTE");
                Label sub = new Label(status.equals("EN_LIGNE") ? "En ligne" : "Non connecte");
                sub.getStyleClass().add("chat-contact-status");
                if (status.equals("EN_LIGNE")) {
                    sub.setStyle("-fx-text-fill: #22c55e;");
                }

                HBox row = new HBox(12, avatar, new VBox(3, name, sub));
                row.setAlignment(Pos.CENTER_LEFT);
                setGraphic(row);
                setText(null);
            }
        });
        contactsTabListView.getSelectionModel().selectedItemProperty().addListener((obs, oldContact, contact) -> {
            if (contact != null) {
                openPrivateChat(contact);
                if (tabPane != null && privateTab != null) {
                    tabPane.getSelectionModel().select(privateTab);
                }
            }
        });

        Label title = new Label("CONTACTS");
        title.getStyleClass().add("section-label");
        return new VBox(title, contactsTabListView);
    }

    private void showPrivateCenter() {
        if (rootPane != null && privateChatPane != null) {
            rootPane.setCenter(privateChatPane);
        }
    }

    private void registerGroupCallbacks() {
        networkClient.setOnGroupCreated(msg -> Platform.runLater(() -> {
            GroupEntry entry = parseGroupEntry(msg.getContent());
            if (entry != null) {
                addGroup(entry.id(), entry.name());
                openGroupChat(entry.id(), entry.name());
                networkClient.requestGroupList();
            }
        }));

        networkClient.setOnGroupListResponse(msg -> Platform.runLater(() -> {
            groupIds.clear();
            groupNames.clear();
            String raw = msg.getContent();
            if (raw != null && !raw.isBlank()) {
                for (String part : raw.split(",")) {
                    GroupEntry entry = parseGroupEntry(part);
                    if (entry != null) addGroup(entry.id(), entry.name());
                }
            }
            if (groupTabListView != null) groupTabListView.refresh();
        }));

        networkClient.setOnGroupMessage(msg -> Platform.runLater(() -> addGroupMessage(msg, false)));

        networkClient.setOnGroupHistoryResponse(msg -> Platform.runLater(() -> {
            int gid = msg.getGroupId();
            String raw = msg.getContent();
            if ("__BEGIN__".equals(raw)) {
                groupConversations.put(gid, FXCollections.observableArrayList());
                if (gid == currentGroupId) messagesListView.setItems(groupConversations.get(gid));
                return;
            }
            if ("__EMPTY__".equals(raw) || raw == null || raw.isBlank()) {
                groupConversations.putIfAbsent(gid, FXCollections.observableArrayList());
                if (gid == currentGroupId) messagesListView.setItems(groupConversations.get(gid));
                return;
            }
            if (raw.contains(";;;")) {
                groupConversations.put(gid, FXCollections.observableArrayList());
                for (String entry : raw.split(";;;")) {
                    String[] parts = entry.split(":::", 4);
                    if (parts.length >= 2) {
                        ChatMessage historyMsg = new ChatMessage();
                        historyMsg.setType(parts.length >= 4 ? safeMessageType(parts[3]) : MessageType.GROUP_MESSAGE);
                        historyMsg.setGroupId(gid);
                        historyMsg.setFrom(parts[0]);
                        historyMsg.setContent(parts[1]);
                        if (parts.length >= 3) historyMsg.setTimestamp(parts[2]);
                        addGroupMessage(historyMsg, true);
                    }
                }
                return;
            }
            addGroupMessage(msg, true);
        }));

        networkClient.setOnGroupMemberAdded(msg -> Platform.runLater(() -> {
            if (msg.getGroupId() == currentGroupId) {
                groupConversations.computeIfAbsent(currentGroupId, k -> FXCollections.observableArrayList())
                        .add(new UiMessage(UiMessage.Kind.TEXT, false, msg.getContent() + " a rejoint le groupe.", null,
                                java.time.LocalDateTime.now().format(timeFormatter)));
            }
            networkClient.requestGroupList();
        }));

        networkClient.setOnGroupMemberRemoved(msg -> Platform.runLater(() -> {
            if (msg.getGroupId() == currentGroupId) {
                groupConversations.computeIfAbsent(currentGroupId, k -> FXCollections.observableArrayList())
                        .add(new UiMessage(UiMessage.Kind.TEXT, false, msg.getContent() + " a quitte le groupe.", null,
                                java.time.LocalDateTime.now().format(timeFormatter)));
            }
            networkClient.requestGroupList();
        }));

        networkClient.setOnGroupMembersResponse(msg -> Platform.runLater(() -> {
            groupMembersById.put(msg.getGroupId(), msg.getContent());
            if (groupInfoAlert != null && msg.getGroupId() == currentGroupId) {
                groupInfoAlert.setContentText(buildGroupInfoText(msg.getContent()));
            }
        }));
    }

    private void addGroup(int id, String name) {
        for (int[] group : groupIds) {
            if (group[0] == id) return;
        }
        groupIds.add(new int[]{id});
        groupNames.add(name);
    }

    private void openGroupChat(int groupId, String groupName) {
        currentPrivateTarget = null;
        currentGroupId = groupId;
        currentGroupName = groupName;
        chatTitleLabel.setText(groupName);
        chatStatusLabel.setText("Groupe de discussion");
        chatAvatarLabel.setText(groupName.substring(0, 1).toUpperCase());
        voiceCallButton.setVisible(false);
        voiceCallButton.setManaged(false);
        videoCallButton.setVisible(false);
        videoCallButton.setManaged(false);
        if (infoButton != null) {
            infoButton.setVisible(false);
            infoButton.setManaged(false);
        }
        groupConversations.putIfAbsent(groupId, FXCollections.observableArrayList());
        messagesListView.setItems(groupConversations.get(groupId));
        networkClient.requestGroupHistory(groupId);
        networkClient.requestGroupMembers(groupId);
        showPrivateCenter();
    }

    private void addGroupMessage(ChatMessage msg, boolean fromHistory) {
        int gid = msg.getGroupId();
        boolean own = username != null && username.equals(msg.getFrom());
        String text = msg.getContent();
        UiMessage.Kind kind = kindForGroup(msg.getType());
        if (!own && kind == UiMessage.Kind.TEXT && msg.getFrom() != null && !fromHistory) {
            text = msg.getFrom() + ": " + text;
        }
        if (!own && kind == UiMessage.Kind.TEXT && fromHistory && msg.getFrom() != null) {
            text = msg.getFrom() + ": " + text;
        }
        String time = msg.getTimestamp();
        if (time == null || time.isBlank()) time = java.time.LocalDateTime.now().format(timeFormatter);
        String filePath = saveIncomingGroupMedia(kind, msg.getContent(), msg.getBinaryData());
        UiMessage uiMessage = new UiMessage(kind, own, text, filePath, time);
        groupConversations.computeIfAbsent(gid, k -> FXCollections.observableArrayList()).add(uiMessage);
        if (gid == currentGroupId) {
            messagesListView.setItems(groupConversations.get(gid));
            messagesListView.scrollTo(groupConversations.get(gid).size() - 1);
        }
    }

    private UiMessage.Kind kindForGroup(MessageType type) {
        if (type == MessageType.GROUP_AUDIO) return UiMessage.Kind.AUDIO;
        if (type == MessageType.GROUP_IMAGE) return UiMessage.Kind.IMAGE;
        if (type == MessageType.GROUP_FILE) return UiMessage.Kind.FILE;
        return UiMessage.Kind.TEXT;
    }

    private MessageType safeMessageType(String raw) {
        try {
            return MessageType.valueOf(raw);
        } catch (Exception ignored) {
            return MessageType.GROUP_MESSAGE;
        }
    }

    private String saveIncomingGroupMedia(UiMessage.Kind kind, String name, byte[] data) {
        if (kind == UiMessage.Kind.TEXT || data == null || data.length == 0) return null;
        String suffix = kind == UiMessage.Kind.AUDIO ? ".wav" : "_" + (name == null ? "file" : name);
        try {
            return saveTempFile("group-media-", suffix.replaceFirst("^\\.", ""), data);
        } catch (IOException e) {
            return null;
        }
    }

    private GroupEntry parseGroupEntry(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String[] parts = raw.contains("|") ? raw.split("\\|", 2) : raw.split(":", 3);
        if (parts.length < 2) return null;
        try {
            return new GroupEntry(Integer.parseInt(parts[0].trim()), parts[1].trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private record GroupEntry(int id, String name) {}

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
            dialog.setHeaderText("Entrez le nom d'utilisateur du contact ÃƒÂ  ajouter");
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
            showInfo("Vous ne pouvez pas vous ajouter vous-mÃƒÂªme.");
            return;
        }

        // Request server to add contact
        networkClient.addContact(contactName);
        searchContactField.clear();
    }

    private void openCreateGroupDialog() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/create-group-dialog.fxml"));
            VBox content = loader.load();
            CreateGroupDialogController controller = loader.getController();
            controller.init(networkClient, allContacts);

            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("Creer un groupe");
            dialog.getDialogPane().setContent(content);
            dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
            dialog.showAndWait().ifPresent(button -> {
                if (button == ButtonType.OK) {
                    String name = controller.getGroupName();
                    String desc = controller.getGroupDescription();
                    List<String> members = controller.getSelectedMembers();
                    if (!name.isBlank()) {
                        networkClient.createGroup(name, desc, members);
                    }
                }
            });
        } catch (IOException e) {
            showInfo("Impossible de creer le groupe: " + e.getMessage());
        }
    }

    @FXML
    private void onSend() {
        String text = messageField.getText().trim();
        if (text.isEmpty()) return;

        if (currentGroupId != -1) {
            networkClient.sendGroupMessage(currentGroupId, text);
            messageField.clear();
            return;
        }

        if (currentPrivateTarget == null) {
            showInfo("Selectionnez un contact pour envoyer un message.");
            return;
        }

        ChatMessage msg = new ChatMessage(MessageType.PRIVATE, username, currentPrivateTarget, currentConversationId, text);
        networkClient.send(msg);
        addPrivateMessage(msg);
        messageField.clear();
    }

    @FXML
    private void onVoiceCall() {
        if (currentPrivateTarget == null) {
            showAlert("SÃƒÂ©lectionnez un contact d'abord");
            return;
        }

        // VÃƒÂ©rifier que le contact est en ligne
        if (!userStatuses.getOrDefault(currentPrivateTarget, "NON_CONNECTE").equals("EN_LIGNE")) {
            showAlert("Utilisateur hors ligne");
            return;
        }

        networkClient.send(new ChatMessage(MessageType.VOICE_CALL_REQUEST, username, currentPrivateTarget, null, "Demande d'appel audio"));

        // UI: afficher ÃƒÂ©tat "Appel en cours..."
        showCallPending(currentPrivateTarget, "Appel vocal en cours...");
    }

    @FXML
    private void onVideoCall() {
        // Similaire ÃƒÂ  voice call mais avec callType = "VIDEO"
        if (currentPrivateTarget == null) {
            showAlert("SÃƒÂ©lectionnez un contact d'abord");
            return;
        }

        ChatMessage callRequest = new ChatMessage();
        callRequest.setType(MessageType.CALL_REQUEST);
        callRequest.setFrom(username);
        callRequest.setTo(currentPrivateTarget);
        callRequest.setCallType("VIDEO");

        networkClient.send(callRequest);
        showCallPending(currentPrivateTarget, "Appel vidÃƒÂ©o en cours...");
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
                Label sub = new Label(statut.equals("EN_LIGNE") ? "Ã¢â€”Â En ligne" : "Ã¢â€”Â Non connectÃƒÂ©");
                sub.getStyleClass().add("chat-contact-status");
                sub.setText(statut.equals("EN_LIGNE") ? "En ligne" : "Non connecte");
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
            case 0 -> "#0284c7";
            case 1 -> "#0ea5e9";
            case 2 -> "#38bdf8";
            case 3 -> "#0891b2";
            default -> "#0369a1";
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
        currentGroupId = -1;
        currentGroupName = null;
        if (chatTitleLabel  != null) chatTitleLabel.setText(other);
        if (chatAvatarLabel != null) chatAvatarLabel.setText(other.substring(0,1).toUpperCase());
        if (infoButton != null) {
            infoButton.setVisible(true);
            infoButton.setManaged(true);
        }

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
                if (msg.getBinaryData() != null && msg.getBinaryData().length > 0) {
                    try {
                        String name = msg.getContent();
                        String ext = "bin";
                        if (name != null && name.contains(".")) {
                            ext = name.substring(name.lastIndexOf('.') + 1).replaceAll("[^a-zA-Z0-9]", "");
                        }
                        if (ext.isBlank()) ext = "bin";
                        localPath = saveTempFile("history-media-", ext, msg.getBinaryData());
                    } catch (java.io.IOException e) {
                        System.err.println("Erreur sauvegarde mÃƒÂ©dia historique: " + e.getMessage());
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
            chatStatusLabel.setText(status.equals("EN_LIGNE") ? "Ã¢â€”Â En ligne" : "Ã¢â€”Â Non connectÃƒÂ©");
            chatStatusLabel.setText(status.equals("EN_LIGNE") ? "En ligne" : "Non connecte");
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
            // Appel vidÃƒÂ©o
            case VIDEO_CALL_REQUEST -> Platform.runLater(() -> handleCallRequest(msg));
            case VIDEO_CALL_ACCEPT  -> Platform.runLater(() -> startVideoWindow(msg.getFrom(), true));
            case VIDEO_CALL_REJECT  -> Platform.runLater(() -> showInfo("Appel refusÃƒÂ© par " + msg.getFrom()));
            case VIDEO_CALL_END     -> Platform.runLater(this::endVideo);
            case VIDEO_FRAME        -> Platform.runLater(() -> VideoCallController.receiveFrame(msg.getBinaryData()));
            // Appel vocal
            case VOICE_CALL_REQUEST -> Platform.runLater(() -> handleVoiceCallRequest(msg));
            case VOICE_CALL_ACCEPT  -> Platform.runLater(() -> startVoiceSession(msg.getFrom()));
            case CALL_INCOMING      -> Platform.runLater(() -> handleIncomingCall(msg));
            case CALL_ANSWER        -> Platform.runLater(() -> handleCallAccepted(msg));
            case CALL_REJECT        -> Platform.runLater(() -> handleCallRejected(msg));
            case CALL_INFO          -> Platform.runLater(() -> handleCallInfo(msg));
            case VOICE_CALL_REJECT  -> Platform.runLater(() -> showInfo("Appel vocal refusÃƒÂ© par " + msg.getFrom()));
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
        alert.setTitle("WeChat - Appel VidÃƒÂ©o");
        alert.setHeaderText("Appel vidÃƒÂ©o entrant de " + msg.getFrom());
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
        if (currentVoiceCall != null) {
            networkClient.send(new ChatMessage(MessageType.VOICE_CALL_REJECT, username, msg.getFrom(), null, "Occupe"));
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("WeChat - Appel Vocal");
        alert.setHeaderText("Appel vocal entrant de " + msg.getFrom());
        alert.setContentText("Souhaitez-vous accepter l'appel ?");

        ButtonType accept = new ButtonType("Accepter", ButtonBar.ButtonData.OK_DONE);
        ButtonType reject = new ButtonType("Refuser", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(accept, reject);

        alert.showAndWait().ifPresent(result -> {
            if (result == accept) {
                networkClient.send(new ChatMessage(MessageType.VOICE_CALL_ACCEPT, username, msg.getFrom(), null, "Appel accepte"));
                startVoiceSession(msg.getFrom());
            } else {
                networkClient.send(new ChatMessage(MessageType.VOICE_CALL_REJECT, username, msg.getFrom(), null, "Appel refuse"));
            }
        });
    }

    // ===== NOUVELLES MÃƒâ€°THODES POUR LES APPELS =====

    private void handleIncomingCall(ChatMessage msg) {
        if (currentCallType != null) {
            // Refuser automatiquement si dÃƒÂ©jÃƒÂ  en appel
            ChatMessage rejectMsg = new ChatMessage(MessageType.CALL_REJECT, username, msg.getFrom(), null, "OccupÃƒÂ©");
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
                ChatMessage answerMsg = new ChatMessage(MessageType.CALL_ANSWER, username, msg.getFrom(), null, "Appel acceptÃƒÂ©");
                answerMsg.setCallType(msg.getCallType());
                networkClient.send(answerMsg);
            } else {
                ChatMessage rejectMsg = new ChatMessage(MessageType.CALL_REJECT, username, msg.getFrom(), null, "Appel refusÃƒÂ©");
                networkClient.send(rejectMsg);
            }
        });
    }

    private void handleCallAccepted(ChatMessage msg) {
        // L'appel a ÃƒÂ©tÃƒÂ© acceptÃƒÂ©, dÃƒÂ©marrer la session audio
        startAudioCall(msg.getFrom(), "AUDIO");
    }

    private void handleCallRejected(ChatMessage msg) {
        showInfo("Appel refusÃƒÂ© par " + msg.getFrom());
    }

    private void handleCallInfo(ChatMessage msg) {
        // Recevoir les infos P2P pour la connexion directe
        remoteHost = msg.getRemoteHost();
        remotePort = msg.getRemotePort();
        currentCallType = msg.getCallType();

        // DÃƒÂ©marrer la transmission audio P2P
        if ("AUDIO".equals(currentCallType) && remoteHost != null) {
            startAudioTransmission(msg.getFrom());
        }
    }

    private void startVoiceSession(String otherUser) {
        // Ancienne mÃƒÂ©thode - maintenant dÃƒÂ©lÃƒÂ©guÃƒÂ©e ÃƒÂ  startAudioCall
        startAudioCall(otherUser, "AUDIO");
    }

    private void startAudioCall(String otherUser, String callType) {
        if (currentVoiceCall != null || currentCallType != null) {
            showInfo("Un appel est dÃƒÂ©jÃƒÂ  en cours.");
            return;
        }

        try {
            currentCallType = callType;
            voiceCallPeer = otherUser;

            currentVoiceCall = new VoiceCallSession(networkClient, username, otherUser);
            currentVoiceCall.start();

            // Ouvrir la fenÃƒÂªtre d'appel
            VoiceCallWindow.open(username, otherUser, () -> {
                endAudioCall();
            });

            showInfo("Appel " + callType.toLowerCase() + " dÃƒÂ©marrÃƒÂ© avec " + otherUser);

        } catch (Exception e) {
            showInfo("Impossible de dÃƒÂ©marrer l'appel: " + e.getMessage());
            endAudioCall();
        }
    }

    private void startAudioTransmission(String otherUser) {
        if (audioTransmission != null) {
            audioTransmission.stop();
        }

        try {
            // DÃƒÂ©marrer la transmission P2P
            audioTransmission = new AudioTransmissionService();
            audioTransmission.initiate(remoteHost, remotePort);

            showInfo("Connexion audio ÃƒÂ©tablie avec " + otherUser);

        } catch (Exception e) {
            showInfo("Erreur de connexion audio: " + e.getMessage());
            endAudioCall();
        }
    }

    private void endVoiceCall() {
        endAudioCall(false);
    }


    private void endAudioCall() {
        endAudioCall(true);
    }

    private void endAudioCall(boolean notifyPeer) {
        if (currentVoiceCall != null) {
            currentVoiceCall.stop();
            currentVoiceCall = null;
        }
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
        if (notifyPeer && voiceCallPeer != null && networkClient != null) {
            ChatMessage endMsg = new ChatMessage(MessageType.VOICE_CALL_END, username, voiceCallPeer, null, "Appel termine");
            networkClient.send(endMsg);
        }
        VoiceCallWindow.close();
        currentCallType = null;
        voiceCallPeer = null;
        incomingCallFrom = null;
    }

    private void showInfo(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, cleanDisplayText(msg));
        alert.show();
    }

    private String cleanDisplayText(String text) {
        if (text == null) return "";
        return text
                .replace("Ã¢â€“Â¶", ">")
                .replace("Ã¢â€“Â ", "Stop")
                .replace("Ã¢â€”Â", "")
                .replace("ÃƒÂ©", "e")
                .replace("ÃƒÂ¨", "e")
                .replace("ÃƒÂª", "e")
                .replace("ÃƒÂ ", "a")
                .replace("ÃƒÂ§", "c")
                .replace("ÃƒÂ´", "o")
                .replace("ÃƒÂ®", "i")
                .replace("ÃƒÂ¢", "a")
                .replace("ÃƒÂ»", "u")
                .replace("Ãƒ", "")
                .replace("Â", "")
                .replace("â€“", "-")
                .replace("â€™", "'")
                .replace("â€œ", "\"")
                .replace("â€", "\"")
                .replace("â€¦", "...")
                .trim();
    }

    private void showAlert(String msg) {
        showInfo(msg);
    }

    private void showCallPending(String user, String message) {
        message = cleanDisplayText(message);
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

            if (currentGroupId != -1) {
                networkClient.sendGroupMedia(currentGroupId, isImage ? MessageType.GROUP_IMAGE : MessageType.GROUP_FILE, file.getName(), data);
                return;
            }

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
            showInfo("SÃƒÂ©lectionnez un contact privÃƒÂ© d'abord.");
            return;
        }
        if (currentVoiceCall != null || currentCallType != null) {
            showInfo("Un appel est dÃƒÂ©jÃƒÂ  en cours.");
            return;
        }

        networkClient.send(new ChatMessage(MessageType.VOICE_CALL_REQUEST, username, currentPrivateTarget, null, "Demande d'appel audio"));

        showInfo("Appel audio demandÃƒÂ© ÃƒÂ  " + currentPrivateTarget);
    }
    private Window getWindow() {
        return messageField != null && messageField.getScene() != null ? messageField.getScene().getWindow() : null;
    }

    private void startAudioRecording() {
        AudioFormat format = new AudioFormat(16000, 16, 1, true, false);
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
        if (!AudioSystem.isLineSupported(info)) {
            showInfo("L'enregistrement audio n'est pas supportÃƒÂ© sur cet appareil.");
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
            showInfo("Impossible d'accÃƒÂ©der au micro: " + e.getMessage());
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

            if (currentGroupId != -1) {
                networkClient.sendGroupMedia(currentGroupId, MessageType.GROUP_AUDIO, "Audio", Files.readAllBytes(tempFile));
            } else if (currentPrivateTarget != null) {
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
            recordAudioButton.setText("Ã¢â€“Â ");
            recordAudioButton.setText("Stop");
            recordAudioButton.setStyle("-fx-text-fill: #f87171; -fx-font-weight: bold;");
        } else {
            recordAudioButton.setText("Ã°Å¸Å½â„¢");
            recordAudioButton.setText("Audio");
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
                        String text = item.getText() == null ? "" : item.getText();

                        // Format localisation envoyÃ©: LAT=...;LON=...
                        boolean isLocation = text.startsWith("LAT=") && text.contains(";LON=");

                        if (isLocation) {
            String sep = ";LON=";
            int idxSep = text.indexOf(sep);
            if (idxSep < 0) {
                Label bubble = new Label(text);
                bubble.setWrapText(true);
                bubble.setMaxWidth(420);
                bubble.getStyleClass().add(isOwn ? "bubble-sent" : "bubble-received");

                Label time = new Label(item.getTimestamp());
                time.getStyleClass().add(isOwn ? "timestamp-sent" : "timestamp-received");

                VBox content = new VBox(2, bubble, time);
                content.setAlignment(isOwn ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
                row.getChildren().add(content);
                return;
            }

            String lat = text.substring("LAT=".length(), idxSep);
            String lon = text.substring(idxSep + sep.length());


                            VBox content = new VBox(6);
                            content.setMaxWidth(420);

                            VBox header = new VBox(2);
                            Label title = new Label("Position");
                            title.getStyleClass().add(isOwn ? "bubble-sent" : "bubble-received");

                            Label coords = new Label("Lat: " + lat + "\nLon: " + lon);
                            coords.getStyleClass().add(isOwn ? "bubble-sent" : "bubble-received");
                            coords.setWrapText(true);

                            Button openMap = new Button("Ouvrir la carte");
                            openMap.getStyleClass().add("btn-icon");
                            openMap.setOnAction(e -> {
                                String url = "https://www.openstreetmap.org/?mlat=" + lat + "&mlon=" + lon;
                                try {
                                    java.awt.Desktop.getDesktop().browse(new java.net.URI(url));
                                } catch (Exception ex) {
                                    showInfo("Lien carte: " + url);
                                }
                            });

                            header.getChildren().addAll(title, coords);
                            content.getChildren().addAll(header, openMap);

                            Label time = new Label(item.getTimestamp());
                            time.getStyleClass().add(isOwn ? "timestamp-sent" : "timestamp-received");
                            content.getChildren().add(time);

                            content.setAlignment(isOwn ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
                            row.getChildren().add(content);
                        } else {
                            String sharedContact = extractSharedContactName(text);
                            if (sharedContact != null) {
                                Button contactButton = new Button("Ouvrir " + sharedContact);
                                contactButton.getStyleClass().add(isOwn ? "bubble-sent" : "bubble-received");
                                contactButton.setMaxWidth(420);
                                contactButton.setOnAction(e -> {
                                    if (!allContacts.contains(sharedContact)) {
                                        allContacts.add(sharedContact);
                                    }
                                    openPrivateChat(sharedContact);
                                    if (tabPane != null && privateTab != null) {
                                        tabPane.getSelectionModel().select(privateTab);
                                    }
                                });

                                Label time = new Label(item.getTimestamp());
                                time.getStyleClass().add(isOwn ? "timestamp-sent" : "timestamp-received");

                                VBox content = new VBox(2, contactButton, time);
                                content.setAlignment(isOwn ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
                                row.getChildren().add(content);
                                break;
                            }

                            Label bubble = new Label(text);
                            bubble.setWrapText(true);
                            bubble.setMaxWidth(420);
                            bubble.getStyleClass().add(isOwn ? "bubble-sent" : "bubble-received");

                            Label time = new Label(item.getTimestamp());
                            time.getStyleClass().add(isOwn ? "timestamp-sent" : "timestamp-received");

                            VBox content = new VBox(2, bubble, time);
                            content.setAlignment(isOwn ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
                            row.getChildren().add(content);
                        }
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
                        Button play = new Button("Ã¢â€“Â¶");
                        play.setText("Lire audio");
                        play.getStyleClass().add("btn-icon");
                        play.setDisable(item.getFilePath() == null);
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
                        Label nameLabel = new Label("Ã°Å¸â€œÅ½ " + item.getText());
                        nameLabel.getStyleClass().add(isOwn ? "bubble-sent" : "bubble-received");
                        nameLabel.setText(item.getText());
                        Button downloadBtn = new Button("Ã°Å¸â€™Â¾");
                        downloadBtn.setText("Telecharger");
                        downloadBtn.getStyleClass().add("btn-icon");
                        downloadBtn.setDisable(item.getFilePath() == null);
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
        } catch (IOException e) { showInfo("Impossible de sauvegarder l'audio reÃƒÂ§u."); }
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
        } catch (IOException e) { showInfo("Impossible de sauvegarder l'image reÃƒÂ§ue."); }
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
        } catch (IOException e) { showInfo("Impossible de sauvegarder le fichier reÃƒÂ§u."); }
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
            showInfo("Fichier sauvegardÃƒÂ© : " + dest.getAbsolutePath());
        } catch (IOException e) { showInfo("Erreur lors de la sauvegarde : " + e.getMessage()); }
    }


    @FXML
    private void onOpenSettings() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Reglages");
        dialog.setHeaderText("Parametres du compte");

        Label avatar = new Label(username != null && !username.isBlank()
                ? username.substring(0, 1).toUpperCase()
                : "?");
        avatar.getStyleClass().add("avatar-letter-large");
        StackPane avatarCircle = new StackPane(avatar);
        avatarCircle.getStyleClass().add("profile-avatar");

        Label name = new Label(username != null ? username : "Utilisateur");
        name.getStyleClass().add("chat-contact-name");
        Label status = new Label(networkClient != null ? "Connecte au serveur" : "Hors ligne");
        status.getStyleClass().add("chat-contact-status");

        CheckBox notifications = new CheckBox("Notifications activees");
        notifications.setSelected(true);
        CheckBox compactMode = new CheckBox("Mode compact");

        VBox content = new VBox(12,
                new HBox(12, avatarCircle, new VBox(4, name, status)),
                new Separator(),
                notifications,
                compactMode,
                new Label("Les reglages sont appliques a cette session."));
        content.setPadding(new Insets(12));

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CLOSE);
        dialog.showAndWait();
    }

    @FXML
    private void onShowContactInfo() {
        if (currentPrivateTarget != null) {
            showInfo("Infos contact: " + currentPrivateTarget);
        } else {
            showInfo("Selectionnez un contact d'abord.");
        }
    }

    @FXML
    private void onMoreOptions() {
        ContextMenu menu = new ContextMenu();

        if (currentGroupId != -1) {
            MenuItem addMemberItem = new MenuItem("Ajouter un membre");
            addMemberItem.setOnAction(e -> openAddGroupMemberDialog());

            MenuItem groupInfoItem = new MenuItem("Infos du groupe");
            groupInfoItem.setOnAction(e -> showGroupInfo());

            MenuItem audioCallItem = new MenuItem("Appel audio du groupe");
            audioCallItem.setOnAction(e -> startCurrentGroupCall("AUDIO"));

            MenuItem videoCallItem = new MenuItem("Appel video du groupe");
            videoCallItem.setOnAction(e -> startCurrentGroupCall("VIDEO"));

            MenuItem searchItem = new MenuItem("Rechercher");
            searchItem.setOnAction(e -> searchInCurrentConversation());

            MenuItem selectMessagesItem = new MenuItem("Selectionner des messages");
            selectMessagesItem.setOnAction(e -> showInfo("Selection des messages activee: cliquez sur un message pour le consulter."));

            MenuItem clearItem = new MenuItem("Effacer la discussion");
            clearItem.setOnAction(e -> clearCurrentChat());

            MenuItem leaveItem = new MenuItem("Quitter le groupe");
            leaveItem.setOnAction(e -> leaveCurrentGroup());

            menu.getItems().addAll(
                    addMemberItem,
                    groupInfoItem,
                    audioCallItem,
                    videoCallItem,
                    searchItem,
                    selectMessagesItem,
                    new SeparatorMenuItem(),
                    clearItem,
                    leaveItem
            );
            menu.show(rootPane, Side.TOP, 0, 60);
            return;
        }

        MenuItem searchItem = new MenuItem("Rechercher dans la conversation");
        searchItem.setOnAction(e -> searchInCurrentConversation());
        MenuItem clearItem = new MenuItem("Effacer le chat");
        clearItem.setOnAction(e -> clearCurrentChat());
        menu.getItems().addAll(searchItem, clearItem);
        menu.show(rootPane, Side.TOP, 0, 60);
    }

    private void openAddGroupMemberDialog() {
        if (currentGroupId == -1) {
            showInfo("Selectionnez un groupe d'abord.");
            return;
        }
        if (allContacts.isEmpty()) {
            showInfo("Aucun contact disponible a ajouter.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/add-member-dialog.fxml"));
            VBox content = loader.load();
            AddMemberDialogController controller = loader.getController();
            controller.init(allContacts);

            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("Ajouter un membre");
            dialog.setHeaderText(currentGroupName);
            dialog.getDialogPane().setContent(content);
            dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

            dialog.showAndWait().ifPresent(button -> {
                if (button == ButtonType.OK) {
                    for (String contact : controller.getSelectedMembers()) {
                        networkClient.addGroupMember(currentGroupId, contact);
                    }
                }
            });
        } catch (IOException e) {
            showInfo("Impossible d'ajouter un membre: " + e.getMessage());
        }
    }

    private void showGroupInfo() {
        if (currentGroupId == -1) {
            showInfo("Selectionnez un groupe d'abord.");
            return;
        }

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Infos du groupe");
        alert.setHeaderText(currentGroupName);
        alert.setContentText(buildGroupInfoText(groupMembersById.get(currentGroupId)));
        groupInfoAlert = alert;
        networkClient.requestGroupMembers(currentGroupId);
        alert.showAndWait();
        groupInfoAlert = null;
    }

    private String buildGroupInfoText(String rawMembers) {
        if (rawMembers == null || rawMembers.isBlank()) {
            return "Chargement des membres...";
        }

        StringBuilder text = new StringBuilder("Membres du groupe:\n");
        for (String entry : rawMembers.split(",")) {
            String[] parts = entry.split(":");
            if (parts.length >= 2) {
                String name = parts[1].trim();
                String role = parts.length >= 3 ? parts[2].trim() : "";
                text.append("- ").append(name);
                if ("ADMIN".equalsIgnoreCase(role)) {
                    text.append(" (Admin)");
                }
                text.append("\n");
            }
        }

        String content = text.toString().trim();
        return content.equals("Membres du groupe:") ? "Aucun membre trouve." : content;
    }

    private void startCurrentGroupCall(String meetingType) {
        if (currentGroupId == -1) {
            showInfo("Selectionnez un groupe d'abord.");
            return;
        }
        networkClient.startGroupMeeting(currentGroupId, meetingType);
        showInfo(("VIDEO".equalsIgnoreCase(meetingType) ? "Appel video" : "Appel audio")
                + " lance pour " + currentGroupName + ".");
    }

    private void leaveCurrentGroup() {
        if (currentGroupId == -1) {
            showInfo("Selectionnez un groupe d'abord.");
            return;
        }

        Alert confirm = new Alert(
                Alert.AlertType.CONFIRMATION,
                "Voulez-vous quitter le groupe \"" + currentGroupName + "\" ?",
                ButtonType.YES,
                ButtonType.NO
        );
        confirm.setHeaderText("Quitter le groupe");
        confirm.showAndWait().ifPresent(button -> {
            if (button == ButtonType.YES) {
                networkClient.leaveGroup(currentGroupId);
                int leavingGroupId = currentGroupId;
                currentGroupId = -1;
                currentGroupName = null;
                groupConversations.remove(leavingGroupId);
                messagesListView.setItems(FXCollections.observableArrayList());
                chatTitleLabel.setText("Selectionnez une conversation");
                chatStatusLabel.setText("");
                chatAvatarLabel.setText("?");
                networkClient.requestGroupList();
            }
        });
    }

    private void searchInCurrentConversation() {
        ObservableList<UiMessage> messages;
        if (currentGroupId != -1) {
            messages = groupConversations.get(currentGroupId);
        } else if (currentPrivateTarget != null) {
            messages = privateConversations.get(currentPrivateTarget);
        } else {
            messages = null;
        }
        if (messages == null) {
            showInfo("Selectionnez une conversation d'abord.");
            return;
        }

        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Recherche");
        String conversationName = currentGroupId != -1 ? currentGroupName : currentPrivateTarget;
        dialog.setHeaderText("Rechercher dans " + conversationName);
        dialog.setContentText("Mot ou phrase :");

        Optional<String> result = dialog.showAndWait();
        if (result.isEmpty()) return;

        String query = result.get().trim().toLowerCase();
        if (query.isEmpty()) return;

        List<Integer> resultIndexes = new ArrayList<>();
        for (int i = 0; i < messages.size(); i++) {
            if (messageContains(messages.get(i), query)) {
                resultIndexes.add(i);
            }
        }

        if (resultIndexes.isEmpty()) {
            showInfo("Aucun resultat pour: " + result.get().trim());
            return;
        }

        if (resultIndexes.size() == 1) {
            selectSearchResult(resultIndexes.get(0), messages);
            return;
        }

        showSearchResultsDialog(result.get().trim(), resultIndexes, messages);
    }

    private boolean messageContains(UiMessage message, String query) {
        String text = String.valueOf(message.getText()).toLowerCase();
        String path = String.valueOf(message.getFilePath()).toLowerCase();
        return text.contains(query) || path.contains(query) || message.getKind().name().toLowerCase().contains(query);
    }

    private void showSearchResultsDialog(String query, List<Integer> resultIndexes, ObservableList<UiMessage> messages) {
        Dialog<Integer> dialog = new Dialog<>();
        dialog.setTitle("Resultats de recherche");
        dialog.setHeaderText(resultIndexes.size() + " messages trouves pour: " + query);

        ListView<Integer> resultsList = new ListView<>();
        resultsList.setPrefSize(420, 320);
        resultsList.setItems(FXCollections.observableArrayList(resultIndexes));
        resultsList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Integer index, boolean empty) {
                super.updateItem(index, empty);
                if (empty || index == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }

                UiMessage message = messages.get(index);
                Label date = new Label(message.getTimestamp());
                date.getStyleClass().add("search-result-date");

                Label preview = new Label(buildSearchPreview(message));
                preview.getStyleClass().add("search-result-preview");
                preview.setWrapText(true);

                VBox content = new VBox(6, date, preview);
                content.getStyleClass().add("search-result-row");
                setText(null);
                setGraphic(content);
            }
        });

        resultsList.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && resultsList.getSelectionModel().getSelectedItem() != null) {
                dialog.setResult(resultsList.getSelectionModel().getSelectedItem());
                dialog.close();
            }
        });

        dialog.getDialogPane().setContent(resultsList);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.setResultConverter(button -> {
            if (button == ButtonType.OK) {
                return resultsList.getSelectionModel().getSelectedItem();
            }
            return null;
        });

        dialog.showAndWait().ifPresent(index -> selectSearchResult(index, messages));
    }

    private void selectSearchResult(int index, ObservableList<UiMessage> messages) {
        messagesListView.setItems(messages);
        messagesListView.getSelectionModel().clearAndSelect(index);
        messagesListView.getFocusModel().focus(index);
        messagesListView.scrollTo(Math.max(0, index - 2));
        messagesListView.requestFocus();

        UiMessage found = messages.get(index);
        String preview = buildSearchPreview(found);
        showInfo("Message selectionne:\n" + preview + "\n\nHeure: " + found.getTimestamp());
    }

    private String buildSearchPreview(UiMessage message) {
        String text = message.getText();
        if (text == null || text.isBlank()) {
            text = switch (message.getKind()) {
                case IMAGE -> "Image";
                case AUDIO -> "Audio";
                case FILE -> "Fichier";
                default -> "Message";
            };
        }
        text = text.replaceAll("\\s+", " ").trim();
        return text.length() > 180 ? text.substring(0, 177) + "..." : text;
    }

    private String extractSharedContactName(String text) {
        if (text == null) return null;
        String marker = "Contact partage:";
        int markerIndex = text.indexOf(marker);
        if (markerIndex < 0) return null;

        String contact = text.substring(markerIndex + marker.length()).trim();
        return contact.isEmpty() ? null : contact;
    }

    private void clearCurrentChat() {
        if (currentGroupId != -1 && groupConversations.containsKey(currentGroupId)) {
            groupConversations.get(currentGroupId).clear();
            messagesListView.setItems(groupConversations.get(currentGroupId));
            showInfo("Chat efface.");
            return;
        }
        if (currentPrivateTarget != null && privateConversations.containsKey(currentPrivateTarget)) {
            privateConversations.get(currentPrivateTarget).clear();
            messagesListView.setItems(privateConversations.get(currentPrivateTarget));
            showInfo("Chat efface.");
        }
    }

    @FXML
    private void onUploadImage() {
        onUploadFile();
    }

    @FXML
    private void onShareContact() {
        if (allContacts.isEmpty()) {
            showInfo("Aucun contact disponible a partager.");
            return;
        }

        ChoiceDialog<String> dialog = new ChoiceDialog<>(allContacts.get(0), allContacts);
        dialog.setTitle("Partager un contact");
        dialog.setHeaderText("Choisissez un contact a partager");
        dialog.setContentText("Contact :");

        Optional<String> choice = dialog.showAndWait();
        if (choice.isEmpty()) return;

        String sharedContact = choice.get();
        String text = "Contact partage: " + sharedContact;
        if (currentGroupId != -1) {
            networkClient.sendGroupMessage(currentGroupId, text);
            return;
        }
        if (currentPrivateTarget == null) {
            showInfo("Selectionnez une conversation d'abord.");
            return;
        }
        ChatMessage msg = new ChatMessage(MessageType.PRIVATE, username, currentPrivateTarget, currentConversationId, text);
        networkClient.send(msg);
        addPrivateMessage(msg);
    }

    @FXML
    private void onShowChats() {
        if (tabPane != null && privateTab != null) {
            tabPane.getSelectionModel().select(privateTab);
        }
        showPrivateCenter();
    }

    @FXML
    private void onShowContacts() {
        if (tabPane != null && contactsTab != null) {
            tabPane.getSelectionModel().select(contactsTab);
        }
        showPrivateCenter();
    }

    @FXML
    private void onShowCalls() {
        if (allContacts.isEmpty()) {
            showInfo("Ajoutez un contact avant de lancer un appel.");
            return;
        }

        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Appels");
        dialog.setHeaderText("Choisissez un contact a appeler");

        ListView<String> listView = new ListView<>(allContacts);
        listView.setPrefSize(320, 320);
        dialog.getDialogPane().setContent(listView);

        ButtonType audioButton = new ButtonType("Audio", ButtonBar.ButtonData.LEFT);
        ButtonType videoButton = new ButtonType("Video", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(audioButton, videoButton, ButtonType.CANCEL);

        final ButtonType[] action = new ButtonType[1];
        dialog.setResultConverter(button -> {
            action[0] = button;
            return button == ButtonType.CANCEL ? null : listView.getSelectionModel().getSelectedItem();
        });

        Optional<String> selected = dialog.showAndWait();
        if (selected.isEmpty()) return;

        openPrivateChat(selected.get());
        if (action[0] == videoButton) {
            onStartVideoCall();
        } else if (action[0] == audioButton) {
            onStartVoiceCall();
        }
    }

    @FXML
    private void onToggleEmojiPicker() {
        if (emojiBar == null) return;
        boolean visible = emojiBar.isVisible();
        emojiBar.setVisible(!visible);
        emojiBar.setManaged(!visible);
        if (!visible && !EMOJIS.isEmpty()) {
            populateEmojiGrid();
        }
    }

    private void populateEmojiGrid() {
        if (emojiGrid != null) {
            emojiGrid.getChildren().clear();
            for (String emoji : EMOJIS) {
                Button button = new Button(emoji);
                button.getStyleClass().add("emoji-item");
                button.setOnAction(e -> {
                    messageField.appendText(emoji);
                    emojiBar.setVisible(false);
                    emojiBar.setManaged(false);
                    messageField.requestFocus();
                });
                emojiGrid.getChildren().add(button);
            }
        }
    }

}




