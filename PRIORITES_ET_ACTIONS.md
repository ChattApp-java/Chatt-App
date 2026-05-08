# 🚀 PRIORITÉS CRITIQUES & PLAN D'ACTION DÉTAILLÉ

**Date Limite:** 21 Mai 2026  
**Semaines Restantes:** 3 semaines

---

## 🔴 PRIORITÉ 1 - APPELS AUDIO/VIDÉO (VERSION 1 CORE FEATURE)

### ⚠️ ÉTAT CRITIQUE
Les appels sont la **FEATURE PRINCIPALE** du cahier des charges. Actuellement:
- ❌ Zéro fonctionnalité
- ⚠️ UI existe mais ne fait rien
- ❌ Aucun code réseau pour transmission
- ❌ Aucune signalisation d'appel

### À IMPLÉMENTER

#### 1. Signalisation d'appel (Demande/Acceptation)

**Fichiers à modifier:**
- `ClientHandler.java` - Ajouter handlers pour CALL_REQUEST, CALL_ANSWER, CALL_REJECT, CALL_END
- `ChatServer.java` - Ajouter routage des messages d'appel
- `MainChatController.java` - Connecter les boutons d'appel
- `NetworkClient.java` - Ajouter callbacks pour appels entrants

**Code à ajouter dans ClientHandler.java:**
```java
case CALL_REQUEST -> {
    if (username != null) handleCallRequest(msg);
}
case CALL_ANSWER -> {
    if (username != null) handleCallAnswer(msg);
}
case CALL_REJECT -> {
    if (username != null) handleCallReject(msg);
}
case CALL_END -> {
    if (username != null) handleCallEnd(msg);
}

private void handleCallRequest(ChatMessage msg) {
    String targetUser = msg.getTarget();
    String callType = msg.getCallType(); // "AUDIO" ou "VIDEO"
    
    // Envoyer notification à l'utilisateur cible
    Set<ClientHandler> targetHandlers = ChatServer.clients.get(targetUser);
    if (targetHandlers != null) {
        ChatMessage notification = new ChatMessage();
        notification.setType(MessageType.CALL_INCOMING);
        notification.setCaller(username);
        notification.setCallType(callType);
        
        for (ClientHandler handler : targetHandlers) {
            handler.send(notification);
        }
    } else {
        // Utilisateur non connecté
        send(new ChatMessage().setType(MessageType.CALL_NOT_AVAILABLE));
    }
}

private void handleCallAnswer(ChatMessage msg) {
    String callerId = msg.getCaller();
    // Établir la connexion P2P ou relayer pour les médias
}

private void handleCallReject(ChatMessage msg) {
    String callerId = msg.getCaller();
    // Notifier l'appelant que l'appel a été rejeté
}

private void handleCallEnd(ChatMessage msg) {
    // Fermer la connexion d'appel
}
```

---

#### 2. Transmission Audio P2P

**Options:**

**Option A: Sockets TCP séparés (SIMPLE)**
```
Avantage: Simple à implémenter
Inconvénient: Latence plus haute, pas de garantie temps réel

Implémentation:
1. Lors de CALL_ANSWER, ouvrir socket TCP supplémentaire
2. Capturer audio avec javax.sound.sampled
3. Transmettre PCM/WAV raw ou compressé
4. Recevoir et jouer l'audio
```

**Option B: Sockets UDP (MIEUX)**
```
Avantage: Meilleure latence, temps réel
Inconvénient: Plus complexe

Implémentation:
1. Ouvrir DatagramSocket UDP (port aléatoire)
2. Capturer audio
3. Compresser avec Opus (npm install opuscodec)
4. Envoyer paquets UDP
5. Recevoir et décompresser
```

**Option C: WebRTC (MEILLEUR)**
```
Avantage: P2P, NAT traversal, meilleure qualité
Inconvénient: Complexe, requiert librairie

Implémentation:
1. Importer org.webrtc:google-webrtc
2. Configurer PeerConnection
3. Créer offre/réponse SDP
4. Établir connexion P2P
```

**RECOMMANDATION POUR VOTRE CAS:** Option B (UDP) - bon équilibre simplicité/qualité

**Classes à créer:**

```java
// AudioCaptureService.java
public class AudioCaptureService {
    private SourceDataLine sourceDataLine;
    private byte[] buffer = new byte[4096];
    private int sampleRate = 16000;
    
    public void start() {
        AudioFormat format = new AudioFormat(sampleRate, 16, 1, true, false);
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
        sourceDataLine = (SourceDataLine) AudioSystem.getLine(info);
        sourceDataLine.open(format);
        sourceDataLine.start();
    }
    
    public byte[] captureAudio() {
        // Capturer depuis le micro
    }
    
    public void playAudio(byte[] audioData) {
        sourceDataLine.write(audioData, 0, audioData.length);
    }
}

// AudioTransmissionService.java
public class AudioTransmissionService {
    private DatagramSocket socket;
    private InetAddress remoteAddress;
    private int remotePort;
    private AudioCaptureService captureService;
    
    public void initiate(String remoteHost, int remotePort) throws Exception {
        socket = new DatagramSocket();
        this.remoteAddress = InetAddress.getByName(remoteHost);
        this.remotePort = remotePort;
        captureService = new AudioCaptureService();
        captureService.start();
        
        // Démarrer threads de capture et réception
        new Thread(this::sendAudioLoop).start();
        new Thread(this::receiveAudioLoop).start();
    }
    
    private void sendAudioLoop() {
        while (true) {
            byte[] audioData = captureService.captureAudio();
            byte[] compressed = compressOpus(audioData);
            DatagramPacket packet = new DatagramPacket(
                compressed, compressed.length,
                remoteAddress, remotePort
            );
            socket.send(packet);
        }
    }
    
    private void receiveAudioLoop() {
        byte[] buffer = new byte[4096];
        while (true) {
            DatagramPacket packet = new DatagramPacket(
                buffer, buffer.length
            );
            socket.receive(packet);
            byte[] decompressed = decompressOpus(packet.getData());
            captureService.playAudio(decompressed);
        }
    }
    
    private byte[] compressOpus(byte[] data) {
        // Utiliser Opus pour compression
        // ou utiliser PCM simple pour prototype
        return data;
    }
    
    private byte[] decompressOpus(byte[] data) {
        // Utiliser Opus pour décompression
        return data;
    }
}
```

**Modification MainChatController.java:**
```java
@FXML
private void onVoiceCall() {
    if (currentPrivateTarget == null) {
        showAlert("Sélectionnez un contact d'abord");
        return;
    }
    
    // Envoyer demande d'appel au serveur
    ChatMessage callRequest = new ChatMessage();
    callRequest.setType(MessageType.CALL_REQUEST);
    callRequest.setTarget(currentPrivateTarget);
    callRequest.setCallType("AUDIO");
    callRequest.setCaller(username);
    
    networkClient.send(callRequest);
    
    // Afficher UI d'attente
    showCallPending("Appel sortant...");
}
```

**Modification NetworkClient.java:**
```java
// Ajouter callback pour appel entrant
private Consumer<ChatMessage> onIncomingCall;
public void setOnIncomingCall(Consumer<ChatMessage> cb) {
    onIncomingCall = cb;
}

// Dans la boucle de réception:
case CALL_INCOMING -> {
    if (onIncomingCall != null) {
        onIncomingCall.accept(msg);
    }
}
```

---

#### 3. Transmission Vidéo P2P

**Similaire à audio mais plus complexe:**

```java
// VideoCaptureService.java
public class VideoCaptureService {
    private Webcam webcam;
    private BufferedImage currentFrame;
    
    public void start() {
        webcam = Webcam.getDefault();
        webcam.open();
        
        new Thread(() -> {
            while (webcam.isOpen()) {
                currentFrame = webcam.getImage();
            }
        }).start();
    }
    
    public byte[] captureFrame() {
        if (currentFrame == null) return new byte[0];
        
        // Convertir BufferedImage en bytes
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(currentFrame, "jpg", baos);
        return baos.toByteArray();
    }
    
    public void displayFrame(byte[] frameData) {
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(frameData));
        // Afficher dans ImageView
    }
}

// VideoTransmissionService.java - similaire à AudioTransmissionService
```

**Utiliser WebcamCapture correctement dans VideoCallController:**
```java
public class VideoCallController {
    @FXML private ImageView localVideoView;
    @FXML private ImageView remoteVideoView;
    
    private VideoCaptureService captureService;
    private VideoTransmissionService transmissionService;
    
    public void init(NetworkClient client, String me, String other) {
        // Démarrer capture vidéo locale
        captureService = new VideoCaptureService();
        captureService.start();
        
        // Démarrer transmission vidéo P2P
        transmissionService = new VideoTransmissionService();
        // ... établir connexion avec 'other'
        
        // Afficher la vidéo locale
        new Thread(() -> {
            while (true) {
                byte[] frame = captureService.captureFrame();
                localVideoView.setImage(convertToImage(frame));
                // Envoyer au serveur
                transmissionService.sendFrame(frame);
            }
        }).start();
    }
    
    public void onFrameReceived(byte[] frameData) {
        remoteVideoView.setImage(convertToImage(frameData));
    }
}
```

---

## 🟡 PRIORITÉ 2 - INTERFACE GROUPES (VERSION 1)

### État
- DB: ✅ Schema existe
- Backend: ⚠️ Partiellement
- Frontend: ❌ Zéro

### À Implémenter

#### 1. Modèles
```java
// Groupe.java
public class Groupe {
    private int id;
    private String nom;
    private String description;
    private int createurId;
    private LocalDateTime dateCreation;
    
    // getters/setters
}

// Modification Conversation.java
public class Conversation {
    private int id;
    private String type; // "INDIVIDUEL" ou "GROUPE"
    private int groupeId; // Si type == GROUPE
    // ...
}
```

#### 2. DAO
```java
// GroupeDAO.java
public class GroupeDAO {
    public Groupe creerGroupe(String nom, int createurId) { }
    public void ajouterMembre(int groupeId, int userId) { }
    public void supprimerMembre(int groupeId, int userId) { }
    public List<Groupe> getMesGroupes(int userId) { }
    public List<Integer> getMembresGroupes(int groupeId) { }
}
```

#### 3. Interface
```
Ajouter onglet "Groupes" dans MainChatController:
- Bouton "Nouveau groupe"
- Liste des groupes
- Double-clic pour ouvrir conversation de groupe

Dialog "Nouveau groupe":
- Champ "Nom"
- Liste de contacts à ajouter
- Bouton "Créer"
```

---

## 🟢 PRIORITÉ 3 - DIAGRAMMES UML (REQUIS POUR RAPPORT)

**Créer avec:**
- Draw.io (gratuit, online)
- Lucidchart
- StarUML
- PlantUML (code-based)

**Diagrammes requis:**

1. **Cas d'utilisation**
```
Actors: Utilisateur
Use cases:
- S'authentifier
- Envoyer message
- Lancer appel audio
- Lancer appel vidéo
- Gérer contacts
- Créer groupe
- Participer réunion
```

2. **Classes**
```
Inclure:
- Toutes les classes modèles
- DAOs
- Services
- Controllers
- Relations (héritage, composition, association)
```

3. **Séquences** (3-4 diagrammes)
```
- Login flow
- Send message flow
- Initiate video call flow
- Join group meeting flow
```

4. **Déploiement**
```
Client PC (JavaFX)
  |
  | TCP Socket (5555)
  |
Serveur (Java)
  |
  | JDBC
  |
MySQL DB
```

---

## 📋 CHECKLIST DE COMPLÉTUDE POUR DEADLINE

### Version 1 Minimale (21 Mai 2026)
- [ ] Authentification ✅ (fait)
- [ ] Chat texte ✅ (fait)
- [ ] Contacts ✅ (fait)
- [ ] **Appels audio P2P** ❌ (CRITIQUE)
- [ ] **Appels vidéo P2P** ❌ (CRITIQUE)
- [ ] **Interface groupes** ⚠️ (DB seul)
- [ ] **Diagrammes UML** ❌ (REQUIS)
- [ ] **JAR exécutable** ❌
- [ ] **Rapport technique** ⚠️
- [ ] **Démo vidéo** ❌

### Dépendances pour V2
- [ ] Chat groupe (requires: groupes UI)
- [ ] Réunions multi (requires: audio/vidéo P2P d'abord)

---

## 📌 RECOMMANDATION FINALE

**Focalisez la dernière semaine sur:**
1. **Appels audio/vidéo simples** (même qualité mauvaise, mais FONCTIONNELS)
2. **Interface groupes de base** (créer, ajouter membres, chat)
3. **Diagrammes UML** (utiliser Draw.io, 30-60 min par diagramme)
4. **JAR exécutable** (mvn package -DskipTests)
5. **Rapport & présentation**

La **démo vidéo** entre 2 clients est CRITIQUE - tout le reste peut être basique, mais les appels doivent marcher.
