package com.wechat.server;

import com.wechat.common.Protocol;
import com.wechat.common.Protocol.NetworkMessage;
import com.wechat.common.Protocol.MessageType;
import com.wechat.dao.UserDAO;
import com.wechat.model.User;
import com.wechat.model.User.UserStatus;
import com.wechat.service.AuthService;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Gère la communication avec un client connecté.
 * Un thread par client.
 */
public class ClientHandler implements Runnable {

    private static final Logger LOGGER = Logger.getLogger(ClientHandler.class.getName());

    private final Socket socket;
    private final ChatServer server;
    private ObjectInputStream input;
    private ObjectOutputStream output;

    private Long userId;
    private String username;
    private volatile boolean connected = false;

    public ClientHandler(Socket socket, ChatServer server) {
        this.socket = socket;
        this.server = server;
    }

    @Override
    public void run() {
        try {
            // Initialiser les streams (output AVANT input !)
            output = new ObjectOutputStream(socket.getOutputStream());
            output.flush();
            input = new ObjectInputStream(socket.getInputStream());
            connected = true;

            LOGGER.info("🔌 Streams initialisés pour " + socket.getInetAddress());

            // Boucle de lecture des messages
            while (connected && !socket.isClosed()) {
                try {
                    Object received = input.readObject();

                    if (received instanceof NetworkMessage) {
                        handleMessage((NetworkMessage) received);
                    } else {
                        LOGGER.warning("📛 Type de message inconnu : " + received.getClass().getName());
                    }

                } catch (ClassNotFoundException e) {
                    LOGGER.log(Level.WARNING, "Classe inconnue reçue", e);
                    sendMessage(NetworkMessage.error("Format de message invalide"));
                } catch (SocketException | EOFException e) {
                    LOGGER.info("🔌 Client déconnecté : " + (username != null ? username : socket.getInetAddress()));
                    break;
                }
            }

        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Erreur I/O client", e);
        } finally {
            disconnect();
        }
    }

    /**
     * Traite un message reçu du client
     */
    private void handleMessage(NetworkMessage msg) {
        LOGGER.info("📨 Reçu [" + msg.getType() + "] de " + (username != null ? username : "anonyme"));

        switch (msg.getType()) {
            case LOGIN_REQUEST -> handleLogin(msg);
            case REGISTER_REQUEST -> handleRegister(msg);
            case LOGOUT -> handleLogout();

            case TEXT_MESSAGE -> handleTextMessage(msg);
            case FILE_MESSAGE, IMAGE_MESSAGE, AUDIO_MESSAGE, VIDEO_MESSAGE -> handleMediaMessage(msg);

            case CALL_OFFER -> handleCallOffer(msg);
            case CALL_ANSWER -> handleCallAnswer(msg);
            case CALL_REJECT -> handleCallReject(msg);
            case CALL_END -> handleCallEnd(msg);

            case TYPING -> handleTyping(msg);
            case USER_STATUS -> handleUserStatus(msg);

            case GROUP_MESSAGE -> handleGroupMessage(msg);
            case GROUP_CREATE -> handleGroupCreate(msg);

            case PING -> sendMessage(NetworkMessage.pong());

            default -> {
                LOGGER.warning("⚠️ Type non géré : " + msg.getType());
                sendMessage(NetworkMessage.error("Type de message non supporté : " + msg.getType()));
            }
        }
    }

    // ===== HANDLERS AUTH =====

    private void handleLogin(NetworkMessage msg) {
        String usernameInput = msg.getContent();
        String passwordInput = msg.getPayload();

        try {
            UserDAO userDAO = new UserDAO();
            Optional<User> userOpt = userDAO.findByUsername(usernameInput);

            if (userOpt.isEmpty()) {
                // Essayer par email
                userOpt = userDAO.findByEmail(usernameInput);
            }

            if (userOpt.isPresent()) {
                User user = userOpt.get();

                // Vérification avec le nouveau hashage
                if (com.wechat.common.EncryptionUtil.verifyPassword(passwordInput, user.getPassword())
                        || user.getPassword().equals(passwordInput)) {  // Fallback pour anciens MDP

                    this.userId = user.getId();
                    this.username = user.getUsername();

                    // Mettre à jour le statut
                    userDAO.updateStatus(userId, UserStatus.ONLINE);

                    // Enregistrer le client
                    server.registerClient(userId, this);

                    // Envoyer la réponse
                    sendMessage(NetworkMessage.loginResponse(true, "Connexion réussie", userId));

                    // Envoyer la liste des utilisateurs en ligne
                    sendOnlineUsersList();

                    LOGGER.info("🔓 Login réussi : " + username + " (ID=" + userId + ")");
                } else {
                    sendMessage(NetworkMessage.loginResponse(false, "Mot de passe incorrect", null));
                }
            } else {
                sendMessage(NetworkMessage.loginResponse(false, "Utilisateur inconnu", null));
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur login", e);
            sendMessage(NetworkMessage.loginResponse(false, "Erreur serveur", null));
        }
    }

    private void handleRegister(NetworkMessage msg) {
        String nickname = msg.getContent();
        String payload = msg.getPayload();  // format: email|password

        try {
            String[] parts = payload.split("\\|");
            if (parts.length != 2) {
                sendMessage(new NetworkMessage(MessageType.REGISTER_RESPONSE) {{
                    setSuccess(false);
                    setContent("Format de requête invalide");
                }});
                return;
            }

            String email = parts[0];
            String password = parts[1];

            AuthService authService = new AuthService();
            AuthService.AuthResult result = authService.register(nickname, email, password, nickname);

            NetworkMessage response = new NetworkMessage(MessageType.REGISTER_RESPONSE);
            response.setSuccess(result.isSuccess());
            response.setContent(result.getMessage());
            if (result.isSuccess() && result.getUser() != null) {
                response.setSenderId(result.getUser().getId());
            }
            sendMessage(response);

            LOGGER.info("📝 Inscription : " + result.getMessage());

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur inscription", e);
            NetworkMessage response = new NetworkMessage(MessageType.REGISTER_RESPONSE);
            response.setSuccess(false);
            response.setContent("Erreur serveur : " + e.getMessage());
            sendMessage(response);
        }
    }

    private void handleLogout() {
        if (userId != null) {
            try {
                new UserDAO().updateStatus(userId, UserStatus.OFFLINE);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Erreur mise à jour statut logout", e);
            }
        }
        sendMessage(new NetworkMessage(MessageType.LOGOUT));
        disconnect();
    }

    // ===== HANDLERS CHAT =====

    private void handleTextMessage(NetworkMessage msg) {
        if (!isAuthenticated()) {
            sendMessage(NetworkMessage.error("Authentification requise"));
            return;
        }

        msg.setSenderId(userId);

        if (msg.getReceiverId() != null) {
            server.sendToUser(msg.getReceiverId(), msg);
            sendMessage(msg);  // Echo au sender
        } else if (msg.getConversationId() != null) {
            server.broadcast(msg);
        }

        LOGGER.info("💬 Message de " + username + " → " + msg.getContent());
    }

    private void handleMediaMessage(NetworkMessage msg) {
        if (!isAuthenticated()) {
            sendMessage(NetworkMessage.error("Authentification requise"));
            return;
        }
        msg.setSenderId(userId);

        if (msg.getReceiverId() != null) {
            server.sendToUser(msg.getReceiverId(), msg);
            sendMessage(msg);
        }
    }

    // ===== HANDLERS APPEL =====

    private void handleCallOffer(NetworkMessage msg) {
        if (!isAuthenticated()) return;
        msg.setSenderId(userId);
        server.sendToUser(msg.getReceiverId(), msg);
        LOGGER.info("📞 Appel offert par " + username + " → user " + msg.getReceiverId());
    }

    private void handleCallAnswer(NetworkMessage msg) {
        if (!isAuthenticated()) return;
        msg.setSenderId(userId);
        server.sendToUser(msg.getReceiverId(), msg);
        LOGGER.info("📞 Appel accepté par " + username);
    }

    private void handleCallReject(NetworkMessage msg) {
        if (!isAuthenticated()) return;
        msg.setSenderId(userId);
        server.sendToUser(msg.getReceiverId(), msg);
        LOGGER.info("📞 Appel refusé par " + username);
    }

    private void handleCallEnd(NetworkMessage msg) {
        if (!isAuthenticated()) return;
        msg.setSenderId(userId);
        server.sendToUser(msg.getReceiverId(), msg);
        LOGGER.info("📞 Appel terminé par " + username);
    }

    // ===== HANDLERS PRÉSENCE =====

    private void handleTyping(NetworkMessage msg) {
        if (!isAuthenticated()) return;
        msg.setSenderId(userId);
        if (msg.getReceiverId() != null) {
            server.sendToUser(msg.getReceiverId(), msg);
        }
    }

    private void handleUserStatus(NetworkMessage msg) {
        if (!isAuthenticated()) return;
    }

    private void handleGroupMessage(NetworkMessage msg) {
        if (!isAuthenticated()) return;
        msg.setSenderId(userId);
        server.broadcast(msg);
    }

    private void handleGroupCreate(NetworkMessage msg) {
        if (!isAuthenticated()) return;
        msg.setSenderId(userId);
        LOGGER.info("👥 Demande création groupe : " + msg.getContent());
        // TODO: Implémenter création groupe côté serveur
        sendMessage(NetworkMessage.error("Création de groupe non implémentée côté serveur"));
    }

    // ===== UTILITAIRES =====

    public void sendMessage(NetworkMessage msg) {
        if (output != null && connected) {
            try {
                output.writeObject(msg);
                output.flush();
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Erreur envoi message", e);
                disconnect();
            }
        }
    }

    public void disconnect() {
        if (!connected) return;
        connected = false;

        server.unregisterClient(userId, this);

        try {
            if (input != null) input.close();
            if (output != null) output.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Erreur fermeture socket", e);
        }

        LOGGER.info("🔌 Handler fermé pour " + (username != null ? username : socket.getInetAddress()));
    }

    private void sendOnlineUsersList() {
        NetworkMessage msg = new NetworkMessage(MessageType.USER_LIST);
        msg.setPayload(String.join(",",
                server.getOnlineUserIds().stream().map(String::valueOf).toList()));
        sendMessage(msg);
    }

    public boolean isAuthenticated() {
        return userId != null;
    }

    public boolean isConnected() {
        return connected && socket != null && !socket.isClosed() && socket.isConnected();
    }

    public Socket getSocket() {
        return socket;
    }

    public Long getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }
}