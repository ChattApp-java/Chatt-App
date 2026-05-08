# 📦 INVENTAIRE COMPLET DES CLASSES

---

## ✅ CLASSES EXISTANTES

### BACKEND - Server

#### Package: `org.example.tpchatjavafx.server`

```
📦 SERVER CORE
├── ✅ ChatServer.java
│   └── Port: 5555 | Role: Serveur TCP multithread
├── ✅ ClientHandler.java
│   └── Role: Gestion 1 client (thread)
└── ✅ ServerLauncher.java
    └── Role: Point d'entrée serveur
```

### FRONTEND - Client

#### Package: `org.example.tpchatjavafx.client`

```
📦 APPLICATION CORE
├── ✅ ChatClientApp.java
│   └── Role: Application JavaFX principale
├── ✅ MainLauncher.java
│   └── Role: Launcher JavaFX
├── ✅ NetworkClient.java
│   └── Role: Client TCP vers serveur
└── ✅ HelloApplication.java
    └── Role: (Legacy?)
```

#### Package: `org.example.tpchatjavafx.client.controller`

```
📦 UI CONTROLLERS (JavaFX)
├── ✅ LoginController.java
│   └── Écran de login/register
├── ✅ MainChatController.java
│   └── Chat principal (messages texte, contacts)
└── ✅ ProfileController.java
    └── Profil utilisateur
```

#### Package: `org.example.tpchatjavafx.client.model`

```
📦 CLIENT MODELS
└── ✅ ChatMessage.java
    └── Sérialisation/désérialisation messages
```

#### Package: `org.example.tpchatjavafx.client.video`

```
📦 VIDEO CALLS
├── ✅ VideoCallWindow.java
│   └── Fenêtre vidéo (UI)
└── ✅ VideoCallController.java
    └── Contrôleur vidéo (UI)
```

#### Package: `org.example.tpchatjavafx.client.voice`

```
📦 VOICE CALLS
├── ✅ VoiceCallWindow.java
│   └── Fenêtre appel vocal (UI)
└── ✅ VoiceCallSession.java
    └── Gestion session audio (?)
```

#### Package: `org.example.tpchatjavafx.client.util`

```
📦 UTILITIES
└── ✅ UiMessage.java
    └── Affichage messages dans ListView
```

### MODÈLES DE DONNÉES (Models)

#### Package: `org.example.tpchatjavafx.model`

```
📦 DATA MODELS
├── ✅ User.java
│   └── id, username, email, passwordHash
├── ✅ Utilisateur.java
│   └── DUPLICATION de User! À consolider
├── ✅ Message.java
│   └── id, contenu, type (TEXTE/AUDIO/VIDEO), dates
├── ✅ Conversation.java
│   └── id, dateCreation, derniereModification
│       ⚠️ MANQUE: champ 'type' (INDIVIDUEL/GROUPE)
├── ✅ Appel.java
│   └── id, type (VOCAL/VIDEO), statut, dates, durée
├── ✅ Notification.java
│   └── id, utilisateur, contenu, type
├── ✅ FichierMedia.java
│   └── id, message, nomFichier, chemin, taille, type
├── ✅ Video.java
│   └── id, resolution, duree
└── ✅ Vocal.java
    └── id, duree
```

### DATA ACCESS OBJECTS (DAOs)

#### Package: `org.example.tpchatjavafx.dao`

```
📦 DATABASE ACCESS
├── ✅ DatabaseConnection.java
│   └── HikariCP connection pool
├── ✅ UtilisateurDAO.java
│   └── CRUD users
├── ✅ MessageDAO.java
│   └── CRUD messages
├── ✅ ConversationDAO.java
│   └── CRUD conversations
├── ✅ ContactDAO.java
│   └── Gestion relations contacts
├── ✅ AppelDAO.java
│   └── Persistance appels
├── ✅ NotificationDAO.java
│   └── Gestion notifications
├── ✅ FichierMediaDAO.java
│   └── Persistance fichiers médias
└── ✅ ConnexionDAO.java
    └── Historique connexions
```

#### Package: `org.example.tpchatjavafx.database`

```
📦 DATABASE LAYER (Legacy?)
├── ✅ DatabaseConnection.java
├── ✅ MessageDAO.java
└── ✅ UserDAO.java
```

### SERVICES

#### Package: `org.example.tpchatjavafx.service`

```
📦 BUSINESS LOGIC
├── ✅ AuthService.java
│   └── Authentification login/register
```

#### Package: `org.example.tpchatjavafx.auth`

```
📦 AUTH (Duplicate?)
└── ✅ AuthService.java
    └── (Duplicate de service/AuthService)
```

### COMMONS

#### Package: `org.example.tpchatjavafx.common`

```
📦 SHARED
└── ✅ MessageType.java
    └── Énumération types de messages
```

### RESOURCES (Interface & Styles)

```
📦 RESOURCES
├── 📁 fxml/
│   ├── ✅ login.fxml
│   ├── ✅ main-chat-view.fxml
│   ├── ✅ profile-view.fxml
│   └── ✅ video-call.fxml
├── 📁 css/
│   ├── ✅ styles.css
│   └── ✅ whatsapp.css
├── ✅ hello-view.fxml
└── 📁 sql/
    └── ✅ schema.sql (vide?)
```

### DATABASE

```
📁 SQL Files
└── ✅ chattapp_db.sql
    └── Schema complet (utilisateur, message, appel, etc.)
```

---

## ❌ CLASSES MANQUANTES (À CRÉER)

### PRIORITY 1 - AUDIO/VIDEO P2P

```
📦 AUDIO SERVICE
├── ❌ AudioCaptureService.java
│   └── Capturer audio depuis le micro
├── ❌ AudioPlaybackService.java
│   └── Jouer l'audio reçu
├── ❌ AudioCompressionService.java
│   └── Compression Opus/CELT
└── ❌ AudioTransmissionService.java
    └── Transmission UDP audio P2P

📦 VIDEO SERVICE
├── ❌ VideoCaptureService.java
│   └── Capturer vidéo depuis webcam
├── ❌ VideoDisplayService.java
│   └── Afficher vidéo reçue
├── ❌ VideoCompressionService.java
│   └── Compression H.264/VP8
└── ❌ VideoTransmissionService.java
    └── Transmission UDP vidéo P2P

📦 CALL MANAGEMENT
├── ❌ CallSignalingService.java (Client-side)
│   └── Signalisation d'appel (demande/acceptation)
├── ❌ CallManager.java (Server-side)
│   └── Gestion appels côté serveur
└── ❌ P2PConnectionManager.java
    └── Établir connexions P2P entre clients
```

### PRIORITY 2 - GROUPES

```
📦 GROUP MODELS
├── ❌ Groupe.java
│   └── Model groupe (nom, description, membres)
├── ❌ Reunion.java
│   └── Model réunion (participants, statut)

📦 GROUP DAOS
├── ❌ GroupeDAO.java
│   └── CRUD groupes
├── ❌ ReunionDAO.java
│   └── CRUD réunions
└── ❌ GroupMemberDAO.java
    └── Gestion membres groupes

📦 GROUP CONTROLLERS
├── ❌ GroupeController.java
│   └── UI contrôleur groupes
├── ❌ GroupeDialogController.java
│   └── Dialog création groupe
├── ❌ ReunionController.java
│   └── UI contrôleur réunions

📦 GROUP UI COMPONENTS
├── ❌ GroupeListCell.java
│   └── Rendu liste groupes
├── ❌ ReunionWindow.java
│   └── Fenêtre réunion multi-users
└── ❌ ParticipantGridView.java
    └── Grille vidéo multi-participants
```

### PRIORITY 3 - FILES & UTILITIES

```
📦 FILE SHARING
├── ❌ FileUploadService.java
│   └── Upload fichiers au serveur
├── ❌ FileDownloadService.java
│   └── Download fichiers
└── ❌ FileMessageHandler.java
    └── Handler messages fichiers

📦 AUDIO/VIDEO MIXING
├── ❌ AudioMixer.java
│   └── Mix audio multi-utilisateurs
├── ❌ VideoMixer.java
│   └── Mix vidéo (grille) multi-utilisateurs
└── ❌ MediaStreamManager.java
    └── Gestion globale des flux médias
```

### OPTIONAL - NICE TO HAVE

```
📦 UTILITIES
├── ❌ ValidationUtil.java
│   └── Validation inputs
├── ❌ EncryptionUtil.java
│   └── Chiffrement messages (optionnel)
├── ❌ LoggingUtil.java
│   └── Logging centralisé
└── ❌ ConfigurationManager.java
    └── Configuration application

📦 TESTING
├── ❌ MessageDAOTest.java
├── ❌ UserDAOTest.java
├── ❌ ChatServerTest.java
├── ❌ NetworkClientTest.java
├── ❌ AudioServiceTest.java
├── ❌ VideoServiceTest.java
└── ❌ IntegrationTest.java
```

---

## ⚠️ CLASSES À MODIFIER/AMÉLIORER

### ChatServer.java
```diff
+    private static final CallManager callManager = new CallManager();

     public static void main(String[] args) {
         System.out.println("=== Chat Server démarré sur le port " + PORT + " ===");
+        // Démarrer call manager
+        new Thread(callManager).start();
     }

     // Ajouter:
+    static void routeCallMessage(ChatMessage msg) {
+        callManager.handleCallMessage(msg);
+    }
```

### ClientHandler.java
```diff
     private void processLine(String line) {
         // ... existing code
         switch (msg.getType()) {
             case CALL_REQUEST -> handleCallRequest(msg);
             case CALL_ANSWER -> handleCallAnswer(msg);
             case CALL_REJECT -> handleCallReject(msg);
             case CALL_END -> handleCallEnd(msg);
+            case CALL_ICE_CANDIDATE -> handleIceCandidate(msg);
         }
     }

     // Ajouter:
+    private void handleCallRequest(ChatMessage msg) { }
+    private void handleCallAnswer(ChatMessage msg) { }
+    private void handleCallReject(ChatMessage msg) { }
+    private void handleCallEnd(ChatMessage msg) { }
```

### MainChatController.java
```diff
@FXML private void onVoiceCall() {
-    // Currently does nothing
+    if (currentPrivateTarget == null) {
+        showAlert("Sélectionnez un contact d'abord");
+        return;
+    }
+    
+    ChatMessage callRequest = new ChatMessage();
+    callRequest.setType(MessageType.CALL_REQUEST);
+    callRequest.setTarget(currentPrivateTarget);
+    callRequest.setCallType("AUDIO");
+    networkClient.send(callRequest);
}

@FXML private void onVideoCall() {
+    // Similaire à onVoiceCall mais avec VIDEO
}

+    // Ajouter onglet Groupes
+    @FXML private Tab groupTab;
+    @FXML private ListView<String> groupsListView;
+    
+    private void loadGroupes() {
+        // Charger groupes utilisateur depuis DAO
+    }
```

### NetworkClient.java
```diff
+    private Consumer<ChatMessage> onIncomingCall;
+    public void setOnIncomingCall(Consumer<ChatMessage> cb) {
+        onIncomingCall = cb;
+    }

     private void processMessageReceived(ChatMessage msg) {
         switch (msg.getType()) {
             case CALL_INCOMING -> {
+                if (onIncomingCall != null) onIncomingCall.accept(msg);
             }
         }
     }
```

### VideoCallWindow.java
```diff
-public class VideoCallWindow {
-    // Current: Juste UI vide
+public class VideoCallWindow {
+    private VideoCaptureService captureService;
+    private VideoTransmissionService transmissionService;
+    
+    public static void open(NetworkClient client, String me, String other) {
+        // ... setup UI
+        VideoCallController controller = loader.getController();
+        
+        // Démarrer services
+        controller.startVideoCapture();
+        controller.startVideoTransmission(other);
+    }
```

### VoiceCallWindow.java
```diff
-// Current: Juste UI vide
+// Ajouter logique réelle
-private static Label timerLabel;
+private static AudioCaptureService captureService;
+private static AudioPlaybackService playbackService;

+public static void open(String localUser, String remoteUser, Runnable onHangup) {
+    // ... setup UI
+    captureService = new AudioCaptureService();
+    playbackService = new AudioPlaybackService();
+    captureService.start();
+}
```

### Conversation.java
```diff
 public class Conversation {
     private int id;
+    private String type; // "INDIVIDUEL" ou "GROUPE"
+    private Integer groupeId; // Si type == "GROUPE"
     private LocalDateTime dateCreation;

     // getters/setters
 }
```

### MessageType.java
```diff
 public enum MessageType {
     LOGIN,
     REGISTER,
+    CALL_REQUEST,
+    CALL_ANSWER,
+    CALL_REJECT,
+    CALL_END,
+    CALL_INCOMING,
+    CALL_AUDIO_DATA,
+    CALL_VIDEO_DATA,
+    GROUP_CREATE,
+    GROUP_MESSAGE,
     // ... existing
 }
```

---

## 📊 RÉSUMÉ STATISTIQUE

### Classes Existantes: 26
```
Server:        3 (ChatServer, ClientHandler, ServerLauncher)
Client:        3 (ChatClientApp, NetworkClient, MainLauncher)
Controllers:   5 (Login, MainChat, Profile, VideoCall, VideoCallController)
Models:        9 (User, Utilisateur, Message, etc.)
DAOs:          8 (UtilisateurDAO, MessageDAO, etc.)
Services:      2 (AuthService duplicaté)
UI Components: 2 (UiMessage)
Utils:         1 (MessageType)
Other:         2 (HelloApplication, HelloController - legacy)
```

### Classes Manquantes: ~25
```
Audio Services:   4
Video Services:   4
Call Management:  3
Group Models:     2
Group DAOs:       3
Group Controllers: 3
UI Components:    3
File Services:    3
Utilities:        3
Testing:          7
```

### Ligne de Code Estimée
```
Existant:    ~3000 lines (50%)
À ajouter:   ~3000 lines (50%)
Total:       ~6000 lines
```

---

## 🎯 ORDRE DE CRÉATION RECOMMANDÉ

### Semaine 1:
1. AudioCaptureService
2. AudioPlaybackService
3. AudioTransmissionService
4. VideoCaptureService
5. VideoDisplayService
6. VideoTransmissionService
7. Modifier MainChatController (connecter UI)

### Semaine 2:
8. CallSignalingService
9. CallManager (server-side)
10. Groupe.java + GroupeDAO
11. GroupeController + UI

### Semaine 3:
12. Tests
13. UML Diagrammes
14. Rapport & Docs
