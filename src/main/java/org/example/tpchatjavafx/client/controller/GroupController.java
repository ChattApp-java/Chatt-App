package org.example.tpchatjavafx.client.controller;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.fxml.FXMLLoader;
import org.example.tpchatjavafx.client.NetworkClient;
import org.example.tpchatjavafx.client.model.ChatMessage;
import org.example.tpchatjavafx.client.util.UiMessage;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GroupController extends SplitPane {

    private NetworkClient networkClient;
    private String username;

    private final List<int[]> groupIds = new ArrayList<>();
    private final ObservableList<String> groupNames = FXCollections.observableArrayList();
    private final Map<Integer, ObservableList<UiMessage>> groupMessages = new HashMap<>();

    private int    selectedGroupId   = -1;
    private String selectedGroupName = "";

    private final DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm");
    private List<String> allContacts = new ArrayList<>();

    // UI
    private ListView<String> groupListView;
    private VBox             chatArea;
    private ScrollPane       chatScroll;
    private TextField        txtGroupMsg;
    private Label            lblGroupName;

    public GroupController() {
        buildUI();
    }

    // ── Appelé par MainChatController après init ──────────────────
    public void init(NetworkClient networkClient, String username, List<String> allContacts) {
        this.networkClient = networkClient;
        this.username = username;
        this.allContacts = allContacts;
        registerCallbacks();
        networkClient.requestGroupList();
    }

    // ── Construction de l'interface ───────────────────────────────
    private void buildUI() {
        // Panneau gauche — liste des groupes
        VBox left = new VBox(8);
        left.setPadding(new Insets(10));
        left.getStyleClass().add("sidebar");
        left.setPrefWidth(220);

        Label lblTitle = new Label("👥 Groupes");
        lblTitle.setStyle("-fx-font-size:16px; -fx-font-weight:bold; -fx-text-fill:#7209B7;");

        Button btnNew = new Button("➕ Nouveau Groupe");
        btnNew.getStyleClass().add("send-btn");
        btnNew.setMaxWidth(Double.MAX_VALUE);
        btnNew.setOnAction(e -> ouvrirCreationGroupe());

        groupListView = new ListView<>(groupNames);
        groupListView.getStyleClass().add("contact-list");
        groupListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : "# " + item);
            }
        });
        groupListView.getSelectionModel().selectedIndexProperty()
                .addListener((obs, old, idx) -> {
                    int i = idx.intValue();
                    if (i >= 0 && i < groupIds.size()) {
                        selectionnerGroupe(groupIds.get(i)[0], groupNames.get(i));
                    }
                });

        VBox.setVgrow(groupListView, Priority.ALWAYS);
        left.getChildren().addAll(lblTitle, btnNew, groupListView);

        // Panneau droit — chat de groupe
        VBox right = new VBox();

        HBox topBar = new HBox(10);
        topBar.getStyleClass().add("chat-top-bar");
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(10, 16, 10, 16));

        lblGroupName = new Label("Sélectionnez un groupe");
        lblGroupName.getStyleClass().add("chat-contact-name");
        HBox.setHgrow(lblGroupName, Priority.ALWAYS);

        Button btnAddMember = new Button("👤+");
        btnAddMember.getStyleClass().add("call-btn");
        btnAddMember.setTooltip(new Tooltip("Ajouter un membre"));
        btnAddMember.setOnAction(e -> ouvrirAjoutMembre());

        Button btnMeeting = new Button("📹 Réunion");
        btnMeeting.getStyleClass().add("call-btn");
        btnMeeting.setOnAction(e -> demarrerReunion());

        topBar.getChildren().addAll(lblGroupName, btnAddMember, btnMeeting);

        chatArea = new VBox(8);
        chatArea.setPadding(new Insets(10));

        chatScroll = new ScrollPane(chatArea);
        chatScroll.setFitToWidth(true);
        chatScroll.getStyleClass().add("messages-list");
        VBox.setVgrow(chatScroll, Priority.ALWAYS);

        HBox bottomBar = new HBox(10);
        bottomBar.getStyleClass().add("input-bar");
        bottomBar.setPadding(new Insets(10, 16, 10, 16));
        bottomBar.setAlignment(Pos.CENTER);

        txtGroupMsg = new TextField();
        txtGroupMsg.setPromptText("Message au groupe...");
        txtGroupMsg.getStyleClass().add("message-input");
        HBox.setHgrow(txtGroupMsg, Priority.ALWAYS);
        txtGroupMsg.setOnAction(e -> envoyerMessage());

        Button btnSend = new Button("➤");
        btnSend.getStyleClass().add("send-btn");
        btnSend.setOnAction(e -> envoyerMessage());

        bottomBar.getChildren().addAll(txtGroupMsg, btnSend);
        right.getChildren().addAll(topBar, chatScroll, bottomBar);

        this.getItems().addAll(left, right);
        this.setDividerPositions(0.28);
    }

    // ── Enregistrement des callbacks réseau ───────────────────────
    private void registerCallbacks() {
        networkClient.setOnGroupCreated(msg -> Platform.runLater(() -> {
            // content = "id|nom"
            String[] parts = msg.getContent().split("\\|", 2);
            if (parts.length == 2) {
                try {
                    ajouterGroupe(Integer.parseInt(parts[0].trim()), parts[1].trim());
                } catch (NumberFormatException ignored) {}
            }
        }));

        networkClient.setOnGroupListResponse(msg -> Platform.runLater(() -> {
            groupIds.clear();
            groupNames.clear();
            String raw = msg.getContent();
            if (raw == null || raw.isBlank()) return;
            // format : "id:nom,id:nom,..."
            for (String entry : raw.split(",")) {
                String[] kv = entry.split(":", 2);
                if (kv.length == 2) {
                    try {
                        ajouterGroupe(Integer.parseInt(kv[0].trim()), kv[1].trim());
                    } catch (NumberFormatException ignored) {}
                }
            }
        }));

        networkClient.setOnGroupMessage(msg -> Platform.runLater(() -> {
            int gid = msg.getGroupId();
            boolean mine = msg.getFrom().equals(username);
            String time  = msg.getTimestamp() != null
                    ? msg.getTimestamp()
                    : LocalDateTime.now().format(timeFmt);
            String text  = mine ? msg.getContent() : msg.getFrom() + ": " + msg.getContent();

            groupMessages.computeIfAbsent(gid, k -> FXCollections.observableArrayList())
                    .add(new UiMessage(UiMessage.Kind.TEXT, mine, text, null, time));

            if (gid == selectedGroupId) {
                afficherBulle(text, mine, time);
            }
        }));

        networkClient.setOnGroupMemberAdded(msg -> Platform.runLater(() -> {
            if (msg.getGroupId() == selectedGroupId) {
                afficherSysteme(msg.getContent() + " a rejoint le groupe.");
            }
        }));

        networkClient.setOnGroupHistoryResponse(msg -> Platform.runLater(() -> {
            if (msg.getGroupId() != selectedGroupId) return;
            chatArea.getChildren().clear();
            String raw = msg.getContent();
            if (raw == null || raw.isBlank()) return;
            // format : "sender:::content:::ts;;;sender:::content:::ts;;;..."
            for (String entry : raw.split(";;;")) {
                String[] parts = entry.split(":::", 3);
                if (parts.length >= 2) {
                    boolean mine = parts[0].equals(username);
                    String time  = parts.length == 3 ? parts[2] : "";
                    String text  = mine ? parts[1] : parts[0] + ": " + parts[1];
                    afficherBulle(text, mine, time);
                }
            }
            Platform.runLater(() -> chatScroll.setVvalue(1.0));
        }));
    }

    // ── Actions UI ────────────────────────────────────────────────
    private void selectionnerGroupe(int groupId, String nom) {
        selectedGroupId   = groupId;
        selectedGroupName = nom;
        lblGroupName.setText("# " + nom);
        chatArea.getChildren().clear();

        ObservableList<UiMessage> cached = groupMessages.get(groupId);
        if (cached != null && !cached.isEmpty()) {
            for (UiMessage m : cached)
                afficherBulle(m.getText(), m.isOwn(), m.getTimestamp());
        } else {
            networkClient.requestGroupHistory(groupId);
        }
    }

    private void envoyerMessage() {
        if (selectedGroupId == -1 || networkClient == null) return;
        String texte = txtGroupMsg.getText().trim();
        if (texte.isEmpty()) return;

        networkClient.sendGroupMessage(selectedGroupId, texte);

        String time = LocalDateTime.now().format(timeFmt);
        groupMessages.computeIfAbsent(selectedGroupId, k -> FXCollections.observableArrayList())
                .add(new UiMessage(UiMessage.Kind.TEXT, true, texte, null, time));
        afficherBulle(texte, true, time);
        txtGroupMsg.clear();
    }

    private void ouvrirCreationGroupe() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/create-group-dialog.fxml"));
            javafx.scene.layout.VBox content = loader.load();
            CreateGroupDialogController controller = loader.getController();
            controller.init(networkClient, allContacts);

            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("Créer un groupe");
            dialog.getDialogPane().setContent(content);
            dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
            
            dialog.showAndWait().ifPresent(bt -> {
                if (bt == ButtonType.OK) {
                    String name = controller.getGroupName();
                    String desc = controller.getGroupDescription();
                    java.util.List<String> members = controller.getSelectedMembers();
                    if (!name.isEmpty()) networkClient.createGroup(name, desc, members);
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void ouvrirAjoutMembre() {
        if (selectedGroupId == -1) {
            new Alert(Alert.AlertType.WARNING, "Sélectionnez d'abord un groupe.").showAndWait();
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/add-member-dialog.fxml"));
            javafx.scene.layout.VBox content = loader.load();
            AddMemberDialogController controller = loader.getController();
            
            // On passe tous les contacts pour l'instant (filtrage optionnel)
            controller.init(allContacts);

            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("Ajouter des membres");
            dialog.getDialogPane().setContent(content);
            dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

            dialog.showAndWait().ifPresent(bt -> {
                if (bt == ButtonType.OK) {
                    List<String> selected = controller.getSelectedMembers();
                    for (String uname : selected) {
                        networkClient.addGroupMember(selectedGroupId, uname);
                    }
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void demarrerReunion() {
        if (selectedGroupId == -1) return;
        networkClient.startGroupMeeting(selectedGroupId, "AUDIO_VIDEO");
        afficherSysteme("Demande de réunion envoyée...");
    }

    // ── Helpers UI ────────────────────────────────────────────────
    private void ajouterGroupe(int id, String nom) {
        for (int[] g : groupIds) if (g[0] == id) return; // déjà présent
        groupIds.add(new int[]{id});
        groupNames.add(nom);
    }

    private void afficherBulle(String texte, boolean mine, String time) {
        Label bubble = new Label(texte);
        bubble.setWrapText(true);
        bubble.setMaxWidth(380);
        bubble.getStyleClass().add(mine ? "bubble-sent" : "bubble-received");

        Label ts = new Label(time);
        ts.getStyleClass().add(mine ? "timestamp-sent" : "timestamp-received");

        VBox box = new VBox(2, bubble, ts);
        box.setAlignment(mine ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

        HBox row = new HBox(box);
        row.setPadding(new Insets(2, 10, 2, 10));
        row.setAlignment(mine ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

        chatArea.getChildren().add(row);
        Platform.runLater(() -> chatScroll.setVvalue(1.0));
    }

    private void afficherSysteme(String msg) {
        Label lbl = new Label(msg);
        lbl.setStyle("-fx-text-fill:#8e8e8e; -fx-font-size:11px;");
        lbl.setMaxWidth(Double.MAX_VALUE);
        lbl.setAlignment(Pos.CENTER);
        chatArea.getChildren().add(lbl);
    }
}