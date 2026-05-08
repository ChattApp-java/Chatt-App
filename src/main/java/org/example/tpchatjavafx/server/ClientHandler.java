package org.example.tpchatjavafx.server;

import org.example.tpchatjavafx.client.model.ChatMessage;
import org.example.tpchatjavafx.common.MessageType;
import org.example.tpchatjavafx.dao.ContactDAO;
import org.example.tpchatjavafx.dao.MessageDAO;
import org.example.tpchatjavafx.dao.UtilisateurDAO;
import org.example.tpchatjavafx.dao.ConversationDAO;
import org.example.tpchatjavafx.model.Message;
import org.example.tpchatjavafx.model.Utilisateur;
import org.example.tpchatjavafx.service.AuthService;

import java.io.*;
import java.net.Socket;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Gère la connexion d'UN client (thread dédié).
 * Responsabilités : auth, persistance messages, routage.
 */
public class ClientHandler implements Runnable {

    private final Socket     socket;
    private PrintWriter      out;
    private String           username;
    private int              userId;
    private final String     socketId;
    private static final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

    private final AuthService  authService = new AuthService();
    private final UtilisateurDAO userDAO   = new UtilisateurDAO();
    private final MessageDAO   messageDAO  = new MessageDAO();
    private final ConversationDAO convDAO  = new ConversationDAO();
    private final ContactDAO contactDAO    = new ContactDAO();

    public ClientHandler(Socket socket) { 
        this.socket = socket;
        this.socketId = UUID.randomUUID().toString();
    }

    public String getUsername() { return username; }
    public String getSocketId() { return socketId; }
    public Socket getSocket() { return socket; }

    // ── Envoi ─────────────────────────────────────────────────

    public void send(ChatMessage msg) {
        if (out != null) {
            out.println(msg.serialize());
            out.flush();
        }
    }

    // ── Boucle principale ──────────────────────────────────────

    @Override
    public void run() {
        try (BufferedReader in = new BufferedReader(
                     new InputStreamReader(socket.getInputStream()))) {
            out = new PrintWriter(
                    new OutputStreamWriter(socket.getOutputStream()), true);

            String line;
            while ((line = in.readLine()) != null) {
                processLine(line.trim());
            }
        } catch (IOException ignored) {
        } finally {
            cleanup();
        }
    }

    // ── Traitement d'une ligne ─────────────────────────────────

    private void processLine(String line) {
        if (line.isBlank()) return;
        ChatMessage msg = ChatMessage.deserialize(line);
        if (msg == null) return;

        switch (msg.getType()) {
            case LOGIN    -> handleLogin(msg);
            case REGISTER -> handleRegister(msg);
            case CONTACT_ADD -> {
                if (username != null) handleContactAdd(msg);
            }
            case CONTACT_LOAD -> {
                if (username != null) handleContactLoad();
            }
            case HISTORY_REQUEST -> {
                if (username != null) handleHistoryRequest(msg);
            }
            case LOGOUT   -> cleanup();
            
            // ===== APPELS =====
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
            
            default       -> {
                // N'autoriser que les utilisateurs authentifiés
                if (username != null) persistAndRoute(msg);
            }
        }
    }

    // ── Auth ──────────────────────────────────────────────────

    private void handleLogin(ChatMessage msg) {
        String uname    = msg.getFrom();
        String password = msg.getContent();

        Utilisateur user = authService.login(uname, password);
        if (user == null) {
            send(new ChatMessage(MessageType.AUTH_FAIL, "SERVER", uname, null, "Identifiants incorrects"));
            return;
        }
        
        setupSession(user);
    }

    private void handleRegister(ChatMessage msg) {
        String uname = msg.getFrom();
        String[] parts = msg.getContent().split("\\|", 2);
        String password = parts[0];
        String email = parts.length > 1 ? parts[1] : "";

        AuthService.RegisterResult res = authService.register(uname, password, email);
        if (!res.success()) {
            send(new ChatMessage(MessageType.AUTH_FAIL, "SERVER", uname, null, res.reason()));
            return;
        }
        
        try {
            Utilisateur user = userDAO.findByUsername(uname);
            setupSession(user);
        } catch (Exception e) {
            send(new ChatMessage(MessageType.AUTH_FAIL, "SERVER", uname, null, "Erreur serveur post-inscription"));
        }
    }

    private void handleContactAdd(ChatMessage msg) {
        String contactName = msg.getContent();
        if (contactName == null || contactName.isBlank()) return;

        try {
            Utilisateur contact = userDAO.findByUsername(contactName);
            if (contact == null) {
                send(new ChatMessage(MessageType.ERROR, "SERVER", username, null, "L'utilisateur " + contactName + " n'existe pas."));
                return;
            }

            if (contact.getId() == userId) {
                send(new ChatMessage(MessageType.ERROR, "SERVER", username, null, "Vous ne pouvez pas vous ajouter vous-même."));
                return;
            }

            boolean added = contactDAO.addContact(userId, contact.getId());
            if (added) {
                handleContactLoad(); // Envoyer la liste mise à jour
            } else {
                send(new ChatMessage(MessageType.ERROR, "SERVER", username, null, "L'utilisateur est déjà dans vos contacts."));
            }
        } catch (Exception e) {
            send(new ChatMessage(MessageType.ERROR, "SERVER", username, null, "Erreur lors de l'ajout du contact : " + e.getMessage()));
        }
    }

    private void handleContactLoad() {
        try {
            List<Utilisateur> contacts = contactDAO.getContacts(userId);
            String csv = contacts.stream()
                    .map(Utilisateur::getUsername)
                    .reduce((a, b) -> a + "," + b)
                    .orElse("");
            send(new ChatMessage(MessageType.CONTACT_LIST, "SERVER", username, null, csv));
        } catch (Exception e) {
            send(new ChatMessage(MessageType.ERROR, "SERVER", username, null, "Erreur chargement contacts : " + e.getMessage()));
        }
    }
    
    private void setupSession(Utilisateur user) {
        this.username = user.getUsername();
        this.userId = user.getId();
        ChatServer.registerClient(username, userId, this);
        
        // Envoi auth success avec l'ID
        send(new ChatMessage(MessageType.AUTH_SUCCESS, "SERVER", String.valueOf(userId), null, username));
        System.out.println("[Auth] Connecté : " + username);
        
        // Push messages non lus
        try {
            List<Message> unread = messageDAO.getUnreadMessages(userId);
            for (Message m : unread) {
                ChatMessage cmsg = new ChatMessage(
                    MessageType.valueOf(m.getType()),
                    m.getExpediteur().getUsername(),
                    this.username,
                    String.valueOf(m.getConversationId()),
                    m.getContenu()
                );
                cmsg.setMessageId(m.getId());
                if (m.getDateEnvoi() != null) {
                    cmsg.setTimestamp(m.getDateEnvoi().format(timeFormatter));
                }

                // Load binary data if it's a media message
                if (m.getType().contains("PRIVATE_AUDIO") || m.getType().contains("PRIVATE_IMAGE") || m.getType().contains("PRIVATE_FILE")) {
                    try {
                        org.example.tpchatjavafx.model.FichierMedia fm = fichierMediaDAO.findByMessageId(m.getId());
                        if (fm != null && fm.getCheminAcces() != null) {
                            java.io.File file = new java.io.File(fm.getCheminAcces());
                            if (file.exists()) {
                                cmsg.setBinaryData(java.nio.file.Files.readAllBytes(file.toPath()));
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("Erreur chargement média offline: " + e.getMessage());
                    }
                }

                send(cmsg);
                // Marquer lu ? Si le client l'affiche, oui
                messageDAO.markAsRead(m.getId());
            }
        } catch (Exception e) {
            System.err.println("Erreur push messages non lus : " + e.getMessage());
        }
    }

    private final org.example.tpchatjavafx.dao.FichierMediaDAO fichierMediaDAO = new org.example.tpchatjavafx.dao.FichierMediaDAO();

    // ── Persistance + routage ─────────────────────────────────

    private void persistAndRoute(ChatMessage msg) {
        msg.setTimestamp(LocalDateTime.now().format(timeFormatter));
        if (msg.getType() == MessageType.PRIVATE || msg.getType() == MessageType.PRIVATE_AUDIO || 
            msg.getType() == MessageType.PRIVATE_IMAGE || msg.getType() == MessageType.PRIVATE_FILE) {
            try {
                Utilisateur receiver = userDAO.findByUsername(msg.getTo());
                if (receiver != null) {
                    int convId = -1;
                    if (msg.getConversationId() != null && !msg.getConversationId().isEmpty()) {
                        convId = Integer.parseInt(msg.getConversationId());
                    } else {
                        org.example.tpchatjavafx.model.Conversation conv = convDAO.createConversation(userId, receiver.getId());
                        convId = conv.getId();
                    }

                    Message m = new Message();
                    m.setContenu(msg.getContent() != null ? msg.getContent() : "Fichier");
                    m.setType(msg.getType().name());
                    m.setExpediteurId(userId);
                    m.setDestinataireId(receiver.getId());
                    m.setConversationId(convId);
                    
                    Message saved = messageDAO.create(m);
                    msg.setMessageId(saved.getId()); 
                    
                    // Handling FichierMedia
                    if (msg.getBinaryData() != null && msg.getBinaryData().length > 0) {
                        String uploadDir = "server_uploads";
                        File dir = new File(uploadDir);
                        if (!dir.exists()) dir.mkdirs();
                        
                        String originalName = msg.getContent() != null ? msg.getContent() : "file.bin";
                        String safeName = System.currentTimeMillis() + "_" + originalName.replaceAll("[^a-zA-Z0-9.-]", "_");
                        File destFile = new File(dir, safeName);
                        
                        try (FileOutputStream fos = new FileOutputStream(destFile)) {
                            fos.write(msg.getBinaryData());
                        }
                        
                        org.example.tpchatjavafx.model.FichierMedia fm;
                        if (msg.getType() == MessageType.PRIVATE_AUDIO) {
                            org.example.tpchatjavafx.model.Vocal vocal = new org.example.tpchatjavafx.model.Vocal();
                            vocal.setDuree(0); // To compute or pass via msg later
                            fm = vocal;
                        } else {
                            fm = new org.example.tpchatjavafx.model.FichierMedia();
                        }
                        
                        fm.setMessageId(saved.getId());
                        fm.setNomFichier(originalName);
                        fm.setCheminAcces(destFile.getAbsolutePath());
                        fm.setTaille(msg.getBinaryData().length);
                        fm.setType(msg.getType() == MessageType.PRIVATE_AUDIO ? "AUDIO" : 
                                   (msg.getType() == MessageType.PRIVATE_IMAGE ? "IMAGE" : "FILE"));
                        
                        fichierMediaDAO.create(fm);
                    }

                    if (msg.getConversationId() == null || msg.getConversationId().isEmpty()) {
                        byte[] data = msg.getBinaryData();
                        msg = new ChatMessage(msg.getType(), msg.getFrom(), msg.getTo(), String.valueOf(convId), msg.getContent());
                        msg.setMessageId(saved.getId());
                        msg.setBinaryData(data);
                    }
                }
            } catch (Exception e) {
                System.err.println("[Handler] Persistance échouée : " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        ChatServer.handleMessage(msg, this);
    }

    // ── Nettoyage ─────────────────────────────────────────────

    private void cleanup() {
        if (username != null) {
            ChatServer.removeClient(username, userId, this);
        }
        try { if (!socket.isClosed()) socket.close(); }
        catch (IOException ignored) {}
    }

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

    private void handleHistoryRequest(ChatMessage msg) {
        String otherUsername = msg.getContent();
        try {
            Utilisateur other = userDAO.findByUsername(otherUsername);
            if (other == null) return;

            org.example.tpchatjavafx.model.Conversation conv = convDAO.findConversationBetween(userId, other.getId());
            if (conv != null) {
                List<Message> history = messageDAO.getHistory(conv.getId());
                for (Message m : history) {
                    String senderName = m.getExpediteur().getUsername();
                    String recipientName = senderName.equals(username) ? otherUsername : username;

                    ChatMessage syncMsg = new ChatMessage(
                        MessageType.SYNC_HISTORY,
                        senderName,
                        recipientName,
                        m.getType(), // Pass original type (PRIVATE_AUDIO, etc.)
                        m.getContenu()
                    );
                    syncMsg.setMessageId(m.getId());
                    if (m.getDateEnvoi() != null) {
                        syncMsg.setTimestamp(m.getDateEnvoi().format(timeFormatter));
                    }
                    
                    // Charger les données binaires si c'est un média
                    if (m.getType().contains("AUDIO") || m.getType().contains("IMAGE") || m.getType().contains("FILE")) {
                        try {
                            org.example.tpchatjavafx.model.FichierMedia fm = fichierMediaDAO.findByMessageId(m.getId());
                            if (fm != null && fm.getCheminAcces() != null) {
                                java.io.File file = new java.io.File(fm.getCheminAcces());
                                if (file.exists()) {
                                    syncMsg.setBinaryData(java.nio.file.Files.readAllBytes(file.toPath()));
                                }
                            }
                        } catch (Exception e) {
                            System.err.println("Erreur chargement média historique: " + e.getMessage());
                        }
                    }
                    send(syncMsg);
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur history: " + e.getMessage());
        }
    }
}
