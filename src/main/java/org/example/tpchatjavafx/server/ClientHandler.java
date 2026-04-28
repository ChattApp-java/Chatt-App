package org.example.tpchatjavafx.server;

import org.example.tpchatjavafx.client.model.ChatMessage;
import org.example.tpchatjavafx.common.MessageType;
import org.example.tpchatjavafx.dao.MessageDAO;
import org.example.tpchatjavafx.dao.UtilisateurDAO;
import org.example.tpchatjavafx.dao.ConversationDAO;
import org.example.tpchatjavafx.model.Message;
import org.example.tpchatjavafx.model.Utilisateur;
import org.example.tpchatjavafx.service.AuthService;

import java.io.*;
import java.net.Socket;
import java.util.List;
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

    private final AuthService  authService = new AuthService();
    private final UtilisateurDAO userDAO   = new UtilisateurDAO();
    private final MessageDAO   messageDAO  = new MessageDAO();
    private final ConversationDAO convDAO  = new ConversationDAO();

    public ClientHandler(Socket socket) { 
        this.socket = socket;
        this.socketId = UUID.randomUUID().toString();
    }

    public String getUsername() { return username; }
    public String getSocketId() { return socketId; }

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
            case LOGOUT   -> cleanup();
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
    
    private void setupSession(Utilisateur user) {
        this.username = user.getUsername();
        this.userId = user.getId();
        ChatServer.registerClient(username, userId, this);
        
        // Envoi auth success avec l'ID
        send(new ChatMessage(MessageType.AUTH_SUCCESS, "SERVER", username, String.valueOf(userId), username));
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
}
