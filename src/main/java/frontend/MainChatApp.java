package frontend;

import base_de_donnees.DAOMessage;
import base_de_donnees.DAOUtilisateur;
import frontend.composants.ChatBubbleFX;
import frontend.composants.UserListCell;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import modele.Message;
import modele.Utilisateur;

import java.time.LocalDateTime;
import java.util.List;

public class MainChatApp extends BorderPane {

    private Stage primaryStage;
    private Utilisateur currentUser;
    private Utilisateur selectedUser;
    private client.ConnexionServeur connexion;
    private AppelWindow currentAppel;
    
    private DAOUtilisateur daoUser;
    private DAOMessage daoMessage;
    private base_de_donnees.DAOAppel daoAppel;
    
    private ListView<Utilisateur> userList;
    private ListView<String> callList;
    private VBox chatArea;
    private ScrollPane chatScrollPane;
    private TextField txtMessage;
    private Button btnSend;
    private Button btnMic;
    private Label lblChatWith;
    private audio.EnregistreurAudio recorder = new audio.EnregistreurAudio();
    private boolean isRecording = false;

    public MainChatApp(Stage stage, Utilisateur user, client.ConnexionServeur conn) {
        this.primaryStage = stage;
        this.currentUser = user;
        this.connexion = conn;
        this.daoUser = new DAOUtilisateur();
        this.daoMessage = new DAOMessage();
        this.daoAppel = new base_de_donnees.DAOAppel();
        
        setupUI();
        loadUsers();
        loadCallHistory();
    }

    private void setupUI() {
        // Sidebar (User List)
        VBox sidebar = new VBox(10);
        sidebar.setPadding(new Insets(10));
        sidebar.setPrefWidth(250);
        sidebar.getStyleClass().add("sidebar");
        
        Label lblUsers = new Label("Utilisateurs");
        lblUsers.getStyleClass().add("label-header");
        lblUsers.setStyle("-fx-font-size: 20px;");
        
        TabPane sidebarTabs = new TabPane();
        sidebarTabs.getStyleClass().add("sidebar-tabs");
        
        Tab tabContacts = new Tab("Contacts");
        tabContacts.setClosable(false);
        
        userList = new ListView<>();
        userList.getStyleClass().add("list-view");
        userList.setCellFactory(param -> new UserListCell());
        userList.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                selectedUser = newVal;
                loadChatHistory();
            }
        });
        tabContacts.setContent(userList);

        Tab tabCalls = new Tab("Appels");
        tabCalls.setClosable(false);
        callList = new ListView<>();
        callList.getStyleClass().add("list-view");
        tabCalls.setContent(callList);

        sidebarTabs.getTabs().addAll(tabContacts, tabCalls);
        VBox.setVgrow(sidebarTabs, Priority.ALWAYS);
        sidebar.getChildren().addAll(lblUsers, sidebarTabs);
        
        // Center (Chat Area)
        VBox centerBox = new VBox();
        
        // Top Bar
        HBox topBar = new HBox(10);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.getStyleClass().add("top-bar");
        lblChatWith = new Label("Sélectionnez un utilisateur");
        lblChatWith.getStyleClass().add("label-header");
        lblChatWith.setStyle("-fx-font-size: 18px;");
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        Button btnAudio = new Button("📞");
        btnAudio.getStyleClass().add("button-call");
        btnAudio.setOnAction(e -> demarrerAppel("audio"));
        
        Button btnVideo = new Button("📹");
        btnVideo.getStyleClass().add("button-call");
        btnVideo.setOnAction(e -> demarrerAppel("video"));
        
        topBar.getChildren().addAll(lblChatWith, spacer, btnAudio, btnVideo);
        
        // Chat History Area
        chatArea = new VBox(10);
        chatArea.setPadding(new Insets(10));
        
        chatScrollPane = new ScrollPane(chatArea);
        chatScrollPane.setFitToWidth(true);
        chatScrollPane.getStyleClass().add("chat-scroll-pane");
        VBox.setVgrow(chatScrollPane, Priority.ALWAYS);
        
        // Bottom Bar (Input)
        HBox bottomBar = new HBox(10);
        bottomBar.setPadding(new Insets(15));
        bottomBar.setAlignment(Pos.CENTER);
        
        txtMessage = new TextField();
        txtMessage.setPromptText("Écrire un message...");
        HBox.setHgrow(txtMessage, Priority.ALWAYS);
        txtMessage.setOnAction(e -> sendMessage());
        
        Button btnAttach = new Button("📎");
        btnAttach.getStyleClass().add("button-secondary");
        btnAttach.setOnAction(e -> choisirFichier());

        btnMic = new Button("🎤");
        btnMic.getStyleClass().add("button-secondary");
        btnMic.setOnAction(e -> enregistrerAudio());

        bottomBar.getChildren().addAll(btnAttach, btnMic, txtMessage, btnSend);
        
        centerBox.getChildren().addAll(topBar, chatScrollPane, bottomBar);
        
        this.setLeft(sidebar);
        this.setCenter(centerBox);
    }

    private void loadUsers() {
        List<Utilisateur> users = daoUser.getTous(); 
        ObservableList<Utilisateur> observableUsers = FXCollections.observableArrayList(users);
        observableUsers.removeIf(u -> u.getId_user() == currentUser.getId_user());
        userList.setItems(observableUsers);
    }

    private void loadChatHistory() {
        if (selectedUser == null) return;
        
        lblChatWith.setText("Chat avec " + selectedUser.getUsername());
        chatArea.getChildren().clear();
        
        List<Message> history = daoMessage.getHistorique(currentUser.getId_user(), selectedUser.getId_user());
        for (Message m : history) {
            boolean isSender = (m.getId_sender() == currentUser.getId_user());
            chatArea.getChildren().add(new ChatBubbleFX(m, isSender));
        }
        
        Platform.runLater(() -> chatScrollPane.setVvalue(1.0));
    }

    private void sendMessage() {
        if (selectedUser == null) return;
        String texte = txtMessage.getText().trim();
        if (texte.isEmpty()) return;
        
        Message msg = new Message(0, currentUser.getId_user(), selectedUser.getId_user(), texte, LocalDateTime.now(), "non_lu");
        int idMsg = daoMessage.sauvegarderMessage(msg);
        
        if (idMsg > 0) {
            msg.setId_message(idMsg);
            chatArea.getChildren().add(new ChatBubbleFX(msg, true));
            
            // Envoyer via TCP
            connexion.envoyer(commun.Protocole.MSG + commun.Protocole.SEP 
                    + selectedUser.getUsername() + commun.Protocole.SEP + texte);
            
            txtMessage.clear();
            Platform.runLater(() -> chatScrollPane.setVvalue(1.0));
        }
    }

    // --- Méthodes appelées par EcouteurMessages ---
    public void mettreAJourListe(String[] users) {
        // Optionnel : Mettre à jour la liste depuis le serveur plutôt que la BDD
        loadUsers();
    }

    public void ajouterMessage(String sender, String content, boolean isMine) {
        if (selectedUser != null && selectedUser.getUsername().equals(sender)) {
            Message msg = new Message(0, selectedUser.getId_user(), currentUser.getId_user(), content, LocalDateTime.now(), "non_lu");
            chatArea.getChildren().add(new ChatBubbleFX(msg, false));
            Platform.runLater(() -> chatScrollPane.setVvalue(1.0));
        }
    }

    public void ajouterUtilisateur(String username) {
        loadUsers();
    }

    public void retirerUtilisateur(String username) {
        loadUsers();
    }

    public void ajouterMessageSysteme(String message) {
        Label sysMsg = new Label(message);
        sysMsg.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px; -fx-alignment: center; -fx-padding: 10 0;");
        sysMsg.setMaxWidth(Double.MAX_VALUE);
        sysMsg.setAlignment(Pos.CENTER);
        chatArea.getChildren().add(sysMsg);
    }

    private void choisirFichier() {
        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        java.io.File file = fileChooser.showOpenDialog(primaryStage);
        if (file != null) {
            ajouterMessageSysteme("Envoi du fichier : " + file.getName());
            client.GestionnaireFichier.envoyerFichier(file, selectedUser.getUsername(), connexion);
        }
    }

    private void enregistrerAudio() {
        if (!isRecording) {
            try {
                isRecording = true;
                btnMic.setText("🛑");
                btnMic.setStyle("-fx-text-fill: red;");
                recorder.demarrer("temp_voice_note.wav");
                ajouterMessageSysteme("Enregistrement en cours...");
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            isRecording = false;
            btnMic.setText("🎤");
            btnMic.setStyle("");
            recorder.arreter();
            ajouterMessageSysteme("Enregistrement terminé. Envoi...");
            java.io.File file = new java.io.File("temp_voice_note.wav");
            client.GestionnaireFichier.envoyerFichier(file, selectedUser.getUsername(), connexion);
        }
    }

    private void loadCallHistory() {
        List<String[]> history = daoAppel.getHistorique(currentUser.getId_user());
        ObservableList<String> items = FXCollections.observableArrayList();
        for (String[] call : history) {
            String label = String.format("[%s] %s -> %s (%ss)", call[2], call[0], call[1], call[4]);
            items.add(label);
        }
        callList.setItems(items);
    }

    private void demarrerAppel(String type) {
        if (selectedUser == null) return;
        ajouterMessageSysteme("Appel " + type + " vers " + selectedUser.getUsername() + "...");
        connexion.envoyer(commun.Protocole.CALL_REQUEST + commun.Protocole.SEP 
                + currentUser.getUsername() + commun.Protocole.SEP 
                + selectedUser.getUsername() + commun.Protocole.SEP + type);
    }

    public void gererAppelEntrant(String fromUser, String type) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Appel entrant");
        alert.setHeaderText(fromUser + " vous appelle (" + type + ")");
        alert.setContentText("Voulez-vous accepter l'appel ?");

        ButtonType btnAccept = new ButtonType("Accepter");
        ButtonType btnReject = new ButtonType("Refuser", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(btnAccept, btnReject);

        alert.showAndWait().ifPresent(response -> {
            if (response == btnAccept) {
                connexion.envoyer(commun.Protocole.CALL_ACCEPT + commun.Protocole.SEP + fromUser + commun.Protocole.SEP + currentUser.getUsername());
                // Trouver l'ID du partenaire (nécessite une recherche ou passage via le protocole)
                // Pour simplifier, on utilise 0 si inconnu, ou on le récupère du DAO
                int idFrom = daoUser.getByUsername(fromUser).getId_user();
                currentAppel = new AppelWindow(type, fromUser, connexion, idFrom, currentUser.getId_user());
                currentAppel.demarrerFlux("localhost");
            } else {
                connexion.envoyer(commun.Protocole.CALL_REJECT + commun.Protocole.SEP + fromUser + commun.Protocole.SEP + currentUser.getUsername());
            }
        });
    }

    public void appelAccepte(String[] parts) {
        if (parts.length >= 2) {
            String partner = parts[1];
            ajouterMessageSysteme("Appel accepté par " + partner);
            int idPartner = daoUser.getByUsername(partner).getId_user();
            currentAppel = new AppelWindow("audio/video", partner, connexion, currentUser.getId_user(), idPartner); 
            currentAppel.demarrerFlux("localhost");
        }
    }

    public void appelRefuse(String fromUser) {
        ajouterMessageSysteme("L'appel vers " + fromUser + " a été refusé.");
    }

    public void appelTermine() {
        if (currentAppel != null) {
            currentAppel.fermer();
            currentAppel = null;
        }
        ajouterMessageSysteme("Appel terminé.");
    }
}
