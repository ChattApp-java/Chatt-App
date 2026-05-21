package org.example.tpchatjavafx.client.controller;

import javafx.application.Platform;
import javafx.animation.PauseTransition;
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
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import javafx.util.Duration;
import org.example.tpchatjavafx.client.NetworkClient;
import org.example.tpchatjavafx.client.model.ChatMessage;
import org.example.tpchatjavafx.client.util.FileIconResolver;
import org.example.tpchatjavafx.client.util.UiMessage;
import org.example.tpchatjavafx.client.util.WaveformVisualizer;
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
    private static final String MIC_ICON =
            "M12 14 C13.66 14 15 12.66 15 11 V5 C15 3.34 13.66 2 12 2 C10.34 2 9 3.34 9 5 V11 C9 12.66 10.34 14 12 14 Z M17.3 11 C17.3 14 14.76 16.1 12 16.1 C9.24 16.1 6.7 14 6.7 11 H5 C5 14.41 7.72 17.23 11 17.72 V21 H13 V17.72 C16.28 17.24 19 14.42 19 11 H17.3 Z";
    private static final String STOP_ICON = "M6 6 H18 V18 H6 Z";
    private static final String DOWNLOAD_ICON = "M12 3 V13.2 L15.6 9.6 L17 11 L12 16 L7 11 L8.4 9.6 L11 12.2 V3 H12 Z M5 19 H19 V21 H5 V19 Z";
    private static final String PLAY_ICON = "M8 5 V19 L19 12 Z";

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
    @FXML private Button    searchConversationButton;
    @FXML private Button    moreOptionsButton;
    @FXML private TabPane   tabPane;
    @FXML private HBox emojiBar;
    @FXML private FlowPane emojiGrid;
    @FXML private Label usernameLabel;
    @FXML private HBox selectionBar;
    @FXML private MessageSelectionBarController selectionBarController;
    @FXML private HBox recordingWaveformBar;

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
    private boolean isSendingContact = false;
    private static final List<String> EMOJIS = Arrays.asList(
            "\uD83D\uDE02", "\uD83E\uDD23", "\uD83D\uDE0A", "\uD83D\uDE05", "\uD83E\uDD7A", "\uD83D\uDE0E",
            "\uD83E\uDD14", "\uD83D\uDE44", "\uD83D\uDE2D", "\u2764\uFE0F", "\uD83D\uDD25", "\uD83D\uDCAF",
            "\uD83D\uDC4D", "\u2705", "\uD83D\uDE4F", "\uD83D\uDCAA", "\uD83D\uDC4F", "\uD83D\uDC4C",
            "\u274C", "\uD83D\uDC40", "\uD83E\uDD1D", "\uD83E\uDEF6", "\uD83E\uDDE1", "\uD83D\uDC99",
            "\uD83D\uDC94", "\uD83D\uDC95", "\uD83E\uDD0D", "\uD83D\uDC4E", "\uD83D\uDE0D", "\uD83D\uDE18",
            "\uD83D\uDE14", "\uD83D\uDE22", "\uD83D\uDE21", "\uD83C\uDF89", "\u2B50", "\uD83C\uDF08"
    );

    private volatile boolean recordingAudio = false;
    private TargetDataLine targetDataLine;
    private Thread recordingThread;
    private final List<Double> recordingAmplitudeData = Collections.synchronizedList(new ArrayList<>());
    private WaveformVisualizer recordingWaveform;

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
    private boolean selectionMode = false;
    private boolean multiMessageSelectionMode = false;
    private final Set<UiMessage> selectedMessages = new LinkedHashSet<>();
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
        updateCallButtonsVisibility();
        if (messagesListView != null) {
            messagesListView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        }
        if (selectionBarController != null) {
            selectionBarController.setHandlers(
                    this::copySelectedMessages,
                    this::forwardSelectedMessages,
                    this::deleteSelectedMessagesForEveryone,
                    this::selectAllMessages,
                    this::exitSelectionMode
            );
            selectionBarController.setSelectedCount(0);
        }
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

        // ===== CALLBACKS RÃƒÆ’Ã¢â‚¬Â°SEAU =====
        networkClient.setOnIncomingCall(msg -> {
            handleIncomingCall(msg);
        });

        networkClient.setOnCallAnswered(this::handleCallAccepted);

        networkClient.setOnCallRejected(msg -> {
            handleCallRejected(msg);
        });

        networkClient.setOnMeetingStarted(msg -> Platform.runLater(() -> {
            try {
                String callType = msg.getMeetingType() != null ? msg.getMeetingType() : "AUDIO";
                int groupId = msg.getGroupId() > 0 ? msg.getGroupId() : currentGroupId;
                String title = groupId == currentGroupId && currentGroupName != null && !currentGroupName.isBlank()
                        ? currentGroupName
                        : "Ma reunion";
                MeetingController.openMeetingWindow(networkClient, groupId, callType, msg.getMeetingId(),
                        title, username, true);
            } catch (IOException e) {
                e.printStackTrace();
                showInfo("Impossible d'ouvrir la reunion: " + e.getMessage());
            }
        }));

        networkClient.setOnMeetingInvite(msg -> {
            Platform.runLater(() -> {
                try {
                    IncomingMeetingDialogController.showInvite(msg.getFrom(), msg.getMeetingType(), accepted -> {
                        if (accepted) {
                            networkClient.joinMeeting(msg.getMeetingId());
                            try {
                                String callType = msg.getMeetingType() != null ? msg.getMeetingType() : "AUDIO";
                                int groupId = msg.getGroupId() > 0 ? msg.getGroupId() : currentGroupId;
                                MeetingController.openMeetingWindow(networkClient, groupId, callType, msg.getMeetingId(),
                                        "Reunion de " + msg.getFrom(), username, false);
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

        networkClient.setOnMeetingInfo(msg -> Platform.runLater(() -> {
            try {
                networkClient.startMeetingAudio(msg.getMeetingId(), 0, msg.getServerUdpAudioPort(), msg.getServerHost());
                if (msg.getMeetingType() != null && msg.getMeetingType().toUpperCase().contains("VIDEO")) {
                    networkClient.startMeetingVideo(msg.getMeetingId(), 0, msg.getServerUdpVideoPort(), msg.getServerHost());
                }
                networkClient.syncMeetingMediaPorts(msg.getMeetingId());
            } catch (Exception e) {
                System.err.println("[MAIN_CHAT] Erreur demarrage relais UDP reunion: " + e.getMessage());
            }
        }));

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

    public void onWindowResize(double width, double height) {
        if (rootPane == null) return;
        Node sidebar = rootPane.getLeft();
        if (sidebar instanceof VBox sidebarBox) {
            double targetWidth = Math.max(280, Math.min(400, width * 0.30));
            sidebarBox.setPrefWidth(targetWidth);
        }
        if (privateChatPane != null) {
            privateChatPane.setMinWidth(Math.max(400, width - 420));
        }
        if (messagesListView != null) {
            messagesListView.refresh();
        }
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
                    setContextMenu(null);
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
                setContextMenu(createContactContextMenu(contact));
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
        updateCallButtonsVisibility();
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
            if (msg.getType() == MessageType.GROUP_HISTORY_RESPONSE && msg.getConversationId() != null) {
                msg.setType(safeMessageType(msg.getConversationId()));
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
                        .add(new UiMessage(UiMessage.Kind.SYSTEM, false, msg.getContent() + " a rejoint le groupe.", null,
                                java.time.LocalDateTime.now().format(timeFormatter)));
            }
            networkClient.requestGroupList();
        }));

        networkClient.setOnGroupMemberRemoved(msg -> Platform.runLater(() -> {
            String systemText = msg.getType() == MessageType.REMOVE_GROUP_MEMBER
                    ? msg.getContent()
                    : msg.getContent() + " a quitte le groupe.";
            if (msg.getGroupId() == currentGroupId) {
                groupConversations.computeIfAbsent(currentGroupId, k -> FXCollections.observableArrayList())
                        .add(new UiMessage(UiMessage.Kind.SYSTEM, false, systemText, null,
                                java.time.LocalDateTime.now().format(timeFormatter)));
                messagesListView.refresh();
            }
            if (msg.getType() == MessageType.REMOVE_GROUP_MEMBER
                    && msg.getContent() != null && username != null && msg.getContent().startsWith(username + " ")) {
                removeGroupLocally(msg.getGroupId());
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

    private void removeGroupLocally(int groupId) {
        for (int i = groupIds.size() - 1; i >= 0; i--) {
            if (groupIds.get(i)[0] == groupId) {
                groupIds.remove(i);
                if (i < groupNames.size()) groupNames.remove(i);
            }
        }
        groupConversations.remove(groupId);
        groupMembersById.remove(groupId);
        if (groupTabListView != null) groupTabListView.refresh();
        if (currentGroupId == groupId) {
            currentGroupId = -1;
            currentGroupName = null;
            messagesListView.setItems(FXCollections.observableArrayList());
            chatTitleLabel.setText("Selectionnez une conversation");
            chatStatusLabel.setText("");
            chatAvatarLabel.setText("?");
            updateCallButtonsVisibility();
        }
    }

    private void openGroupChat(int groupId, String groupName) {
        currentPrivateTarget = null;
        currentGroupId = groupId;
        currentGroupName = groupName;
        chatTitleLabel.setText(groupName);
        chatStatusLabel.setText("Groupe de discussion");
        chatAvatarLabel.setText(groupName.substring(0, 1).toUpperCase());
        updateCallButtonsVisibility();
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
        if (kind == UiMessage.Kind.SYSTEM) {
            own = false;
        }
        if (!own && kind == UiMessage.Kind.TEXT && msg.getFrom() != null && !fromHistory) {
            text = msg.getFrom() + ": " + text;
        }
        if (!own && kind == UiMessage.Kind.TEXT && fromHistory && msg.getFrom() != null) {
            text = msg.getFrom() + ": " + text;
        }
        String time = msg.getTimestamp();
        if (time == null || time.isBlank()) time = java.time.LocalDateTime.now().format(timeFormatter);
        String filePath = saveIncomingGroupMedia(kind, msg.getContent(), msg.getBinaryData());
        UiMessage uiMessage = new UiMessage(kind, own, text, filePath, time, msg.getMessageId());
        groupConversations.computeIfAbsent(gid, k -> FXCollections.observableArrayList()).add(uiMessage);
        if (gid == currentGroupId) {
            messagesListView.setItems(groupConversations.get(gid));
            messagesListView.scrollTo(groupConversations.get(gid).size() - 1);
        }
    }

    private UiMessage.Kind kindForGroup(MessageType type) {
        if (type == MessageType.SYSTEM || type == MessageType.REMOVE_GROUP_MEMBER) return UiMessage.Kind.SYSTEM;
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
            dialog.setHeaderText("Entrez le nom d'utilisateur du contact ÃƒÆ’Ã‚Â  ajouter");
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
            showInfo("Vous ne pouvez pas vous ajouter vous-mÃƒÆ’Ã‚Âªme.");
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
        messageField.clear();
    }

    @FXML
    private void onVoiceCall() {
        if (currentPrivateTarget == null) {
            showAlert("SÃƒÆ’Ã‚Â©lectionnez un contact d'abord");
            return;
        }

        // VÃƒÆ’Ã‚Â©rifier que le contact est en ligne
        if (!userStatuses.getOrDefault(currentPrivateTarget, "NON_CONNECTE").equals("EN_LIGNE")) {
            showAlert("Utilisateur hors ligne");
            return;
        }

        networkClient.send(new ChatMessage(MessageType.VOICE_CALL_REQUEST, username, currentPrivateTarget, null, "Demande d'appel audio"));

        // UI: afficher ÃƒÆ’Ã‚Â©tat "Appel en cours..."
        showCallPending(currentPrivateTarget, "Appel vocal en cours...");
    }

    @FXML
    private void onVideoCall() {
        // Similaire ÃƒÆ’Ã‚Â  voice call mais avec callType = "VIDEO"
        if (currentPrivateTarget == null) {
            showAlert("SÃƒÆ’Ã‚Â©lectionnez un contact d'abord");
            return;
        }

        ChatMessage callRequest = new ChatMessage();
        callRequest.setType(MessageType.CALL_REQUEST);
        callRequest.setFrom(username);
        callRequest.setTo(currentPrivateTarget);
        callRequest.setCallType("VIDEO");

        networkClient.send(callRequest);
        showCallPending(currentPrivateTarget, "Appel vidÃƒÆ’Ã‚Â©o en cours...");
    }

    private void setupContactCellFactory() {
        privateListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(String user, boolean empty) {
                super.updateItem(user, empty);
                if (empty || user == null) { setGraphic(null); setText(null); setContextMenu(null); return; }
                String initial = user.substring(0, 1).toUpperCase();

                Label av = new Label(initial);
                av.getStyleClass().add("avatar-letter");
                StackPane avatar = new StackPane(av);
                avatar.getStyleClass().add("avatar-circle");

                avatar.setStyle("-fx-background-color:" + pickAvatarColor(user) + "; -fx-background-radius:50%; -fx-min-width:42px; -fx-min-height:42px; -fx-max-width:42px; -fx-max-height:42px;");

                Label name = new Label(user);
                name.getStyleClass().add("chat-contact-name");

                String statut = userStatuses.getOrDefault(user, "NON_CONNECTE");
                Label sub = new Label(statut.equals("EN_LIGNE") ? "ÃƒÂ¢Ã¢â‚¬â€Ã‚Â En ligne" : "ÃƒÂ¢Ã¢â‚¬â€Ã‚Â Non connectÃƒÆ’Ã‚Â©");
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
                setContextMenu(createContactContextMenu(user));
            }
        });
    }

    private void onUserStatusChanged(ChatMessage msg) {
        String targetUser = msg.getFrom();
        String status = msg.getContent();
        userStatuses.put(targetUser, status);

        Platform.runLater(() -> {
            privateListView.refresh();
            if (contactsTabListView != null) contactsTabListView.refresh();
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
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Deconnexion");
        confirm.setHeaderText("Se deconnecter ?");
        confirm.setContentText("Vous devrez vous reconnecter pour acceder a vos messages.");
        confirm.getDialogPane().setStyle("-fx-background-color: #9dd1f9;");

        Node contentLabel = confirm.getDialogPane().lookup(".content.label");
        if (contentLabel != null) {
            contentLabel.setStyle("-fx-text-fill: white;");
        }
        Node headerLabel = confirm.getDialogPane().lookup(".header-panel .label");
        if (headerLabel != null) {
            headerLabel.setStyle("-fx-text-fill: white;");
        }

        ButtonType btnOui = new ButtonType("Oui");
        ButtonType btnNon = new ButtonType("Non", ButtonBar.ButtonData.CANCEL_CLOSE);
        confirm.getButtonTypes().setAll(btnOui, btnNon);

        if (confirm.showAndWait().orElse(btnNon) == btnOui) {
            if (networkClient != null) networkClient.close();
            try {
                javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/fxml/login.fxml"));
                javafx.scene.Parent root = loader.load();
                javafx.scene.Scene scene = new javafx.scene.Scene(root);
                scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
                javafx.stage.Stage stage = (javafx.stage.Stage) rootPane.getScene().getWindow();
                stage.setScene(scene);
            } catch (java.io.IOException e) {
                e.printStackTrace();
                Platform.exit();
            }
        }
    }

    private void openPrivateChat(String other) {
        if (other == null || other.isBlank()) {
            return;
        }
        currentPrivateTarget = other;
        currentGroupId = -1;
        currentGroupName = null;
        if (chatTitleLabel  != null) chatTitleLabel.setText(other);
        if (chatAvatarLabel != null) chatAvatarLabel.setText(other.substring(0,1).toUpperCase());
        if (privateListView != null) {
            privateListView.getSelectionModel().select(other);
        }
        if (contactsTabListView != null) {
            contactsTabListView.getSelectionModel().select(other);
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
        markVisibleMessagesRead(other, msgs);

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
                        System.err.println("Erreur sauvegarde mÃƒÆ’Ã‚Â©dia historique: " + e.getMessage());
                    }
                }
                uiMsg = new UiMessage(kind, own, msg.getContent(), localPath, time, msg.getMessageId());
                uiMsg.setRead(msg.isRead());
            } else {
                uiMsg = new UiMessage(UiMessage.Kind.TEXT, own, msg.getContent(), null, time, msg.getMessageId());
                uiMsg.setRead(msg.isRead());
            }

            if (uiMsg != null) {
                uiMsgs.add(uiMsg);
            }

            // Auto-scroll if it's the current view
            if (other.equals(currentPrivateTarget)) {
                if (!own) markMessageRead(msg);
                messagesListView.scrollTo(uiMsgs.size() - 1);
            }
        });
    }

    private void updateChatHeaderStatus(String status) {
        if (chatStatusLabel != null) {
            chatStatusLabel.setText(status.equals("EN_LIGNE") ? "ÃƒÂ¢Ã¢â‚¬â€Ã‚Â En ligne" : "ÃƒÂ¢Ã¢â‚¬â€Ã‚Â Non connectÃƒÆ’Ã‚Â©");
            chatStatusLabel.setText(status.equals("EN_LIGNE") ? "En ligne" : "Non connecte");
            chatStatusLabel.setStyle(status.equals("EN_LIGNE") ? "-fx-text-fill: #25D366;" : "-fx-text-fill: #8e8e8e;");
        }
    }

    private void onMessageReceived(ChatMessage msg) {
        if (msg.getType() == MessageType.CHAT_DELETE_EVERYONE) {
            Platform.runLater(() -> handlePrivateChatDeletedForEveryone(msg));
            return;
        }
        if (msg.getType() == MessageType.GROUP_CHAT_DELETE_EVERYONE) {
            Platform.runLater(() -> handleGroupChatDeletedForEveryone(msg));
            return;
        }
        if (msg.getType() == MessageType.MESSAGE_DELETE_EVERYONE || msg.getType() == MessageType.DELETE_MESSAGE) {
            Platform.runLater(() -> removeMessageLocally(msg.getMessageId()));
            return;
        }
        if (msg.getType() == MessageType.MESSAGE_READ) {
            Platform.runLater(() -> markLocalMessageRead(msg.getMessageId()));
            return;
        }

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
            // Appel vidÃƒÆ’Ã‚Â©o
            case VIDEO_CALL_REQUEST -> Platform.runLater(() -> handleCallRequest(msg));
            case VIDEO_CALL_ACCEPT  -> Platform.runLater(() -> startVideoWindow(msg.getFrom(), true));
            case VIDEO_CALL_REJECT  -> Platform.runLater(() -> showInfo("Appel refusÃƒÆ’Ã‚Â© par " + msg.getFrom()));
            case VIDEO_CALL_END     -> Platform.runLater(this::endVideo);
            case VIDEO_FRAME        -> { }
            // Appel vocal
            case VOICE_CALL_REQUEST -> Platform.runLater(() -> handleVoiceCallRequest(msg));
            case VOICE_CALL_ACCEPT  -> Platform.runLater(() -> startVoiceSession(msg.getFrom()));
            case CALL_INCOMING      -> Platform.runLater(() -> {
                if ("VIDEO".equalsIgnoreCase(msg.getCallType())) {
                    handleIncomingCall(msg);
                }
            });
            case CALL_ANSWER        -> Platform.runLater(() -> {
                if ("VIDEO".equalsIgnoreCase(msg.getCallType())) {
                    handleCallAccepted(msg);
                }
            });
            case CALL_REJECT        -> Platform.runLater(() -> {
                if ("VIDEO".equalsIgnoreCase(msg.getCallType())) {
                    handleCallRejected(msg);
                }
            });
            case CALL_INFO          -> Platform.runLater(() -> {
                if ("AUDIO".equalsIgnoreCase(msg.getCallType())) {
                    handleCallInfo(msg);
                }
            });
            case VOICE_CALL_REJECT  -> Platform.runLater(() -> showInfo("Appel vocal refusÃƒÆ’Ã‚Â© par " + msg.getFrom()));
            case VOICE_CALL_END     -> Platform.runLater(this::endVoiceCall);
            case VOICE_FRAME        -> { }
            default -> {}
        }

        // Increment unread count if not in current chat
        if (msg.getType() != MessageType.VIDEO_FRAME
                && msg.getType() != MessageType.VOICE_FRAME
                && !msg.getType().name().contains("CALL")
                && msg.getType() != MessageType.SYSTEM) {
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

            UiMessage uiMsg = new UiMessage(UiMessage.Kind.TEXT, isOwn, msg.getContent(), null, time, msg.getMessageId());
            uiMsg.setRead(msg.isRead());
            msgs.add(uiMsg);

            if (other.equals(currentPrivateTarget)) {
                if (!isOwn) markMessageRead(msg);
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
        alert.setTitle("WeChat - Appel VidÃƒÆ’Ã‚Â©o");
        alert.setHeaderText("Appel vidÃƒÆ’Ã‚Â©o entrant de " + msg.getFrom());
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

    // ===== NOUVELLES MÃƒÆ’Ã¢â‚¬Â°THODES POUR LES APPELS =====

    private void handleIncomingCall(ChatMessage msg) {
        if (currentCallType != null) {
            ChatMessage rejectMsg = new ChatMessage(MessageType.CALL_REJECT, username, msg.getFrom(), null, "Occupe");
            rejectMsg.setCallType(msg.getCallType());
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
                ChatMessage answerMsg = new ChatMessage(MessageType.CALL_ANSWER, username, msg.getFrom(), null, "Appel accepte");
                answerMsg.setCallType(msg.getCallType());
                networkClient.send(answerMsg);
                if ("VIDEO".equalsIgnoreCase(msg.getCallType())) {
                    startVideoWindow(msg.getFrom(), false);
                }
            } else {
                ChatMessage rejectMsg = new ChatMessage(MessageType.CALL_REJECT, username, msg.getFrom(), null, "Appel refuse");
                rejectMsg.setCallType(msg.getCallType());
                networkClient.send(rejectMsg);
            }
        });
    }

    private void handleCallAccepted(ChatMessage msg) {
        String acceptedType = msg.getCallType();
        if ("VIDEO".equalsIgnoreCase(acceptedType)) {
            startVideoWindow(msg.getFrom(), true);
            return;
        }
        if ("AUDIO".equalsIgnoreCase(acceptedType)) {
            startAudioCall(msg.getFrom(), "AUDIO");
        }
    }

    private void handleCallRejected(ChatMessage msg) {
        String callType = msg.getCallType();
        if ("VIDEO".equalsIgnoreCase(callType)) {
            showInfo("Appel video refuse par " + msg.getFrom());
        } else {
            showInfo("Appel refuse par " + msg.getFrom());
        }
    }

    private void handleCallInfo(ChatMessage msg) {
        // Recevoir les infos P2P pour la connexion directe
        remoteHost = msg.getRemoteHost();
        remotePort = msg.getRemotePort();
        currentCallType = msg.getCallType();

        // DÃƒÆ’Ã‚Â©marrer la transmission audio P2P
        if ("AUDIO".equals(currentCallType) && remoteHost != null) {
            startAudioTransmission(msg.getFrom());
        }
    }

    private void startVoiceSession(String otherUser) {
        // Ancienne mÃƒÆ’Ã‚Â©thode - maintenant dÃƒÆ’Ã‚Â©lÃƒÆ’Ã‚Â©guÃƒÆ’Ã‚Â©e ÃƒÆ’Ã‚Â  startAudioCall
        startAudioCall(otherUser, "AUDIO");
    }

    private void startAudioCall(String otherUser, String callType) {
        if (currentVoiceCall != null || currentCallType != null) {
            showInfo("Un appel est dÃƒÆ’Ã‚Â©jÃƒÆ’Ã‚Â  en cours.");
            return;
        }

        try {
            currentCallType = callType;
            voiceCallPeer = otherUser;

            currentVoiceCall = new VoiceCallSession(networkClient, username, otherUser);
            currentVoiceCall.start();

            // Ouvrir la fenÃƒÆ’Ã‚Âªtre d'appel
            VoiceCallWindow.open(
                    username,
                    otherUser,
                    this::endAudioCall,
                    () -> {
                        if (currentVoiceCall != null) {
                            currentVoiceCall.setMicrophoneMuted(!currentVoiceCall.isMicrophoneMuted());
                        }
                    },
                    () -> {
                        if (currentVoiceCall != null) {
                            currentVoiceCall.setSpeakerEnabled(!currentVoiceCall.isSpeakerEnabled());
                        }
                    }
            );

            showInfo("Appel " + callType.toLowerCase() + " dÃƒÆ’Ã‚Â©marrÃƒÆ’Ã‚Â© avec " + otherUser);

        } catch (Exception e) {
            showInfo("Impossible de dÃƒÆ’Ã‚Â©marrer l'appel: " + e.getMessage());
            endAudioCall();
        }
    }

    private void startAudioTransmission(String otherUser) {
        if (audioTransmission != null) {
            audioTransmission.stop();
        }

        try {
            // DÃƒÆ’Ã‚Â©marrer la transmission P2P
            audioTransmission = new AudioTransmissionService();
            audioTransmission.initiate(remoteHost, remotePort);

            showInfo("Connexion audio ÃƒÆ’Ã‚Â©tablie avec " + otherUser);

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
                .replace("ÃƒÂ¢Ã¢â‚¬â€œÃ‚Â¶", ">")
                .replace("ÃƒÂ¢Ã¢â‚¬â€œÃ‚Â ", "Stop")
                .replace("ÃƒÂ¢Ã¢â‚¬â€Ã‚Â", "")
                .replace("ÃƒÆ’Ã‚Â©", "e")
                .replace("ÃƒÆ’Ã‚Â¨", "e")
                .replace("ÃƒÆ’Ã‚Âª", "e")
                .replace("ÃƒÆ’Ã‚Â ", "a")
                .replace("ÃƒÆ’Ã‚Â§", "c")
                .replace("ÃƒÆ’Ã‚Â´", "o")
                .replace("ÃƒÆ’Ã‚Â®", "i")
                .replace("ÃƒÆ’Ã‚Â¢", "a")
                .replace("ÃƒÆ’Ã‚Â»", "u")
                .replace("ÃƒÆ’", "")
                .replace("Ã‚", "")
                .replace("Ã¢â‚¬â€œ", "-")
                .replace("Ã¢â‚¬â„¢", "'")
                .replace("Ã¢â‚¬Å“", "\"")
                .replace("Ã¢â‚¬Â", "\"")
                .replace("Ã¢â‚¬Â¦", "...")
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
                    networkClient.send(msg);
                } else {
                    ChatMessage msg = new ChatMessage(MessageType.PRIVATE_FILE, username, currentPrivateTarget, null, file.getName());
                    msg.setBinaryData(data);
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
        if (currentGroupId != -1) {
            startCurrentGroupCall("VIDEO");
            return;
        }
        String target = resolveSelectedPrivateTarget();
        if (target == null) {
            showInfo("Selectionnez un contact prive d'abord.");
            return;
        }
        currentPrivateTarget = target;
        if (!userStatuses.getOrDefault(target, "NON_CONNECTE").equals("EN_LIGNE")) {
            showInfo("Utilisateur hors ligne");
            return;
        }
        if (currentCallType != null) {
            showInfo("Un appel est deja en cours.");
            return;
        }

        ChatMessage callRequest = new ChatMessage(MessageType.CALL_REQUEST, username, target, null, "Demande d'appel video");
        callRequest.setCallType("VIDEO");
        networkClient.send(callRequest);
        showCallPending(target, "Appel video en cours...");
    }

    @FXML
    private void onStartVoiceCall() {
        if (currentGroupId != -1) {
            startCurrentGroupCall("AUDIO");
            return;
        }
        String target = resolveSelectedPrivateTarget();
        if (target == null) {
            showInfo("Selectionnez un contact prive d'abord.");
            return;
        }
        currentPrivateTarget = target;
        if (currentVoiceCall != null || currentCallType != null) {
            showInfo("Un appel est deja en cours.");
            return;
        }

        networkClient.send(new ChatMessage(MessageType.VOICE_CALL_REQUEST, username, target, null, "Demande d'appel audio"));

        showInfo("Appel audio demande a " + target);
    }
    private Window getWindow() {
        return messageField != null && messageField.getScene() != null ? messageField.getScene().getWindow() : null;
    }

    private void startAudioRecording() {
        AudioFormat format = new AudioFormat(16000, 16, 1, true, false);
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
        if (!AudioSystem.isLineSupported(info)) {
            showInfo("L'enregistrement audio n'est pas supportÃƒÆ’Ã‚Â© sur cet appareil.");
            return;
        }
        try {
            targetDataLine = (TargetDataLine) AudioSystem.getLine(info);
            targetDataLine.open(format);
            targetDataLine.start();
            recordingAudio = true;
            recordingAmplitudeData.clear();
            showRecordingWaveform(true);
            updateRecordButtonState();
            recordingThread = new Thread(() -> captureAudio(format));
            recordingThread.setDaemon(true);
            recordingThread.start();
        } catch (LineUnavailableException e) {
            showInfo("Impossible d'accÃƒÆ’Ã‚Â©der au micro: " + e.getMessage());
        }
    }

    private void stopAudioRecording() {
        recordingAudio = false;
        if (targetDataLine != null) {
            targetDataLine.stop();
            targetDataLine.close();
        }
        updateRecordButtonState();
        showRecordingWaveform(false);
    }

    private void captureAudio(AudioFormat format) {
        byte[] buffer = new byte[4096];
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            while (recordingAudio) {
                int count = targetDataLine.read(buffer, 0, buffer.length);
                if (count > 0) {
                    out.write(buffer, 0, count);
                    double amplitude = AudioCaptureService.calculateAmplitude(buffer, count);
                    recordingAmplitudeData.add(amplitude);
                    Platform.runLater(() -> {
                        if (recordingWaveform != null) recordingWaveform.setData(recordingAmplitudeData);
                    });
                }
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
                ChatMessage msg = new ChatMessage(MessageType.PRIVATE_AUDIO, username, currentPrivateTarget, currentConversationId, "Audio");
                msg.setBinaryData(Files.readAllBytes(tempFile));
                networkClient.send(msg);
            }
        } catch (IOException e) {
            showInfo("Impossible d'enregistrer l'audio: " + e.getMessage());
        }
    }

    private void updateRecordButtonState() {
        if (recordAudioButton == null) return;
        recordAudioButton.setText("");
        if (recordingAudio) {
            recordAudioButton.setGraphic(createActionIcon(STOP_ICON));
            if (!recordAudioButton.getStyleClass().contains("recording-icon-btn")) {
                recordAudioButton.getStyleClass().add("recording-icon-btn");
            }
            recordAudioButton.setTooltip(new Tooltip("Arreter l'enregistrement"));
        } else {
            recordAudioButton.setGraphic(createActionIcon(MIC_ICON));
            recordAudioButton.getStyleClass().remove("recording-icon-btn");
            recordAudioButton.setTooltip(new Tooltip("Message audio"));
        }
    }

    private SVGPath createActionIcon(String content) {
        SVGPath icon = new SVGPath();
        icon.setContent(content);
        icon.getStyleClass().add("whatsapp-action-icon");
        return icon;
    }

    private void showRecordingWaveform(boolean visible) {
        if (recordingWaveformBar == null) return;
        recordingWaveformBar.setVisible(visible);
        recordingWaveformBar.setManaged(visible);
        if (visible) {
            if (recordingWaveform == null) {
                recordingWaveform = new WaveformVisualizer(300, 36);
            }
            recordingWaveformBar.getChildren().setAll(recordingWaveform);
            recordingWaveform.setData(recordingAmplitudeData);
        } else {
            recordingWaveformBar.getChildren().clear();
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
                    setOnMousePressed(null);
                    setOnMouseReleased(null);
                    setOnMouseExited(null);
                    setOnMouseClicked(null);
                    return;
                }

                boolean isOwn = item.isOwn();
                HBox row = new HBox();
                row.getStyleClass().add("bubble-row");
                row.setAlignment(isOwn ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

                switch (item.getKind()) {
                    case SYSTEM -> {
                        row.setAlignment(Pos.CENTER);
                        row.getChildren().add(createSystemMessageBubble(item.getText()));
                    }
                    case TEXT -> {
                        String text = item.getText() == null ? "" : item.getText();

                        // Format localisation envoyÃƒÂ©: LAT=...;LON=...
                        boolean isLocation = text.startsWith("LAT=") && text.contains(";LON=");

                        if (isLocation) {
            String sep = ";LON=";
            int idxSep = text.indexOf(sep);
            if (idxSep < 0) {
                Label bubble = new Label(text);
                bubble.setWrapText(true);
                bubble.setMaxWidth(420);
                bubble.getStyleClass().add(isOwn ? "bubble-sent" : "bubble-received");

                Label time = new Label(messageFooter(item, isOwn));
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

                            Label time = new Label(messageFooter(item, isOwn));
                            time.getStyleClass().add(isOwn ? "timestamp-sent" : "timestamp-received");
                            content.getChildren().add(time);

                            content.setAlignment(isOwn ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
                            row.getChildren().add(content);
                        } else {
                            String sharedContact = extractSharedContactName(text);
                            if (sharedContact != null) {
                                SVGPath userIcon = new SVGPath();
                                userIcon.setContent("M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm0 3c1.66 0 3 1.34 3 3s-1.34 3-3 3-3-1.34-3-3 1.34-3 3-3zm0 14.2c-2.5 0-4.71-1.28-6-3.22.03-1.99 4-3.08 6-3.08 1.99 0 5.97 1.09 6 3.08-1.29 1.94-3.5 3.22-6 3.22z");
                                userIcon.setStyle("-fx-fill: " + (isOwn ? "white" : "#00a884") + ";");
                                
                                Label nameLabel = new Label(sharedContact);
                                nameLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: " + (isOwn ? "white" : "black") + "; -fx-font-size: 14px;");
                                
                                HBox topBox = new HBox(10, userIcon, nameLabel);
                                topBox.setAlignment(Pos.CENTER_LEFT);
                                
                                Button openBtn = new Button("Ouvrir");
                                openBtn.setStyle("-fx-background-color: transparent; -fx-border-color: " + (isOwn ? "white" : "#00a884") + "; -fx-border-radius: 4; -fx-text-fill: " + (isOwn ? "white" : "#00a884") + "; -fx-cursor: hand; -fx-font-weight: bold;");
                                openBtn.setMaxWidth(Double.MAX_VALUE);
                                openBtn.setOnAction(e -> {
                                    if (!allContacts.contains(sharedContact)) {
                                        allContacts.add(sharedContact);
                                    }
                                    openPrivateChat(sharedContact);
                                    if (tabPane != null && privateTab != null) {
                                        tabPane.getSelectionModel().select(privateTab);
                                    }
                                });
                                
                                VBox contactBox = new VBox(8, topBox, openBtn);
                                contactBox.getStyleClass().add(isOwn ? "bubble-sent" : "bubble-received");
                                contactBox.setPadding(new javafx.geometry.Insets(10));

                                Label time = new Label(messageFooter(item, isOwn));
                                time.getStyleClass().add(isOwn ? "timestamp-sent" : "timestamp-received");

                                VBox content = new VBox(2, contactBox, time);
                                content.setAlignment(isOwn ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
                                row.getChildren().add(content);
                                break;
                            }

                            Label bubble = new Label(text);
                            bubble.setWrapText(true);
                            bubble.setMaxWidth(420);
                            bubble.getStyleClass().add(isOwn ? "bubble-sent" : "bubble-received");

                            Label time = new Label(messageFooter(item, isOwn));
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

                        Label time = new Label(messageFooter(item, isOwn));
                        time.getStyleClass().add(isOwn ? "timestamp-sent" : "timestamp-received");

                        VBox content = new VBox(2, imageView, time);
                        content.setAlignment(isOwn ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
                        row.getChildren().add(content);
                    }
                    case AUDIO -> {
                        Button play = new Button("ÃƒÂ¢Ã¢â‚¬â€œÃ‚Â¶");
                        play.setText("");
                        play.setGraphic(createActionIcon(PLAY_ICON));
                        play.getStyleClass().add("btn-icon");
                        play.getStyleClass().add("media-icon-btn");
                        play.setDisable(item.getFilePath() == null);
                        WaveformVisualizer waveform = new WaveformVisualizer(220, 34);
                        waveform.setData(loadWaveformData(item.getFilePath()));
                        play.setOnAction(e -> playAudio(item.getFilePath(), waveform, play));

                        Label time = new Label(messageFooter(item, isOwn));
                        time.getStyleClass().add(isOwn ? "timestamp-sent" : "timestamp-received");

                        HBox inner = new HBox(10, play, waveform);
                        inner.getStyleClass().add(isOwn ? "media-card-sent" : "media-card-received");
                        inner.setAlignment(Pos.CENTER_LEFT);
                        VBox content = new VBox(2, inner, time);
                        content.setAlignment(isOwn ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
                        row.getChildren().add(content);
                    }
                    case FILE -> {
                        Node icon = FileIconResolver.getIconForFile(item.getText());
                        Label nameLabel = new Label("ÃƒÂ°Ã…Â¸Ã¢â‚¬Å“Ã…Â½ " + item.getText());
                        nameLabel.getStyleClass().add("file-name");
                        nameLabel.setText(item.getText() == null ? "Fichier" : item.getText());
                        Label sizeLabel = new Label(formatFileSize(item.getFilePath()));
                        sizeLabel.getStyleClass().add("file-size");
                        VBox fileInfo = new VBox(3, nameLabel, sizeLabel);
                        HBox.setHgrow(fileInfo, javafx.scene.layout.Priority.ALWAYS);
                        Button downloadBtn = new Button("ÃƒÂ°Ã…Â¸Ã¢â‚¬â„¢Ã‚Â¾");
                        downloadBtn.setText("");
                        downloadBtn.setGraphic(createActionIcon(DOWNLOAD_ICON));
                        downloadBtn.getStyleClass().add("btn-icon");
                        downloadBtn.getStyleClass().add("file-download-btn");
                        downloadBtn.getStyleClass().add("media-icon-btn");
                        downloadBtn.setDisable(item.getFilePath() == null);
                        downloadBtn.setOnAction(e -> downloadFile(item.getFilePath(), item.getText()));

                        Label time = new Label(messageFooter(item, isOwn));
                        time.getStyleClass().add(isOwn ? "timestamp-sent" : "timestamp-received");

                        HBox inner = new HBox(12, icon, fileInfo, downloadBtn);
                        inner.getStyleClass().add(isOwn ? "media-card-sent" : "media-card-received");
                        inner.setAlignment(Pos.CENTER_LEFT);
                        VBox content = new VBox(2, inner, time);
                        content.setAlignment(isOwn ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
                        row.getChildren().add(content);
                    }
                }
                setText(null);
                if (selectionMode && item.getKind() != UiMessage.Kind.SYSTEM) {
                    row.getChildren().add(0, createSelectionCheckBox(item));
                }
                setGraphic(row);
                row.getStyleClass().remove("message-selection-row");
                if (selectedMessages.contains(item) || item.isSelected()) {
                    row.getStyleClass().add("message-selection-row");
                }

                PauseTransition longPress = new PauseTransition(Duration.millis(500));
                longPress.setOnFinished(event -> enterSelectionMode(item));
                setOnMousePressed(event -> longPress.playFromStart());
                setOnMouseReleased(event -> longPress.stop());
                setOnMouseExited(event -> longPress.stop());
                setOnMouseClicked(event -> {
                    if (selectionMode) {
                        toggleMessageSelection(item);
                        event.consume();
                    }
                });
            }
        });
    }

    private CheckBox createSelectionCheckBox(UiMessage item) {
        CheckBox checkBox = new CheckBox();
        checkBox.getStyleClass().add("message-select-check");
        checkBox.setSelected(selectedMessages.contains(item) || item.isSelected());
        checkBox.setFocusTraversable(false);
        checkBox.setOnAction(event -> {
            toggleMessageSelection(item);
            event.consume();
        });
        checkBox.setOnMouseClicked(event -> event.consume());
        return checkBox;
    }

    private String messageFooter(UiMessage item, boolean isOwn) {
        String time = item.getTimestamp() == null ? "" : item.getTimestamp();
        if (!isOwn) return time;
        return time + "  " + (item.isRead() ? "Lu" : "Envoye");
    }

    private Node createSystemMessageBubble(String text) {
        HBox container = new HBox();
        container.setAlignment(Pos.CENTER);
        container.setPadding(new Insets(5, 0, 5, 0));

        Label label = new Label(text == null ? "" : text);
        label.setWrapText(true);
        label.setMaxWidth(420);
        label.setStyle("-fx-background-color: #1f2c34; -fx-text-fill: #8696a0; "
                + "-fx-padding: 5 12; -fx-background-radius: 8; -fx-font-size: 12;");
        container.getChildren().add(label);
        return container;
    }

    private void updateCallButtonsVisibility() {
        boolean canCall = resolveSelectedPrivateTarget() != null || currentGroupId != -1;
        if (voiceCallButton != null) {
            voiceCallButton.setVisible(canCall);
            voiceCallButton.setManaged(canCall);
            voiceCallButton.setDisable(!canCall);
        }
        if (videoCallButton != null) {
            videoCallButton.setVisible(canCall);
            videoCallButton.setManaged(canCall);
            videoCallButton.setDisable(!canCall);
        }
    }

    private String resolveSelectedPrivateTarget() {
        if (currentPrivateTarget != null && !currentPrivateTarget.isBlank()) {
            return currentPrivateTarget;
        }
        if (privateListView != null) {
            String selected = privateListView.getSelectionModel().getSelectedItem();
            if (selected != null && !selected.isBlank()) {
                currentPrivateTarget = selected;
                return selected;
            }
        }
        if (contactsTabListView != null) {
            String selected = contactsTabListView.getSelectionModel().getSelectedItem();
            if (selected != null && !selected.isBlank()) {
                currentPrivateTarget = selected;
                return selected;
            }
        }
        return null;
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
            boolean own = msg.getFrom().equals(username);
            UiMessage uiMessage = new UiMessage(UiMessage.Kind.AUDIO, own, "Audio", path, time, msg.getMessageId());
            uiMessage.setRead(msg.isRead());
            privateConversations.get(other).add(uiMessage);
            if (other.equals(currentPrivateTarget)) {
                if (!own) markMessageRead(msg);
                messagesListView.setItems(privateConversations.get(other));
            }
            if (!privateListView.getItems().contains(other)) privateListView.getItems().add(other);
        } catch (IOException e) { showInfo("Impossible de sauvegarder l'audio reÃƒÆ’Ã‚Â§u."); }
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
            boolean own = msg.getFrom().equals(username);
            UiMessage uiMessage = new UiMessage(UiMessage.Kind.IMAGE, own, "", path, time, msg.getMessageId());
            uiMessage.setRead(msg.isRead());
            privateConversations.get(other).add(uiMessage);
            if (other.equals(currentPrivateTarget)) {
                if (!own) markMessageRead(msg);
                messagesListView.setItems(privateConversations.get(other));
            }
            if (!privateListView.getItems().contains(other)) privateListView.getItems().add(other);
        } catch (IOException e) { showInfo("Impossible de sauvegarder l'image reÃƒÆ’Ã‚Â§ue."); }
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
            boolean own = msg.getFrom().equals(username);
            UiMessage uiMessage = new UiMessage(UiMessage.Kind.FILE, own, name != null ? name : "Fichier", path, time, msg.getMessageId());
            uiMessage.setRead(msg.isRead());
            privateConversations.get(other).add(uiMessage);
            if (other.equals(currentPrivateTarget)) {
                if (!own) markMessageRead(msg);
                messagesListView.setItems(privateConversations.get(other));
            }
            if (!privateListView.getItems().contains(other)) privateListView.getItems().add(other);
        } catch (IOException e) { showInfo("Impossible de sauvegarder le fichier reÃƒÆ’Ã‚Â§u."); }
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

    private void playAudio(String path, WaveformVisualizer waveform, Button playButton) {
        if (path == null) return;
        new Thread(() -> {
            try (AudioInputStream ais = AudioSystem.getAudioInputStream(new File(path))) {
                Clip clip = AudioSystem.getClip();
                clip.open(ais);
                Platform.runLater(() -> playButton.setText("||"));
                clip.start();
                int bars = Math.max(1, loadWaveformData(path).size());
                while (clip.isOpen() && clip.isRunning()) {
                    int pos = (int) ((clip.getMicrosecondPosition() / (double) Math.max(1, clip.getMicrosecondLength())) * bars);
                    Platform.runLater(() -> waveform.setPlaybackPosition(pos));
                    Thread.sleep(60);
                }
                Platform.runLater(() -> {
                    waveform.setPlaybackPosition(bars);
                    playButton.setText(">");
                });
                clip.close();
            } catch (Exception e) {
                Platform.runLater(() -> showInfo("Impossible de lire l'audio."));
            }
        }, "WaveformAudioPlayback").start();
    }

    private List<Double> loadWaveformData(String path) {
        if (path == null) return defaultWaveformData();
        try (AudioInputStream stream = AudioSystem.getAudioInputStream(new File(path))) {
            byte[] bytes = stream.readAllBytes();
            List<Double> amplitudes = new ArrayList<>();
            int chunkSize = Math.max(512, bytes.length / 48);
            for (int i = 0; i < bytes.length; i += chunkSize) {
                amplitudes.add(AudioCaptureService.calculateAmplitude(bytes, Math.min(chunkSize, bytes.length - i)));
            }
            return amplitudes.isEmpty() ? defaultWaveformData() : amplitudes;
        } catch (Exception e) {
            return defaultWaveformData();
        }
    }

    private List<Double> defaultWaveformData() {
        List<Double> values = new ArrayList<>();
        for (int i = 0; i < 36; i++) {
            values.add(0.18 + (Math.sin(i * 0.55) + 1) * 0.28);
        }
        return values;
    }

    private String formatFileSize(String path) {
        if (path == null) {
            return "Taille inconnue";
        }
        try {
            long bytes = Files.size(Path.of(path));
            if (bytes < 1024) {
                return bytes + " B";
            }
            double kb = bytes / 1024.0;
            if (kb < 1024) {
                return new DecimalFormat("#,##0.#").format(kb) + " KB";
            }
            double mb = kb / 1024.0;
            if (mb < 1024) {
                return new DecimalFormat("#,##0.#").format(mb) + " MB";
            }
            return new DecimalFormat("#,##0.#").format(mb / 1024.0) + " GB";
        } catch (IOException | RuntimeException e) {
            return "Taille inconnue";
        }
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
            showInfo("Fichier sauvegardÃƒÆ’Ã‚Â© : " + dest.getAbsolutePath());
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
        showContactInfo(currentPrivateTarget);
    }

    private void showContactInfo(String contact) {
        if (contact == null || contact.isBlank()) {
            showInfo("Selectionnez un contact d'abord.");
            return;
        }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Infos du contact");
        dialog.setHeaderText(contact);

        String status = userStatuses.getOrDefault(contact, "NON_CONNECTE");
        String email = "Non disponible";
        try {
            Utilisateur utilisateur = utilisateurDAO.findByUsername(contact);
            if (utilisateur != null && utilisateur.getEmail() != null && !utilisateur.getEmail().isBlank()) {
                email = utilisateur.getEmail();
            }
        } catch (SQLException ignored) {
        }

        VBox content = new VBox(10,
                new Label("Nom : " + contact),
                new Label("Email : " + email),
                new Label("Statut : " + ("EN_LIGNE".equals(status) ? "En ligne" : "Non connecte"))
        );
        content.setPadding(new Insets(12));
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.showAndWait();
    }

    @FXML
    private void onMoreOptions() {
        ContextMenu menu = new ContextMenu();
        menu.getStyleClass().add("options-popup-menu");

        UiMessage selectedMessage = messagesListView == null ? null : messagesListView.getSelectionModel().getSelectedItem();
        int selectedCount = messagesListView == null ? 0 : messagesListView.getSelectionModel().getSelectedItems().size();
        if (selectedCount > 1) {
            menu.getItems().add(menuOption("\u232B", "Supprimer la selection pour moi", this::deleteSelectedMessagesForMe));
            menu.getItems().add(menuOption("\uD83D\uDDD1", "Supprimer la selection pour tous", this::deleteSelectedMessagesForEveryone));
            menu.getItems().add(new SeparatorMenuItem());
        }
        if (selectedCount <= 1 && selectedMessage != null && selectedMessage.getMessageId() > 0) {
            menu.getItems().add(menuOption("\u232B", "Supprimer pour moi", this::deleteSelectedMessageForMe));
            menu.getItems().add(menuOption("\uD83D\uDDD1", "Supprimer pour tous", this::deleteSelectedMessageForEveryone));
            menu.getItems().add(new SeparatorMenuItem());
        }

        if (currentGroupId != -1) {
            menu.getItems().add(menuOption("+", "Ajouter un membre", this::openAddGroupMemberDialog));
            menu.getItems().add(menuOption("-", "Supprimer un membre", this::showRemoveGroupMemberDialog));
            menu.getItems().add(menuOption("i", "Infos du groupe", this::showGroupInfo));
            menu.getItems().add(menuOption("\uD83C\uDFA4", "Appel audio du groupe", () -> startCurrentGroupCall("AUDIO")));
            menu.getItems().add(menuOption("\u25A3", "Appel video du groupe", () -> startCurrentGroupCall("VIDEO")));
            menu.getItems().add(menuOption("\u2611", "Selectionner des messages", this::enableMultiMessageSelection));
            menu.getItems().add(new SeparatorMenuItem());
            menu.getItems().add(menuOption("\u232B", "Effacer la conversation", this::clearCurrentChatForMe));
            menu.getItems().add(menuOption("\u21B5", "Quitter le groupe", this::leaveCurrentGroup));
        } else {
            menu.getItems().add(menuOption("i", "Infos du contact", this::onShowContactInfo));
            menu.getItems().add(menuOption("\u2611", "Selectionner des messages", this::enableMultiMessageSelection));
            menu.getItems().add(menuOption("\u232B", "Effacer la conversation", this::clearCurrentChatForMe));
            if (currentPrivateTarget != null) {
                menu.getItems().add(menuOption("\u2715", "Supprimer ce contact",
                        () -> deleteContactWithConfirm(currentPrivateTarget)));
            }
        }

        Node anchor = moreOptionsButton != null ? moreOptionsButton : rootPane;
        menu.show(anchor, Side.BOTTOM, -260, 4);
    }

    private MenuItem menuOption(String icon, String text, Runnable action) {
        Label iconLabel = new Label(icon);
        iconLabel.getStyleClass().add("options-menu-icon");
        MenuItem item = new MenuItem(text, iconLabel);
        item.getStyleClass().add("options-menu-item");
        item.setOnAction(e -> action.run());
        return item;
    }

    private ContextMenu createContactContextMenu(String contact) {
        ContextMenu menu = new ContextMenu();
        MenuItem infoItem = new MenuItem("Infos du contact", new Label("i"));
        infoItem.setOnAction(e -> showContactInfo(contact));
        MenuItem openItem = new MenuItem("Ouvrir la conversation");
        openItem.setOnAction(e -> {
            openPrivateChat(contact);
            if (tabPane != null && privateTab != null) tabPane.getSelectionModel().select(privateTab);
        });
        MenuItem clearItem = new MenuItem("Effacer la discussion");
        clearItem.setOnAction(e -> {
            openPrivateChat(contact);
            clearCurrentChatForMe();
        });
        MenuItem deleteItem = new MenuItem("Supprimer le contact");
        deleteItem.setOnAction(e -> deleteContactWithConfirm(contact));
        menu.getItems().addAll(infoItem, openItem, clearItem, new SeparatorMenuItem(), deleteItem);
        return menu;
    }

    private void deleteContactWithConfirm(String contact) {
        if (contact == null || contact.isBlank()) return;
        Alert confirm = new Alert(
                Alert.AlertType.CONFIRMATION,
                "Supprimer " + contact + " de vos contacts ?",
                ButtonType.YES,
                ButtonType.NO
        );
        confirm.setHeaderText("Supprimer le contact");
        if (confirm.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) return;

        try {
            Utilisateur contactUser = utilisateurDAO.findByUsername(contact);
            if (contactUser != null) {
                networkClient.sendDeleteContact(contactUser.getId());
            } else {
                networkClient.deleteContact(contact);
            }
        } catch (SQLException e) {
            networkClient.deleteContact(contact);
        }
        allContacts.remove(contact);
        privateConversations.remove(contact);
        if (contact.equals(currentPrivateTarget)) {
            currentPrivateTarget = null;
            messagesListView.setItems(FXCollections.observableArrayList());
            chatTitleLabel.setText("Selectionnez une conversation");
            chatStatusLabel.setText("");
            chatAvatarLabel.setText("?");
            updateCallButtonsVisibility();
        }
        privateListView.refresh();
        if (contactsTabListView != null) contactsTabListView.refresh();
    }

    private void showRemoveGroupMemberDialog() {
        if (currentGroupId == -1) {
            showInfo("Selectionnez un groupe d'abord.");
            return;
        }

        networkClient.requestGroupMembers(currentGroupId);
        List<String> members = parseGroupMemberNames(groupMembersById.get(currentGroupId));
        if (members.isEmpty()) {
            members = new ArrayList<>(allContacts);
        }
        members.remove(username);

        if (members.isEmpty()) {
            showInfo("Aucun membre disponible a supprimer.");
            return;
        }

        ChoiceDialog<String> dialog = new ChoiceDialog<>(members.get(0), members);
        dialog.setTitle("Supprimer un membre");
        dialog.setHeaderText("Action administrateur");
        dialog.setContentText("Membre :");
        dialog.showAndWait().ifPresent(member -> {
            Alert confirm = new Alert(
                    Alert.AlertType.CONFIRMATION,
                    "Retirer " + member + " du groupe " + currentGroupName + " ?",
                    ButtonType.YES,
                    ButtonType.NO
            );
            confirm.setHeaderText("Supprimer un membre");
            if (confirm.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) {
                networkClient.removeGroupMember(currentGroupId, member);
            }
        });
    }

    private List<String> parseGroupMemberNames(String rawMembers) {
        List<String> members = new ArrayList<>();
        if (rawMembers == null || rawMembers.isBlank()) return members;
        for (String entry : rawMembers.split(",")) {
            String[] parts = entry.split(":");
            if (parts.length >= 2 && !parts[1].trim().isBlank()) {
                members.add(parts[1].trim());
            }
        }
        return members;
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

    public void showGroupMembers(int groupId, String groupName) {
        if (networkClient != null) {
            networkClient.requestGroupMembers(groupId);
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/group-info-dialog.fxml"));
            VBox content = loader.load();
            GroupInfoDialogController controller = loader.getController();
            controller.init(groupName, "Membres du groupe", groupMembersById.get(groupId));

            javafx.scene.Scene scene = new javafx.scene.Scene(content);
            scene.setFill(javafx.scene.paint.Color.TRANSPARENT);

            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.initStyle(javafx.stage.StageStyle.TRANSPARENT);
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            showInfo("Impossible d'ouvrir les membres du groupe.");
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

    private void markMessageRead(ChatMessage msg) {
        if (networkClient != null && msg != null && msg.getMessageId() > 0 && !username.equals(msg.getFrom())) {
            networkClient.markMessageRead(msg.getMessageId());
        }
    }

    private void markVisibleMessagesRead(String other, ObservableList<UiMessage> messages) {
        if (networkClient == null || other == null || messages == null) return;
        for (UiMessage message : messages) {
            if (!message.isOwn() && message.getMessageId() > 0) {
                networkClient.markMessageRead(message.getMessageId());
            }
        }
    }

    private void markLocalMessageRead(int messageId) {
        if (messageId <= 0) return;
        privateConversations.values().forEach(messages -> {
            for (UiMessage message : messages) {
                if (message.getMessageId() == messageId) {
                    message.setRead(true);
                }
            }
        });
        if (messagesListView != null) messagesListView.refresh();
    }

    @FXML
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

    private void clearCurrentChatForMe() {
        if (currentGroupId != -1 && groupConversations.containsKey(currentGroupId)) {
            networkClient.clearGroupChat(currentGroupId);
            clearGroupMessagesLocally(currentGroupId);
            showInfo("Conversation effacee pour ce compte.");
            return;
        }
        if (currentPrivateTarget != null && privateConversations.containsKey(currentPrivateTarget)) {
            networkClient.clearPrivateChat(currentPrivateTarget);
            clearPrivateMessagesLocally(currentPrivateTarget);
            showInfo("Conversation effacee pour ce compte.");
        }
    }

    private void deleteSelectedMessageForMe() {
        UiMessage selected = messagesListView.getSelectionModel().getSelectedItem();
        if (selected == null || selected.getMessageId() <= 0) {
            showInfo("Selectionnez un message d'abord.");
            return;
        }
        networkClient.clearMessageForMe(selected.getMessageId());
        removeMessageLocally(selected.getMessageId());
        showInfo("Message supprime pour moi.");
    }

    private void deleteSelectedMessageForEveryone() {
        UiMessage selected = messagesListView.getSelectionModel().getSelectedItem();
        if (selected == null || selected.getMessageId() <= 0) {
            showInfo("Selectionnez un message d'abord.");
            return;
        }
        Alert confirm = new Alert(
                Alert.AlertType.CONFIRMATION,
                "Supprimer ce message pour tous les participants ?",
                ButtonType.YES,
                ButtonType.NO
        );
        confirm.setHeaderText("Supprimer le message");
        if (confirm.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) return;

        networkClient.sendDeleteMessage(selected.getMessageId());
        removeMessageLocally(selected.getMessageId());
        showInfo("Message supprime pour tout le monde.");
    }

    private void enableMultiMessageSelection() {
        enterSelectionMode(messagesListView == null ? null : messagesListView.getSelectionModel().getSelectedItem());
    }

    private void enterSelectionMode(UiMessage firstMessage) {
        if (messagesListView == null) return;
        selectionMode = true;
        multiMessageSelectionMode = true;
        messagesListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        setSelectionBarVisible(true);
        if (firstMessage != null) toggleMessageSelection(firstMessage);
        updateSelectionBar();
    }

    private void toggleMessageSelection(UiMessage message) {
        if (message == null) return;
        if (!selectionMode) enterSelectionMode(message);
        boolean selected = selectedMessages.contains(message);
        message.setSelected(!selected);
        if (selected) {
            selectedMessages.remove(message);
        } else {
            selectedMessages.add(message);
        }
        updateSelectionBar();
        messagesListView.refresh();
    }

    private void selectAllMessages() {
        ObservableList<UiMessage> messages = currentVisibleMessages();
        if (messages == null || messages.isEmpty()) return;
        if (!selectionMode) enterSelectionMode(null);
        for (UiMessage message : messages) {
            message.setSelected(true);
            selectedMessages.add(message);
        }
        updateSelectionBar();
        messagesListView.refresh();
    }

    private void copySelectedMessages() {
        String text = selectedMessages.stream()
                .map(this::buildSearchPreview)
                .filter(value -> value != null && !value.isBlank())
                .collect(Collectors.joining(System.lineSeparator()));
        ClipboardContent content = new ClipboardContent();
        content.putString(text);
        Clipboard.getSystemClipboard().setContent(content);
        showInfo(selectedMessages.size() + " message(s) copie(s).");
    }

    private void forwardSelectedMessages() {
        if (selectedMessages.isEmpty()) {
            showInfo("Selectionnez au moins un message.");
            return;
        }
        List<String> targets = new ArrayList<>(allContacts);
        for (int i = 0; i < groupNames.size(); i++) {
            targets.add("[Groupe] " + groupNames.get(i));
        }
        if (targets.isEmpty()) {
            showInfo("Aucun contact ou groupe disponible.");
            return;
        }

        ChoiceDialog<String> dialog = new ChoiceDialog<>(targets.get(0), targets);
        dialog.setTitle("Transferer");
        dialog.setHeaderText("Choisissez une destination");
        dialog.setContentText("Destination :");
        dialog.showAndWait().ifPresent(target -> {
            for (UiMessage message : selectedMessages) {
                String text = buildSearchPreview(message);
                byte[] binaryData = null;
                MessageType pType = MessageType.PRIVATE;
                MessageType gType = MessageType.GROUP_MESSAGE;
                
                if (message.getKind() == UiMessage.Kind.AUDIO || message.getKind() == UiMessage.Kind.IMAGE || message.getKind() == UiMessage.Kind.FILE) {
                    if (message.getFilePath() != null) {
                        try {
                            java.io.File f = new java.io.File(message.getFilePath());
                            if (f.exists()) {
                                binaryData = java.nio.file.Files.readAllBytes(f.toPath());
                                text = f.getName();
                                if (message.getKind() == UiMessage.Kind.AUDIO) {
                                    pType = MessageType.PRIVATE_AUDIO;
                                    gType = MessageType.GROUP_AUDIO;
                                } else if (message.getKind() == UiMessage.Kind.IMAGE) {
                                    pType = MessageType.PRIVATE_IMAGE;
                                    gType = MessageType.GROUP_IMAGE;
                                } else {
                                    pType = MessageType.PRIVATE_FILE;
                                    gType = MessageType.GROUP_FILE;
                                }
                            }
                        } catch (java.io.IOException e) {
                            e.printStackTrace();
                        }
                    }
                }

                if (target.startsWith("[Groupe] ")) {
                    int index = groupNames.indexOf(target.substring("[Groupe] ".length()));
                    if (index >= 0 && index < groupIds.size()) {
                        int gId = groupIds.get(index)[0];
                        if (binaryData != null) {
                            networkClient.sendGroupMedia(gId, gType, text, binaryData);
                        } else {
                            networkClient.sendGroupMessage(gId, text);
                        }
                    }
                } else {
                    ChatMessage msg = new ChatMessage(pType, username, target, currentConversationId, text);
                    if (binaryData != null) msg.setBinaryData(binaryData);
                    networkClient.send(msg);
                }
            }
            showInfo(selectedMessages.size() + " message(s) transfere(s).");
            exitSelectionMode();
        });
    }

    private List<Integer> selectedMessageIds() {
        return selectedMessages.stream()
                .map(UiMessage::getMessageId)
                .filter(id -> id > 0)
                .distinct()
                .collect(Collectors.toList());
    }

    private void deleteSelectedMessagesForMe() {
        List<Integer> ids = selectedMessageIds();
        if (ids.isEmpty()) {
            showInfo("Selectionnez au moins un message.");
            return;
        }
        saveCurrentMessageSelection(ids);
        ids.forEach(id -> {
            networkClient.clearMessageForMe(id);
            removeMessageLocally(id);
        });
        disableMultiMessageSelection();
        showInfo(ids.size() + " message(s) supprime(s) pour moi.");
    }

    private void deleteSelectedMessagesForEveryone() {
        List<Integer> ids = selectedMessageIds();
        if (ids.isEmpty()) {
            showInfo("Selectionnez au moins un message.");
            return;
        }
        Alert confirm = new Alert(
                Alert.AlertType.CONFIRMATION,
                "Supprimer " + ids.size() + " message(s) pour tous les participants ?",
                ButtonType.YES,
                ButtonType.NO
        );
        confirm.setHeaderText("Supprimer la selection");
        if (confirm.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) return;

        saveCurrentMessageSelection(ids);
        ids.forEach(id -> {
            networkClient.sendDeleteMessage(id);
            removeMessageLocally(id);
        });
        disableMultiMessageSelection();
        showInfo(ids.size() + " message(s) supprime(s) pour tout le monde.");
    }

    private void saveCurrentMessageSelection(List<Integer> ids) {
        if (currentUser == null || ids == null || ids.isEmpty()) return;
        try {
            messageDAO.saveMessageSelection(currentUser.getId(), ids, UUID.randomUUID().toString());
        } catch (SQLException e) {
            showInfo("Selection sauvegardee localement, mais pas en BD : " + e.getMessage());
        }
    }

    private void disableMultiMessageSelection() {
        exitSelectionMode();
    }

    private void exitSelectionMode() {
        if (messagesListView == null) return;
        selectedMessages.forEach(message -> message.setSelected(false));
        selectedMessages.clear();
        selectionMode = false;
        multiMessageSelectionMode = false;
        messagesListView.getSelectionModel().clearSelection();
        messagesListView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        setSelectionBarVisible(false);
        updateSelectionBar();
        messagesListView.refresh();
    }

    private void setSelectionBarVisible(boolean visible) {
        if (selectionBar != null) {
            selectionBar.setVisible(visible);
            selectionBar.setManaged(visible);
        }
    }

    private void updateSelectionBar() {
        if (selectionBarController != null) {
            selectionBarController.setSelectedCount(selectedMessages.size());
        }
        if (selectionMode && selectedMessages.isEmpty()) {
            setSelectionBarVisible(true);
        }
    }

    private ObservableList<UiMessage> currentVisibleMessages() {
        if (currentGroupId != -1) return groupConversations.get(currentGroupId);
        if (currentPrivateTarget != null) return privateConversations.get(currentPrivateTarget);
        return messagesListView == null ? null : messagesListView.getItems();
    }

    private void removeMessageLocally(int messageId) {
        if (messageId <= 0) return;
        privateConversations.values().forEach(messages -> messages.removeIf(message -> message.getMessageId() == messageId));
        groupConversations.values().forEach(messages -> messages.removeIf(message -> message.getMessageId() == messageId));
        messagesListView.refresh();
    }

    private void deleteCurrentChatForEveryone() {
        if (!confirmDeleteForEveryone()) return;

        if (currentGroupId != -1 && groupConversations.containsKey(currentGroupId)) {
            int groupId = currentGroupId;
            networkClient.deleteGroupChatForEveryone(groupId);
            clearGroupMessagesLocally(groupId);
            showInfo("Discussion supprimee pour tout le monde.");
            return;
        }
        if (currentPrivateTarget != null && privateConversations.containsKey(currentPrivateTarget)) {
            String target = currentPrivateTarget;
            networkClient.deletePrivateChatForEveryone(target);
            clearPrivateMessagesLocally(target);
            showInfo("Discussion supprimee pour tout le monde.");
        }
    }

    private boolean confirmDeleteForEveryone() {
        Alert confirm = new Alert(
                Alert.AlertType.CONFIRMATION,
                "Cette action supprime les messages pour tous les participants. Continuer ?",
                ButtonType.YES,
                ButtonType.NO
        );
        confirm.setHeaderText("Supprimer pour tout le monde");
        return confirm.showAndWait().orElse(ButtonType.NO) == ButtonType.YES;
    }

    private void clearPrivateMessagesLocally(String contact) {
        ObservableList<UiMessage> messages = privateConversations.get(contact);
        if (messages != null) messages.clear();
        if (contact != null && contact.equals(currentPrivateTarget)) {
            messagesListView.setItems(messages != null ? messages : FXCollections.observableArrayList());
        }
    }

    private void clearGroupMessagesLocally(int groupId) {
        ObservableList<UiMessage> messages = groupConversations.get(groupId);
        if (messages != null) messages.clear();
        if (groupId == currentGroupId) {
            messagesListView.setItems(messages != null ? messages : FXCollections.observableArrayList());
        }
    }

    private void handlePrivateChatDeletedForEveryone(ChatMessage msg) {
        String other = username != null && username.equals(msg.getFrom()) ? msg.getTo() : msg.getFrom();
        if (other == null || "SERVER".equals(other)) {
            other = msg.getContent();
        }
        clearPrivateMessagesLocally(other);
        if (other != null && other.equals(currentPrivateTarget)) {
            showInfo("La discussion a ete supprimee pour tout le monde.");
        }
    }

    private void handleGroupChatDeletedForEveryone(ChatMessage msg) {
        clearGroupMessagesLocally(msg.getGroupId());
        if (msg.getGroupId() == currentGroupId) {
            showInfo("La discussion du groupe a ete supprimee pour tout le monde.");
        }
    }

    @FXML
    private void onUploadImage() {
        onUploadFile();
    }

    @FXML
    private void onShareContact() {
        if (isSendingContact) {
            return;
        }
        if (allContacts.isEmpty()) {
            showInfo("Aucun contact disponible a partager.");
            return;
        }
        isSendingContact = true;

        ChoiceDialog<String> dialog = new ChoiceDialog<>(allContacts.get(0), allContacts);
        dialog.setTitle("Partager un contact");
        dialog.setHeaderText("Choisissez un contact a partager");
        dialog.setContentText("Contact :");

        Optional<String> choice = dialog.showAndWait();
        if (choice.isEmpty()) {
            resetSendingContactGuard();
            return;
        }

        String sharedContact = choice.get();
        String text = "Contact partage: " + sharedContact;
        if (currentGroupId != -1) {
            networkClient.sendGroupMessage(currentGroupId, text);
            resetSendingContactGuard();
            return;
        }
        if (currentPrivateTarget == null) {
            showInfo("Selectionnez une conversation d'abord.");
            resetSendingContactGuard();
            return;
        }
        ChatMessage msg = new ChatMessage(MessageType.PRIVATE, username, currentPrivateTarget, currentConversationId, text);
        networkClient.send(msg);
        resetSendingContactGuard();
    }

    private void resetSendingContactGuard() {
        PauseTransition pause = new PauseTransition(Duration.seconds(1));
        pause.setOnFinished(e -> isSendingContact = false);
        pause.play();
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
