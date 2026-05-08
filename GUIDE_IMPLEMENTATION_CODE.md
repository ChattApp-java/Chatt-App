# 🔧 GUIDE D'IMPLÉMENTATION - CODE TEMPLATES

---

## 1️⃣ IMPLÉMENTER LES SERVICES AUDIO

### Créer: `AudioCaptureService.java`

```java
package org.example.tpchatjavafx.client.util;

import javax.sound.sampled.*;
import java.util.function.Consumer;

/**
 * Capture audio du microphone et envoie via callback.
 */
public class AudioCaptureService {
    
    private TargetDataLine microphone;
    private byte[] buffer = new byte[4096];
    private boolean isRunning = false;
    private Consumer<byte[]> onAudioCaptured;
    
    private static final AudioFormat FORMAT = new AudioFormat(
        16000,    // Sample rate 16kHz
        16,       // Bits per sample
        1,        // Mono
        true,     // Signed
        false     // Big-endian
    );
    
    public void setOnAudioCaptured(Consumer<byte[]> callback) {
        this.onAudioCaptured = callback;
    }
    
    public void start() throws LineUnavailableException {
        if (isRunning) return;
        
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, FORMAT);
        microphone = (TargetDataLine) AudioSystem.getLine(info);
        microphone.open(FORMAT);
        microphone.start();
        
        isRunning = true;
        
        // Thread de capture
        new Thread(this::captureLoop, "AudioCapture").start();
    }
    
    private void captureLoop() {
        while (isRunning) {
            int bytesRead = microphone.read(buffer, 0, buffer.length);
            if (bytesRead > 0 && onAudioCaptured != null) {
                byte[] audioData = new byte[bytesRead];
                System.arraycopy(buffer, 0, audioData, 0, bytesRead);
                onAudioCaptured.accept(audioData);
            }
        }
    }
    
    public void stop() {
        isRunning = false;
        if (microphone != null) {
            microphone.stop();
            microphone.close();
        }
    }
    
    public static AudioFormat getFormat() {
        return FORMAT;
    }
}
```

### Créer: `AudioPlaybackService.java`

```java
package org.example.tpchatjavafx.client.util;

import javax.sound.sampled.*;

/**
 * Joue l'audio reçu du réseau.
 */
public class AudioPlaybackService {
    
    private SourceDataLine speaker;
    private boolean isRunning = false;
    
    public void start() throws LineUnavailableException {
        if (isRunning) return;
        
        AudioFormat format = AudioCaptureService.getFormat();
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
        speaker = (SourceDataLine) AudioSystem.getLine(info);
        speaker.open(format);
        speaker.start();
        
        isRunning = true;
    }
    
    public void playAudio(byte[] audioData) {
        if (speaker != null && isRunning) {
            speaker.write(audioData, 0, audioData.length);
        }
    }
    
    public void stop() {
        isRunning = false;
        if (speaker != null) {
            speaker.drain();
            speaker.stop();
            speaker.close();
        }
    }
}
```

### Créer: `AudioTransmissionService.java`

```java
package org.example.tpchatjavafx.client.util;

import java.io.IOException;
import java.net.*;

/**
 * Transmission audio UDP entre 2 clients.
 * - Capture audio et envoie via UDP
 * - Reçoit audio UDP et joue
 */
public class AudioTransmissionService {
    
    private DatagramSocket socket;
    private InetAddress remoteAddress;
    private int remotePort;
    private AudioCaptureService captureService;
    private AudioPlaybackService playbackService;
    private boolean isRunning = false;
    
    public void initiate(String remoteHost, int remotePort) throws Exception {
        // Créer socket UDP sur port aléatoire
        socket = new DatagramSocket();
        this.remoteAddress = InetAddress.getByName(remoteHost);
        this.remotePort = remotePort;
        
        // Initialiser services audio
        captureService = new AudioCaptureService();
        playbackService = new AudioPlaybackService();
        
        // Callback pour envoyer audio capturé
        captureService.setOnAudioCaptured(audioData -> sendAudio(audioData));
        
        // Démarrer services
        captureService.start();
        playbackService.start();
        
        isRunning = true;
        
        // Thread de réception audio
        new Thread(this::receiveAudioLoop, "AudioReceive").start();
    }
    
    private void sendAudio(byte[] audioData) {
        if (socket == null || !isRunning) return;
        
        try {
            DatagramPacket packet = new DatagramPacket(
                audioData,
                audioData.length,
                remoteAddress,
                remotePort
            );
            socket.send(packet);
        } catch (IOException e) {
            System.err.println("Erreur envoi audio: " + e.getMessage());
        }
    }
    
    private void receiveAudioLoop() {
        byte[] buffer = new byte[4096];
        
        while (isRunning) {
            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                
                byte[] audioData = new byte[packet.getLength()];
                System.arraycopy(packet.getData(), 0, audioData, 0, packet.getLength());
                
                playbackService.playAudio(audioData);
            } catch (IOException e) {
                if (!isRunning) break; // Normal shutdown
                System.err.println("Erreur réception audio: " + e.getMessage());
            }
        }
    }
    
    public int getLocalPort() {
        return socket != null ? socket.getLocalPort() : -1;
    }
    
    public void stop() {
        isRunning = false;
        
        if (captureService != null) captureService.stop();
        if (playbackService != null) playbackService.stop();
        if (socket != null) socket.close();
    }
}
```

---

## 2️⃣ IMPLÉMENTER SIGNALISATION D'APPEL

### Modifier: `MessageType.java`

```java
package org.example.tpchatjavafx.common;

public enum MessageType {
    LOGIN,
    REGISTER,
    MESSAGE,
    USER_LIST,
    CONTACT_LIST,
    CONTACT_ADD,
    HISTORY_REQUEST,
    HISTORY_RESPONSE,
    USER_STATUS,
    LOGOUT,
    
    // ===== APPELS =====
    CALL_REQUEST,          // Demander appel
    CALL_ANSWER,           // Accepter appel
    CALL_REJECT,           // Refuser appel
    CALL_END,              // Terminer appel
    CALL_INCOMING,         // Notification appel entrant
    CALL_INFO,             // Info pour connexion P2P (IP, port, etc)
    
    ERROR
}
```

### Modifier: `ChatMessage.java`

```java
package org.example.tpchatjavafx.client.model;

/**
 * Message sérialisé pour transmission TCP.
 */
public class ChatMessage {
    private String type;           // MessageType
    private String from;           // Émetteur
    private String to;             // Destinataire
    private String content;        // Contenu
    private String callType;       // "AUDIO" ou "VIDEO"
    private String remoteHost;     // Host pour P2P
    private int remotePort;        // Port pour P2P
    private long timestamp;
    
    // Serialization format: type|from|to|content|callType|remoteHost|remotePort|timestamp
    
    public static ChatMessage deserialize(String line) {
        if (line == null || line.isEmpty()) return null;
        
        String[] parts = line.split("\\|", -1);
        if (parts.length < 3) return null;
        
        ChatMessage msg = new ChatMessage();
        msg.type = parts[0];
        msg.from = parts[1];
        msg.to = parts[2];
        msg.content = parts.length > 3 ? parts[3] : "";
        msg.callType = parts.length > 4 ? parts[4] : "";
        msg.remoteHost = parts.length > 5 ? parts[5] : "";
        msg.remotePort = parts.length > 6 ? Integer.parseInt(parts[6]) : 0;
        msg.timestamp = System.currentTimeMillis();
        
        return msg;
    }
    
    public String serialize() {
        return String.join("|",
            type != null ? type : "",
            from != null ? from : "",
            to != null ? to : "",
            content != null ? content : "",
            callType != null ? callType : "",
            remoteHost != null ? remoteHost : "",
            String.valueOf(remotePort),
            String.valueOf(timestamp)
        );
    }
    
    // === GETTERS/SETTERS ===
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    
    public String getFrom() { return from; }
    public void setFrom(String from) { this.from = from; }
    
    public String getTo() { return to; }
    public void setTo(String to) { this.to = to; }
    
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    
    public String getCallType() { return callType; }
    public void setCallType(String callType) { this.callType = callType; }
    
    public String getRemoteHost() { return remoteHost; }
    public void setRemoteHost(String remoteHost) { this.remoteHost = remoteHost; }
    
    public int getRemotePort() { return remotePort; }
    public void setRemotePort(int remotePort) { this.remotePort = remotePort; }
    
    public long getTimestamp() { return timestamp; }
}
```

### Modifier: `ClientHandler.java` (Server)

```java
// Ajouter dans processLine():
case "CALL_REQUEST" -> {
    if (username != null) handleCallRequest(msg);
}
case "CALL_ANSWER" -> {
    if (username != null) handleCallAnswer(msg);
}
case "CALL_REJECT" -> {
    if (username != null) handleCallReject(msg);
}
case "CALL_END" -> {
    if (username != null) handleCallEnd(msg);
}

// Ajouter ces méthodes:

private void handleCallRequest(ChatMessage msg) {
    String targetUser = msg.getTo();
    String callType = msg.getCallType();
    
    System.out.println("[Server] " + username + " appelle " + targetUser + 
                       " (" + callType + ")");
    
    Set<ClientHandler> targetHandlers = ChatServer.clients.get(targetUser);
    if (targetHandlers != null && !targetHandlers.isEmpty()) {
        // Utilisateur en ligne - envoyer notification
        ChatMessage notification = new ChatMessage();
        notification.setType("CALL_INCOMING");
        notification.setFrom(username);
        notification.setTo(targetUser);
        notification.setCallType(callType);
        notification.setRemoteHost(socket.getInetAddress().getHostAddress());
        notification.setRemotePort(9999); // Port local pour réception
        
        for (ClientHandler handler : targetHandlers) {
            handler.send(notification);
        }
    } else {
        // Utilisateur non connecté
        ChatMessage response = new ChatMessage();
        response.setType("ERROR");
        response.setContent("Utilisateur hors ligne");
        send(response);
    }
}

private void handleCallAnswer(ChatMessage msg) {
    String callerId = msg.getFrom();
    String targetUser = msg.getTo();
    
    System.out.println("[Server] " + username + " accepte appel de " + callerId);
    
    Set<ClientHandler> callerHandlers = ChatServer.clients.get(callerId);
    if (callerHandlers != null && !callerHandlers.isEmpty()) {
        ChatMessage answer = new ChatMessage();
        answer.setType("CALL_ANSWER");
        answer.setFrom(username);
        answer.setTo(callerId);
        answer.setRemoteHost(socket.getInetAddress().getHostAddress());
        answer.setRemotePort(10000); // Port local pour réception
        
        for (ClientHandler handler : callerHandlers) {
            handler.send(answer);
        }
    }
}

private void handleCallReject(ChatMessage msg) {
    String callerId = msg.getFrom();
    
    System.out.println("[Server] " + username + " refuse appel de " + callerId);
    
    Set<ClientHandler> callerHandlers = ChatServer.clients.get(callerId);
    if (callerHandlers != null) {
        ChatMessage rejection = new ChatMessage();
        rejection.setType("CALL_REJECT");
        rejection.setFrom(username);
        
        for (ClientHandler handler : callerHandlers) {
            handler.send(rejection);
        }
    }
}

private void handleCallEnd(ChatMessage msg) {
    System.out.println("[Server] " + username + " termine appel");
    // Notification au client distant (optionnel)
}
```

---

## 3️⃣ CONNECTER UI AU CODE

### Modifier: `MainChatController.java`

```java
@FXML private Button voiceCallButton;
@FXML private Button videoCallButton;

private AudioTransmissionService audioService;
private String incomingCallFrom = null;

@Override
public void init(NetworkClient networkClient, String username, int userId) {
    this.networkClient = networkClient;
    this.username = username;
    this.currentUser = new Utilisateur(); // TODO: charger du DB
    
    // ===== CALLBACKS RÉSEAU =====
    networkClient.setOnIncomingCall(msg -> {
        handleIncomingCall(msg);
    });
    
    networkClient.setOnCallAnswered(msg -> {
        handleCallAnswered(msg);
    });
    
    networkClient.setOnCallRejected(msg -> {
        handleCallRejected(msg);
    });
    
    // ... resto du code
}

@FXML
private void onVoiceCall() {
    if (currentPrivateTarget == null) {
        showAlert("Sélectionnez un contact d'abord");
        return;
    }
    
    // Vérifier que le contact est en ligne
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
    
    // UI: afficher état "Appel en cours..."
    showCallPending(currentPrivateTarget, "Appel vocal en cours...");
}

@FXML
private void onVideoCall() {
    // Similaire à voice call mais avec callType = "VIDEO"
    if (currentPrivateTarget == null) {
        showAlert("Sélectionnez un contact d'abord");
        return;
    }
    
    ChatMessage callRequest = new ChatMessage();
    callRequest.setType("CALL_REQUEST");
    callRequest.setFrom(username);
    callRequest.setTo(currentPrivateTarget);
    callRequest.setCallType("VIDEO");
    
    networkClient.send(callRequest);
    showCallPending(currentPrivateTarget, "Appel vidéo en cours...");
}

private void handleIncomingCall(ChatMessage msg) {
    String caller = msg.getFrom();
    String callType = msg.getCallType();
    
    System.out.println("[UI] Appel entrant de " + caller + " (" + callType + ")");
    
    incomingCallFrom = caller;
    
    // Afficher dialog
    Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
    alert.setTitle("Appel entrant");
    alert.setHeaderText("Appel " + callType.toLowerCase() + " de " + caller);
    alert.setContentText("Accepter l'appel?");
    
    alert.showAndWait().ifPresent(result -> {
        if (result == ButtonType.OK) {
            // Accepter appel
            ChatMessage answer = new ChatMessage();
            answer.setType("CALL_ANSWER");
            answer.setFrom(username);
            answer.setTo(caller);
            answer.setCallType(callType);
            
            networkClient.send(answer);
            
            // Démarrer transmission
            if (callType.equals("AUDIO")) {
                startAudioCall(caller, false);
            } else {
                startVideoCall(caller, false);
            }
        } else {
            // Refuser
            ChatMessage reject = new ChatMessage();
            reject.setType("CALL_REJECT");
            reject.setFrom(username);
            reject.setTo(caller);
            
            networkClient.send(reject);
        }
    });
}

private void handleCallAnswered(ChatMessage msg) {
    String answerer = msg.getFrom();
    String remoteHost = msg.getRemoteHost();
    int remotePort = msg.getRemotePort();
    
    System.out.println("[UI] " + answerer + " a accepté l'appel");
    
    // Démarrer transmission avec P2P
    startAudioCall(answerer, true, remoteHost, remotePort);
}

private void startAudioCall(String remoteUser, boolean isCaller) {
    try {
        // Afficher window VoiceCallWindow
        org.example.tpchatjavafx.client.voice.VoiceCallWindow.open(
            username, 
            remoteUser,
            this::endAudioCall
        );
        
        // TODO: Implémenter la transmission audio réelle
        // Actuellement c'est juste la UI
    } catch (Exception e) {
        e.printStackTrace();
        showAlert("Erreur: " + e.getMessage());
    }
}

private void startAudioCall(String remoteUser, boolean isCaller, 
                           String remoteHost, int remotePort) {
    try {
        if (audioService != null) audioService.stop();
        
        audioService = new AudioTransmissionService();
        audioService.initiate(remoteHost, remotePort);
        
        org.example.tpchatjavafx.client.voice.VoiceCallWindow.open(
            username,
            remoteUser,
            this::endAudioCall
        );
    } catch (Exception e) {
        e.printStackTrace();
        showAlert("Erreur audio: " + e.getMessage());
    }
}

private void endAudioCall() {
    if (audioService != null) {
        audioService.stop();
        audioService = null;
    }
    
    // Notifier l'autre utilisateur
    ChatMessage endMsg = new ChatMessage();
    endMsg.setType("CALL_END");
    endMsg.setFrom(username);
    endMsg.setTo(incomingCallFrom != null ? incomingCallFrom : currentPrivateTarget);
    
    networkClient.send(endMsg);
    
    incomingCallFrom = null;
}

private void startVideoCall(String remoteUser, boolean isCaller) {
    // TODO: Similaire mais avec vidéo
}

private void showCallPending(String user, String message) {
    // TODO: Afficher un dialog/notification
    System.out.println(message);
}

private void handleCallRejected(ChatMessage msg) {
    showAlert("L'appel a été refusé");
}
```

### Modifier: `NetworkClient.java`

```java
private Consumer<ChatMessage> onIncomingCall;
private Consumer<ChatMessage> onCallAnswered;
private Consumer<ChatMessage> onCallRejected;

public void setOnIncomingCall(Consumer<ChatMessage> cb) {
    this.onIncomingCall = cb;
}

public void setOnCallAnswered(Consumer<ChatMessage> cb) {
    this.onCallAnswered = cb;
}

public void setOnCallRejected(Consumer<ChatMessage> cb) {
    this.onCallRejected = cb;
}

// Dans la boucle de réception:
private void processMessageReceived(ChatMessage msg) {
    switch (msg.getType()) {
        case "MESSAGE" -> {
            if (onMessageReceived != null) {
                onMessageReceived.accept(msg);
            }
        }
        case "CALL_INCOMING" -> {
            if (onIncomingCall != null) {
                onIncomingCall.accept(msg);
            }
        }
        case "CALL_ANSWER" -> {
            if (onCallAnswered != null) {
                onCallAnswered.accept(msg);
            }
        }
        case "CALL_REJECT" -> {
            if (onCallRejected != null) {
                onCallRejected.accept(msg);
            }
        }
        // ... resto
    }
}
```

---

## 4️⃣ IMPLÉMENTER VIDÉO (SIMPLIFIÉ)

### Créer: `VideoCaptureService.java`

```java
package org.example.tpchatjavafx.client.util;

import com.github.sarxos.webcam.Webcam;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.image.PixelWriter;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.function.Consumer;

/**
 * Capture vidéo de la webcam.
 */
public class VideoCaptureService {
    
    private Webcam webcam;
    private Consumer<byte[]> onFrameCaptured;
    private boolean isRunning = false;
    
    public void setOnFrameCaptured(Consumer<byte[]> callback) {
        this.onFrameCaptured = callback;
    }
    
    public void start() {
        if (isRunning) return;
        
        webcam = Webcam.getDefault();
        if (webcam == null) {
            System.err.println("Aucune webcam disponible");
            return;
        }
        
        webcam.open();
        isRunning = true;
        
        // Thread de capture
        new Thread(this::captureLoop, "VideoCapture").start();
    }
    
    private void captureLoop() {
        int frameSkip = 0;
        
        while (isRunning) {
            BufferedImage frame = webcam.getImage();
            if (frame != null) {
                // Envoyer 1 frame tous les 3 (3-4 FPS)
                if (frameSkip++ % 3 == 0) {
                    byte[] jpegData = convertToJPEG(frame);
                    if (onFrameCaptured != null) {
                        onFrameCaptured.accept(jpegData);
                    }
                }
            }
            
            try {
                Thread.sleep(30); // ~33 FPS capture, skip 2/3
            } catch (InterruptedException e) {
                break;
            }
        }
    }
    
    private byte[] convertToJPEG(BufferedImage image) {
        try {
            // Redimensionner pour réduire bande passante
            BufferedImage resized = new BufferedImage(
                320, 240, BufferedImage.TYPE_INT_RGB
            );
            java.awt.Graphics2D g2d = resized.createGraphics();
            g2d.drawImage(image, 0, 0, 320, 240, null);
            g2d.dispose();
            
            // Convertir en JPEG
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(resized, "jpg", baos);
            return baos.toByteArray();
        } catch (Exception e) {
            System.err.println("Erreur conversion JPEG: " + e.getMessage());
            return new byte[0];
        }
    }
    
    public void stop() {
        isRunning = false;
        if (webcam != null && webcam.isOpen()) {
            webcam.close();
        }
    }
}
```

---

## ✅ CHECKLIST D'IMPLÉMENTATION

```
[ ] 1. Créer AudioCaptureService
[ ] 2. Créer AudioPlaybackService
[ ] 3. Créer AudioTransmissionService
[ ] 4. Modifier MessageType.java (ajouter CALL_*)
[ ] 5. Modifier ChatMessage.java (ajouter champs)
[ ] 6. Modifier ClientHandler.java (handlers appels)
[ ] 7. Modifier MainChatController.java (connecter UI)
[ ] 8. Modifier NetworkClient.java (callbacks)
[ ] 9. Créer VideoCaptureService
[ ] 10. Créer VideoTransmissionService (similaire audio)
[ ] 11. Tester Audio P2P entre 2 clients
[ ] 12. Tester Vidéo P2P entre 2 clients
[ ] 13. Ajouter interface Groupes
[ ] 14. Générer JAR
[ ] 15. Créer diagrammes UML
```

---

## 🎯 PROCHAINES ÉTAPES

1. **Aujourd'hui:** Lire ce guide
2. **Demain:** Créer les 3 services audio
3. **Jour 3:** Implémenter signalisation
4. **Jour 4:** Connecter UI
5. **Jour 5:** Tester audio entre 2 clients
6. **Jour 6-7:** Répéter pour vidéo

**Bonne luck!** 🚀
