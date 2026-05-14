package org.example.tpchatjavafx.client.controller;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import org.example.tpchatjavafx.client.NetworkClient;
import org.example.tpchatjavafx.client.util.UiMessage;
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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GroupController extends javafx.scene.control.SplitPane {

    private NetworkClient networkClient;
    private String username;

    private final List<int[]> groupIds = new ArrayList<>();
    private final ObservableList<String> groupNames = FXCollections.observableArrayList();
    private final Map<Integer, ObservableList<UiMessage>> groupMessages = new HashMap<>();
    private final java.util.Set<Integer> activeMeetingGroupIds = new java.util.HashSet<>();

    private int selectedGroupId = -1;
    private String selectedGroupName = "";

    private final DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm");
    private List<String> allContacts = new ArrayList<>();

    private ListView<String> groupListView;
    private VBox chatArea;
    private ScrollPane chatScroll;
    private TextField txtGroupMsg;
    private Label lblGroupName;
    private Label lblGroupStatus;
    private Label emptyStateLabel;
    private Button btnAddMember;
    private Button btnMeeting;
    private Button btnLeaveGroup;
    private Button btnSend;
    private Button btnAttach;
    private Button btnImage;
    private Button btnAudio;
    private VBox membersBox;
    private Label membersTitle;
    private boolean currentUserAdmin;
    private volatile boolean recordingAudio;
    private TargetDataLine targetDataLine;

    public GroupController() {
        buildUI();
    }

    public void init(NetworkClient networkClient, String username, List<String> allContacts) {
        this.networkClient = networkClient;
        this.username = username;
        this.allContacts = allContacts;
        registerCallbacks();
        networkClient.requestGroupList();
    }

    private void buildUI() {
        getStyleClass().add("group-root");

        VBox left = new VBox(12);
        left.setPadding(new Insets(14));
        left.getStyleClass().addAll("sidebar", "group-sidebar");
        left.setPrefWidth(270);
        left.setMinWidth(230);
        left.setMaxWidth(320);

        Label lblTitle = new Label("Groupes");
        lblTitle.getStyleClass().add("group-panel-title");
        Label lblSubtitle = new Label("Conversations et reunions d'equipe");
        lblSubtitle.getStyleClass().add("group-panel-subtitle");
        VBox heading = new VBox(2, lblTitle, lblSubtitle);

        Button btnNew = new Button("+ Nouveau groupe");
        btnNew.getStyleClass().addAll("send-btn", "group-primary-btn");
        btnNew.setMaxWidth(Double.MAX_VALUE);
        btnNew.setOnAction(e -> ouvrirCreationGroupe());

        groupListView = new ListView<>(groupNames);
        groupListView.getStyleClass().addAll("contact-list", "group-list");
        groupListView.setPlaceholder(new Label("Aucun groupe pour le moment"));
        groupListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }

                Label avatarText = new Label(initialFor(item));
                avatarText.getStyleClass().add("group-avatar-letter");
                StackPane avatar = new StackPane(avatarText);
                avatar.getStyleClass().add("group-avatar");

                Label name = new Label(item);
                name.getStyleClass().add("group-cell-name");
                
                int gid = -1;
                int idx = getIndex();
                if (idx >= 0 && idx < groupIds.size()) gid = groupIds.get(idx)[0];
                
                Label detail = new Label(activeMeetingGroupIds.contains(gid) ? "● Reunion en cours" : "Groupe de discussion");
                detail.getStyleClass().add("group-cell-detail");
                if (activeMeetingGroupIds.contains(gid)) detail.setStyle("-fx-text-fill: #25D366; -fx-font-weight: bold;");

                VBox copy = new VBox(2, name, detail);
                HBox.setHgrow(copy, Priority.ALWAYS);

                HBox row = new HBox(10, avatar, copy);
                row.setAlignment(Pos.CENTER_LEFT);
                row.getStyleClass().add("group-cell-content");

                setText(null);
                setGraphic(row);
            }
        });
        groupListView.getSelectionModel().selectedIndexProperty().addListener((obs, old, idx) -> {
            int i = idx.intValue();
            if (i >= 0 && i < groupIds.size()) {
                selectionnerGroupe(groupIds.get(i)[0], groupNames.get(i));
            }
        });

        VBox.setVgrow(groupListView, Priority.ALWAYS);
        left.getChildren().addAll(heading, btnNew, groupListView);

        VBox right = new VBox();
        right.getStyleClass().add("chat-area");

        HBox topBar = new HBox(10);
        topBar.getStyleClass().addAll("chat-top-bar", "group-top-bar");
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(12, 16, 12, 16));

        Label groupAvatarText = new Label("#");
        groupAvatarText.getStyleClass().add("group-header-avatar-letter");
        StackPane groupAvatar = new StackPane(groupAvatarText);
        groupAvatar.getStyleClass().add("group-header-avatar");

        lblGroupName = new Label("Selectionnez un groupe");
        lblGroupName.getStyleClass().add("chat-contact-name");
        lblGroupStatus = new Label("Choisissez un groupe pour envoyer des messages.");
        lblGroupStatus.getStyleClass().add("chat-contact-status");
        VBox headerCopy = new VBox(2, lblGroupName, lblGroupStatus);
        HBox.setHgrow(headerCopy, Priority.ALWAYS);

        btnAddMember = new Button("+ Membre");
        btnAddMember.getStyleClass().addAll("call-btn", "group-action-btn");
        btnAddMember.setTooltip(new Tooltip("Ajouter un membre"));
        btnAddMember.setOnAction(e -> ouvrirAjoutMembre());

        btnMeeting = new Button("Reunion");
        btnMeeting.getStyleClass().addAll("call-btn", "group-action-btn");
        btnMeeting.setTooltip(new Tooltip("Demarrer une reunion"));
        btnMeeting.setOnAction(e -> demarrerReunion());

        btnLeaveGroup = new Button("Quitter groupe");
        btnLeaveGroup.getStyleClass().addAll("call-btn", "group-danger-btn");
        btnLeaveGroup.setTooltip(new Tooltip("Quitter le groupe"));
        btnLeaveGroup.setOnAction(e -> quitterGroupe());

        Button btnMembers = new Button("Membres");
        btnMembers.getStyleClass().addAll("call-btn", "group-action-btn");
        btnMembers.setTooltip(new Tooltip("Afficher les membres"));
        btnMembers.setOnAction(e -> toggleMembers());

        topBar.getChildren().addAll(groupAvatar, headerCopy, btnMembers, btnAddMember, btnMeeting, btnLeaveGroup);

        chatArea = new VBox(8);
        chatArea.setPadding(new Insets(18));
        chatArea.getStyleClass().add("group-messages-box");

        emptyStateLabel = new Label("Selectionnez un groupe pour afficher la conversation.");
        emptyStateLabel.getStyleClass().add("group-empty-state");
        emptyStateLabel.setMaxWidth(Double.MAX_VALUE);
        emptyStateLabel.setAlignment(Pos.CENTER);
        chatArea.getChildren().add(emptyStateLabel);

        chatScroll = new ScrollPane(chatArea);
        chatScroll.setFitToWidth(true);
        chatScroll.getStyleClass().addAll("messages-list", "group-message-scroll");
        VBox.setVgrow(chatScroll, Priority.ALWAYS);

        membersTitle = new Label("Membres");
        membersTitle.getStyleClass().add("group-members-title");
        membersBox = new VBox(8, membersTitle);
        membersBox.getStyleClass().add("group-members-box");
        membersBox.setVisible(false);
        membersBox.setManaged(false);

        HBox bottomBar = new HBox(10);
        bottomBar.getStyleClass().addAll("input-bar", "group-input-bar");
        bottomBar.setPadding(new Insets(12, 16, 12, 16));
        bottomBar.setAlignment(Pos.CENTER);

        btnAttach = new Button("Fichier");
        btnAttach.getStyleClass().addAll("call-btn", "group-action-btn");
        btnAttach.setTooltip(new Tooltip("Joindre un fichier"));
        btnAttach.setOnAction(e -> envoyerFichier());

        btnImage = new Button("Image");
        btnImage.getStyleClass().addAll("call-btn", "group-action-btn");
        btnImage.setTooltip(new Tooltip("Envoyer une image"));
        btnImage.setOnAction(e -> envoyerImage());

        txtGroupMsg = new TextField();
        txtGroupMsg.setPromptText("Message au groupe...");
        txtGroupMsg.getStyleClass().add("message-input");
        txtGroupMsg.setOnAction(e -> envoyerMessage());
        HBox.setHgrow(txtGroupMsg, Priority.ALWAYS);

        btnSend = new Button("Envoyer");
        btnSend.getStyleClass().addAll("send-btn", "group-send-btn");
        btnSend.setOnAction(e -> envoyerMessage());

        btnAudio = new Button("Audio");
        btnAudio.getStyleClass().addAll("call-btn", "group-action-btn");
        btnAudio.setTooltip(new Tooltip("Enregistrer un message audio"));
        btnAudio.setOnAction(e -> toggleAudioRecording());

        bottomBar.getChildren().addAll(btnAttach, btnImage, txtGroupMsg, btnAudio, btnSend);
        right.getChildren().addAll(topBar, membersBox, chatScroll, bottomBar);

        getItems().addAll(left, right);
        setDividerPositions(0.24);
        updateGroupActions(false);
    }

    private void registerCallbacks() {
        networkClient.setOnGroupCreated(msg -> Platform.runLater(() -> {
            GroupEntry entry = parseGroupEntry(msg.getContent());
            if (entry != null) {
                ajouterGroupe(entry.id(), entry.name());
                selectGroupById(entry.id());
                if (networkClient != null) networkClient.requestGroupList();
            }
        }));

        networkClient.setOnGroupListResponse(msg -> Platform.runLater(() -> {
            int previouslySelected = selectedGroupId;
            groupIds.clear();
            groupNames.clear();
            activeMeetingGroupIds.clear();
            String raw = msg.getContent();
            if (raw != null && !raw.isBlank()) {
                for (String entry : raw.split(",")) {
                    GroupEntry parsed = parseGroupEntry(entry);
                    if (parsed != null) {
                        ajouterGroupe(parsed.id(), parsed.name());
                        if (parsed.hasMeeting()) activeMeetingGroupIds.add(parsed.id());
                    }
                }
            }
            if (previouslySelected != -1) {
                if (containsGroup(previouslySelected)) selectGroupById(previouslySelected);
                else clearSelectedGroup();
            }
            groupListView.refresh();
        }));

        networkClient.setOnGroupMessage(msg -> Platform.runLater(() -> {
            int gid = msg.getGroupId();
            boolean mine = msg.getFrom().equals(username);
            String time = msg.getTimestamp() != null ? msg.getTimestamp() : LocalDateTime.now().format(timeFmt);
            UiMessage.Kind kind = kindFor(msg.getType());
            String text = mine || kind != UiMessage.Kind.TEXT ? msg.getContent() : msg.getFrom() + ": " + msg.getContent();
            String filePath = saveIncomingMedia(kind, msg.getContent(), msg.getBinaryData());

            groupMessages.computeIfAbsent(gid, k -> FXCollections.observableArrayList())
                    .add(new UiMessage(kind, mine, text, filePath, time));

            if (gid == selectedGroupId) {
                removeSystemMessages();
                afficherBulle(new UiMessage(kind, mine, text, filePath, time));
            }
        }));

        networkClient.setOnGroupMemberAdded(msg -> Platform.runLater(() -> {
            if (msg.getGroupId() == selectedGroupId) {
                afficherSysteme(msg.getContent() + " a rejoint le groupe.");
            }
        }));

        networkClient.setOnGroupMemberRemoved(msg -> Platform.runLater(() -> {
            if (msg.getGroupId() != selectedGroupId) return;
            if (username != null && username.equals(msg.getContent())) {
                networkClient.requestGroupList();
                clearSelectedGroup();
            } else {
                afficherSysteme(msg.getContent() + " a quitte le groupe.");
            }
        }));

        networkClient.setOnGroupMembersResponse(msg -> Platform.runLater(() -> {
            if (msg.getGroupId() != selectedGroupId) return;
            renderMembers(msg.getContent());
        }));

        networkClient.setOnGroupHistoryResponse(msg -> Platform.runLater(() -> {
            if (msg.getGroupId() != selectedGroupId) return;
            String raw = msg.getContent();
            if ("__BEGIN__".equals(raw)) {
                chatArea.getChildren().clear();
                groupMessages.remove(msg.getGroupId());
                return;
            }
            if ("__EMPTY__".equals(raw) || raw == null || raw.isBlank()) {
                chatArea.getChildren().clear();
                groupMessages.remove(msg.getGroupId());
                afficherSysteme("Aucun message dans ce groupe.");
                return;
            }
            String historyType = msg.getConversationId();
            if (historyType != null && !historyType.isBlank()) {
                MessageType type = parseType(historyType);
                UiMessage.Kind kind = kindFor(type);
                boolean mine = username != null && username.equals(msg.getFrom());
                String time = msg.getTimestamp() != null ? msg.getTimestamp() : "";
                String text = mine || kind != UiMessage.Kind.TEXT ? raw : msg.getFrom() + ": " + raw;
                String filePath = saveIncomingMedia(kind, raw, msg.getBinaryData());
                UiMessage uiMessage = new UiMessage(kind, mine, text, filePath, time);
                groupMessages.computeIfAbsent(msg.getGroupId(), k -> FXCollections.observableArrayList()).add(uiMessage);
                afficherBulle(uiMessage);
                Platform.runLater(() -> chatScroll.setVvalue(1.0));
                return;
            }
            chatArea.getChildren().clear();
            groupMessages.remove(msg.getGroupId());
            for (String entry : raw.split(";;;")) {
                String[] parts = entry.split(":::", 4);
                if (parts.length >= 2) {
                    boolean mine = parts[0].equals(username);
                    String time = parts.length >= 3 ? parts[2] : "";
                    MessageType type = parts.length >= 4 ? parseType(parts[3]) : MessageType.GROUP_MESSAGE;
                    String text = mine ? parts[1] : parts[0] + ": " + parts[1];
                    UiMessage.Kind kind = kindFor(type);
                    groupMessages.computeIfAbsent(msg.getGroupId(), k -> FXCollections.observableArrayList())
                            .add(new UiMessage(kind, mine, text, null, time));
                    afficherBulle(new UiMessage(kind, mine, text, null, time));
                }
            }
            Platform.runLater(() -> chatScroll.setVvalue(1.0));
        }));
    }

    private void selectionnerGroupe(int groupId, String nom) {
        selectedGroupId = groupId;
        selectedGroupName = nom;
        lblGroupName.setText(nom);
        lblGroupStatus.setText(activeMeetingGroupIds.contains(groupId) ? "Reunion en cours" : "Groupe actif");
        if (activeMeetingGroupIds.contains(groupId)) {
            btnMeeting.setText("Rejoindre");
            btnMeeting.getStyleClass().add("group-join-btn");
        } else {
            btnMeeting.setText("Reunion");
            btnMeeting.getStyleClass().remove("group-join-btn");
        }
        currentUserAdmin = false;
        btnAddMember.setDisable(true);
        updateGroupActions(true);
        chatArea.getChildren().clear();
        afficherSysteme("Chargement des messages...");
        networkClient.requestGroupHistory(groupId);
        networkClient.requestGroupMembers(groupId);
    }

    private void envoyerMessage() {
        if (selectedGroupId == -1 || networkClient == null) return;
        String texte = txtGroupMsg.getText().trim();
        if (texte.isEmpty()) return;

        networkClient.sendGroupMessage(selectedGroupId, texte);
        txtGroupMsg.clear();
    }

    @FXML
    private void onSendMessage() {
        envoyerMessage();
    }

    private void ouvrirCreationGroupe() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/create-group-dialog.fxml"));
            VBox content = loader.load();
            CreateGroupDialogController controller = loader.getController();
            controller.init(networkClient, allContacts);

            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("Creer un groupe");
            dialog.getDialogPane().setContent(content);
            dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
            applyDialogStyles(dialog);

            dialog.showAndWait().ifPresent(bt -> {
                if (bt == ButtonType.OK) {
                    String name = controller.getGroupName();
                    String desc = controller.getGroupDescription();
                    List<String> members = controller.getSelectedMembers();
                    if (!name.isEmpty()) networkClient.createGroup(name, desc, members);
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void onNewGroup() {
        ouvrirCreationGroupe();
    }

    private void ouvrirAjoutMembre() {
        if (selectedGroupId == -1) {
            new Alert(Alert.AlertType.WARNING, "Selectionnez d'abord un groupe.").showAndWait();
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/add-member-dialog.fxml"));
            VBox content = loader.load();
            AddMemberDialogController controller = loader.getController();
            controller.init(allContacts);

            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("Ajouter des membres");
            dialog.getDialogPane().setContent(content);
            dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
            applyDialogStyles(dialog);

            dialog.showAndWait().ifPresent(bt -> {
                if (bt == ButtonType.OK) {
                    for (String uname : controller.getSelectedMembers()) {
                        networkClient.addGroupMember(selectedGroupId, uname);
                    }
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void onAddMember() {
        ouvrirAjoutMembre();
    }

    private void demarrerReunion() {
        if (selectedGroupId == -1) return;
        if (activeMeetingGroupIds.contains(selectedGroupId)) {
            // Rejoindre au lieu de démarrer
            networkClient.joinMeetingByGroup(selectedGroupId);
        } else {
            javafx.scene.control.ChoiceDialog<String> dialog =
                    new javafx.scene.control.ChoiceDialog<>("VIDEO", "AUDIO", "VIDEO");
            dialog.setTitle("Demarrer une reunion");
            dialog.setHeaderText("Choisir le type de reunion");
            dialog.setContentText("Type :");
            dialog.showAndWait().ifPresent(type -> networkClient.startGroupMeeting(selectedGroupId, type));
        }
    }

    @FXML
    private void onStartMeeting() {
        demarrerReunion();
    }

    private void quitterGroupe() {
        if (selectedGroupId == -1 || networkClient == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Voulez-vous quitter le groupe \"" + selectedGroupName + "\" ?",
                ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText("Quitter le groupe");
        confirm.showAndWait().ifPresent(choice -> {
            if (choice == ButtonType.YES) {
                networkClient.leaveGroup(selectedGroupId);
            }
        });
    }

    private void ajouterGroupe(int id, String nom) {
        for (int[] g : groupIds) {
            if (g[0] == id) return;
        }
        groupIds.add(new int[]{id});
        groupNames.add(nom);
    }

    private void selectGroupById(int id) {
        for (int i = 0; i < groupIds.size(); i++) {
            if (groupIds.get(i)[0] == id) {
                groupListView.getSelectionModel().select(i);
                selectionnerGroupe(id, groupNames.get(i));
                return;
            }
        }
    }

    private boolean containsGroup(int id) {
        for (int[] group : groupIds) {
            if (group[0] == id) return true;
        }
        return false;
    }

    private void clearSelectedGroup() {
        selectedGroupId = -1;
        selectedGroupName = "";
        groupListView.getSelectionModel().clearSelection();
        lblGroupName.setText("Selectionnez un groupe");
        lblGroupStatus.setText("Choisissez un groupe pour envoyer des messages.");
        chatArea.getChildren().clear();
        chatArea.getChildren().add(emptyStateLabel);
        updateGroupActions(false);
    }

    private void afficherBulle(UiMessage message) {
        boolean mine = message.isOwn();
        Label bubble = new Label(message.getText());
        bubble.setWrapText(true);
        bubble.setMaxWidth(420);
        bubble.getStyleClass().add(mine ? "bubble-sent" : "bubble-received");

        VBox content;
        if (message.getKind() == UiMessage.Kind.IMAGE && message.getFilePath() != null) {
            ImageView imageView = new ImageView(new Image(new File(message.getFilePath()).toURI().toString(), 240, 0, true, true));
            imageView.getStyleClass().add(mine ? "bubble-sent" : "bubble-received");
            content = new VBox(2, imageView);
        } else if (message.getKind() == UiMessage.Kind.AUDIO) {
            Button play = new Button("Lire audio");
            play.getStyleClass().addAll("call-btn", "group-action-btn");
            play.setDisable(message.getFilePath() == null);
            play.setOnAction(e -> playAudio(message.getFilePath()));
            content = new VBox(2, play);
        } else if (message.getKind() == UiMessage.Kind.FILE) {
            Button download = new Button("Telecharger " + message.getText());
            download.getStyleClass().addAll("call-btn", "group-action-btn");
            download.setDisable(message.getFilePath() == null);
            download.setOnAction(e -> downloadFile(message.getFilePath(), message.getText()));
            content = new VBox(2, download);
        } else {
            content = new VBox(2, bubble);
        }

        Label ts = new Label(message.getTimestamp());
        ts.getStyleClass().add(mine ? "timestamp-sent" : "timestamp-received");
        content.getChildren().add(ts);
        content.setAlignment(mine ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

        HBox row = new HBox(content);
        row.setPadding(new Insets(2, 10, 2, 10));
        row.setAlignment(mine ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

        chatArea.getChildren().add(row);
        Platform.runLater(() -> chatScroll.setVvalue(1.0));
    }

    private void afficherSysteme(String msg) {
        Label lbl = new Label(msg);
        lbl.getStyleClass().add("group-system-msg");
        lbl.setMaxWidth(Double.MAX_VALUE);
        lbl.setAlignment(Pos.CENTER);
        chatArea.getChildren().add(lbl);
    }

    private void removeSystemMessages() {
        chatArea.getChildren().removeIf(node -> node instanceof Label label
                && label.getStyleClass().contains("group-system-msg"));
    }

    private void updateGroupActions(boolean enabled) {
        btnAddMember.setDisable(!enabled);
        if (enabled) btnAddMember.setDisable(!currentUserAdmin);
        btnMeeting.setDisable(!enabled);
        btnLeaveGroup.setDisable(!enabled);
        btnSend.setDisable(!enabled);
        btnAttach.setDisable(!enabled);
        btnImage.setDisable(!enabled);
        btnAudio.setDisable(!enabled);
        txtGroupMsg.setDisable(!enabled);
    }

    private void toggleMembers() {
        boolean show = !membersBox.isVisible();
        membersBox.setVisible(show);
        membersBox.setManaged(show);
        if (show && selectedGroupId != -1 && networkClient != null) {
            networkClient.requestGroupMembers(selectedGroupId);
        }
    }

    private void renderMembers(String raw) {
        membersBox.getChildren().setAll(membersTitle);
        currentUserAdmin = false;
        if (raw != null && !raw.isBlank()) {
            String[] entries = raw.split(",");
            for (String entry : entries) {
                String[] parts = entry.split(":", 3);
                if (parts.length >= 3 && parts[1].equals(username) && "ADMIN".equalsIgnoreCase(parts[2])) {
                    currentUserAdmin = true;
                }
            }
            for (String entry : entries) {
                String[] parts = entry.split(":", 3);
                if (parts.length < 2) continue;
                String name = parts[1];
                String role = parts.length == 3 ? parts[2] : "MEMBRE";
                Label member = new Label(name + ("ADMIN".equalsIgnoreCase(role) ? " (Admin)" : ""));
                member.getStyleClass().add("group-member-row");
                if (currentUserAdmin && !name.equals(username)) {
                    javafx.scene.control.ContextMenu cm = new javafx.scene.control.ContextMenu();
                    javafx.scene.control.MenuItem mi = new javafx.scene.control.MenuItem("Retirer du groupe");
                    mi.setOnAction(e -> networkClient.removeGroupMember(selectedGroupId, name));
                    cm.getItems().add(mi);
                    member.setContextMenu(cm);
                }
                membersBox.getChildren().add(member);
            }
        }
        btnAddMember.setDisable(selectedGroupId == -1 || !currentUserAdmin);
    }

    private void envoyerImage() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choisir une image");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif"));
        File file = chooser.showOpenDialog(getScene() == null ? null : getScene().getWindow());
        if (file != null) envoyerMedia(file, MessageType.GROUP_IMAGE);
    }

    private void envoyerFichier() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choisir un fichier");
        File file = chooser.showOpenDialog(getScene() == null ? null : getScene().getWindow());
        if (file != null) envoyerMedia(file, MessageType.GROUP_FILE);
    }

    private void envoyerMedia(File file, MessageType type) {
        if (selectedGroupId == -1 || networkClient == null || file == null) return;
        try {
            networkClient.sendGroupMedia(selectedGroupId, type, file.getName(), Files.readAllBytes(file.toPath()));
        } catch (IOException e) {
            new Alert(Alert.AlertType.ERROR, "Impossible de lire le fichier: " + e.getMessage()).showAndWait();
        }
    }

    private void toggleAudioRecording() {
        if (recordingAudio) {
            stopAudioRecording();
        } else {
            startAudioRecording();
        }
    }

    private void startAudioRecording() {
        AudioFormat format = new AudioFormat(16000, 16, 1, true, false);
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
        if (!AudioSystem.isLineSupported(info)) {
            new Alert(Alert.AlertType.WARNING, "Micro non supporte sur cet appareil.").showAndWait();
            return;
        }
        try {
            targetDataLine = (TargetDataLine) AudioSystem.getLine(info);
            targetDataLine.open(format);
            targetDataLine.start();
            recordingAudio = true;
            btnAudio.setText("Stop");
            btnAudio.setStyle("-fx-text-fill: #f87171; -fx-font-weight: bold;");
            Thread thread = new Thread(() -> captureAudio(format), "group-audio-recording");
            thread.setDaemon(true);
            thread.start();
        } catch (LineUnavailableException e) {
            new Alert(Alert.AlertType.ERROR, "Impossible d'acceder au micro: " + e.getMessage()).showAndWait();
        }
    }

    private void stopAudioRecording() {
        recordingAudio = false;
        if (targetDataLine != null) {
            targetDataLine.stop();
            targetDataLine.close();
        }
        btnAudio.setText("Audio");
        btnAudio.setStyle("");
    }

    private void captureAudio(AudioFormat format) {
        byte[] buffer = new byte[4096];
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            while (recordingAudio) {
                int count = targetDataLine.read(buffer, 0, buffer.length);
                if (count > 0) out.write(buffer, 0, count);
            }
            byte[] raw = out.toByteArray();
            if (raw.length == 0) return;
            ByteArrayOutputStream wav = new ByteArrayOutputStream();
            try (AudioInputStream stream = new AudioInputStream(new ByteArrayInputStream(raw), format, raw.length / format.getFrameSize())) {
                AudioSystem.write(stream, AudioFileFormat.Type.WAVE, wav);
            }
            Platform.runLater(() -> networkClient.sendGroupMedia(selectedGroupId, MessageType.GROUP_AUDIO, "Audio", wav.toByteArray()));
        } catch (Exception e) {
            Platform.runLater(() -> new Alert(Alert.AlertType.ERROR, "Erreur audio: " + e.getMessage()).showAndWait());
        }
    }

    private UiMessage.Kind kindFor(MessageType type) {
        if (type == MessageType.GROUP_AUDIO) return UiMessage.Kind.AUDIO;
        if (type == MessageType.GROUP_IMAGE) return UiMessage.Kind.IMAGE;
        if (type == MessageType.GROUP_FILE) return UiMessage.Kind.FILE;
        return UiMessage.Kind.TEXT;
    }

    private MessageType parseType(String raw) {
        try {
            return MessageType.valueOf(raw);
        } catch (Exception ignored) {
            return MessageType.GROUP_MESSAGE;
        }
    }

    private String saveIncomingMedia(UiMessage.Kind kind, String name, byte[] data) {
        if (kind == UiMessage.Kind.TEXT || data == null || data.length == 0) return null;
        String suffix = kind == UiMessage.Kind.AUDIO ? ".wav" : "_" + (name == null ? "file" : name);
        try {
            Path temp = Files.createTempFile("group-media-", suffix);
            try (FileOutputStream out = new FileOutputStream(temp.toFile())) {
                out.write(data);
            }
            return temp.toString();
        } catch (IOException e) {
            return null;
        }
    }

    private void playAudio(String path) {
        if (path == null) return;
        new Thread(() -> {
            try (AudioInputStream stream = AudioSystem.getAudioInputStream(new File(path))) {
                Clip clip = AudioSystem.getClip();
                clip.open(stream);
                clip.start();
            } catch (Exception e) {
                Platform.runLater(() -> new Alert(Alert.AlertType.ERROR, "Impossible de lire l'audio.").showAndWait());
            }
        }, "group-audio-playback").start();
    }

    private void downloadFile(String path, String suggestedName) {
        if (path == null) return;
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Enregistrer le fichier");
        chooser.setInitialFileName(suggestedName == null || suggestedName.isBlank() ? "fichier" : suggestedName);
        File dest = chooser.showSaveDialog(getScene() == null ? null : getScene().getWindow());
        if (dest == null) return;
        try {
            Files.copy(Path.of(path), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            new Alert(Alert.AlertType.ERROR, "Impossible d'enregistrer le fichier: " + e.getMessage()).showAndWait();
        }
    }

    private void applyDialogStyles(Dialog<ButtonType> dialog) {
        dialog.getDialogPane().getStyleClass().add("group-dialog");
        String stylesheet = getClass().getResource("/css/whatsapp.css").toExternalForm();
        dialog.getDialogPane().getStylesheets().add(stylesheet);
    }

    private String initialFor(String value) {
        if (value == null || value.isBlank()) return "#";
        return value.trim().substring(0, 1).toUpperCase();
    }

    private GroupEntry parseGroupEntry(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String[] parts = raw.contains("|") ? raw.split("\\|", 2) : raw.split(":", 3);
        if (parts.length < 2) return null;
        try {
            int id = Integer.parseInt(parts[0].trim());
            String name = parts[1].trim();
            boolean hasMeeting = !raw.contains("|") && parts.length >= 3 && "1".equals(parts[2].trim());
            return new GroupEntry(id, name, hasMeeting);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private record GroupEntry(int id, String name, boolean hasMeeting) {}
}
