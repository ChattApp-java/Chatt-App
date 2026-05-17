package com.wechat.client;

import com.wechat.common.Protocol;
import com.wechat.common.Protocol.NetworkMessage;
import com.wechat.common.Protocol.MessageType;
import com.wechat.model.User;
import com.wechat.model.User.UserStatus;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Client réseau WeChat.
 * Gère la connexion au serveur, l'authentification et la réception des messages.
 * Utilise un pattern Observer avec des Consumer callbacks.
 */
public class NetworkClient {

    private static final Logger LOGGER = Logger.getLogger(NetworkClient.class.getName());
    private static NetworkClient instance;

    private Socket socket;
    private ObjectOutputStream output;
    private ObjectInputStream input;
    private ExecutorService executor;

    private volatile boolean connected = false;
    private volatile boolean authenticated = false;
    private User currentUser;

    // ─── CALLBACKS / LISTENERS ───
    private Consumer<NetworkMessage> messageListener;
    private Consumer<User> userStatusListener;
    private Consumer<String> errorListener;
    private Consumer<Boolean> connectionListener;
    private Consumer<String> typingListener;
    private Runnable disconnectListener;

    // ─── RÉPONSES EN ATTENTE ───
    private final ConcurrentHashMap<MessageType, BlockingQueue<NetworkMessage>> pendingResponses
            = new ConcurrentHashMap<>();

    private NetworkClient() {
        this.executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "NetworkClient-Receiver");
            t.setDaemon(true);
            return t;
        });
    }

    /**
     * Singleton : un seul client réseau par application
     */
    public static synchronized NetworkClient getInstance() {
        if (instance == null) {
            instance = new NetworkClient();
        }
        return instance;
    }

    // ═══════════════════════════════════════════════════════════
    // CONNEXION
    // ═══════════════════════════════════════════════════════════

    /**
     * Connecte au serveur localhost:5000
     * @return true si connexion établie
     */
    public boolean connect() {
        return connect(Protocol.SERVER_HOST, Protocol.SERVER_PORT);
    }

    /**
     * Connecte au serveur spécifié
     */
    public boolean connect(String host, int port) {
        if (connected) {
            LOGGER.warning("Déjà connecté au serveur");
            return true;
        }

        try {
            LOGGER.info("🔌 Connexion au serveur " + host + ":" + port + "...");
            socket = new Socket(host, port);
            socket.setKeepAlive(true);

            // IMPORTANT : output AVANT input
            output = new ObjectOutputStream(socket.getOutputStream());
            output.flush();
            input = new ObjectInputStream(socket.getInputStream());

            connected = true;
            LOGGER.info("✅ Connecté au serveur !");

            // Démarrer le thread de réception
            startReceiverThread();

            // Notifier
            if (connectionListener != null) {
                connectionListener.accept(true);
            }

            return true;

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "❌ Impossible de se connecter au serveur", e);
            if (errorListener != null) {
                errorListener.accept("Connexion impossible : " + e.getMessage());
            }
            return false;
        }
    }

    /**
     * Déconnecte proprement
     */
    public void disconnect() {
        if (!connected) return;

        LOGGER.info("🔌 Déconnexion...");
        connected = false;
        authenticated = false;

        try {
            if (output != null) output.close();
            if (input != null) input.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Erreur fermeture socket", e);
        }

        executor.shutdown();
        pendingResponses.clear();

        if (disconnectListener != null) {
            disconnectListener.run();
        }
        if (connectionListener != null) {
            connectionListener.accept(false);
        }

        LOGGER.info("👋 Déconnecté");
    }

    // ═══════════════════════════════════════════════════════════
    // AUTHENTIFICATION
    // ═══════════════════════════════════════════════════════════

    /**
     * Authentifie un utilisateur.
     * @param email ou username
     * @param password
     * @return User si succès, null si échec
     */
    public User authenticate(String email, String password) {
        if (!connected) {
            LOGGER.warning("Non connecté au serveur");
            return null;
        }

        try {
            // Envoyer la requête de login
            NetworkMessage loginRequest = NetworkMessage.loginRequest(email, password);
            send(loginRequest);

            // Attendre la réponse
            NetworkMessage response = waitForResponse(MessageType.LOGIN_RESPONSE, 10000);

            if (response != null && response.isSuccess()) {
                authenticated = true;
                currentUser = new User();
                currentUser.setId(response.getSenderId());
                currentUser.setUsername(email);
                currentUser.setStatus(UserStatus.ONLINE);

                LOGGER.info("🔓 Authentifié : " + email + " (ID=" + response.getSenderId() + ")");
                return currentUser;
            } else {
                String error = response != null ? response.getContent() : "Timeout";
                LOGGER.warning("❌ Échec auth : " + error);
                if (errorListener != null) {
                    errorListener.accept(error);
                }
                return null;
            }

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur authentification", e);
            return null;
        }
    }

    /**
     * Inscrit un nouvel utilisateur.
     * @return true si inscription réussie
     */
    public boolean register(String name, String email, String password) {
        if (!connected) {
            LOGGER.warning("Non connecté au serveur");
            return false;
        }

        try {
            NetworkMessage registerRequest = new NetworkMessage(MessageType.REGISTER_REQUEST);
            registerRequest.setContent(name);      // nickname
            registerRequest.setPayload(email + "|" + password);  // email|password

            send(registerRequest);

            NetworkMessage response = waitForResponse(MessageType.REGISTER_RESPONSE, 10000);

            if (response != null && response.isSuccess()) {
                LOGGER.info("✅ Inscription réussie : " + email);
                return true;
            } else {
                String error = response != null ? response.getContent() : "Timeout";
                LOGGER.warning("❌ Échec inscription : " + error);
                if (errorListener != null) {
                    errorListener.accept(error);
                }
                return false;
            }

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur inscription", e);
            return false;
        }
    }

    /**
     * Déconnexion du compte (garde la connexion TCP)
     */
    public void logout() {
        if (connected) {
            send(new NetworkMessage(MessageType.LOGOUT));
        }
        authenticated = false;
        currentUser = null;
    }

    // ═══════════════════════════════════════════════════════════
    // ENVOI DE MESSAGES
    // ═══════════════════════════════════════════════════════════

    /**
     * Envoie un message texte privé
     */
    public void sendTextMessage(Long receiverId, String text) {
        if (!checkAuth()) return;

        NetworkMessage msg = NetworkMessage.textMessage(currentUser.getId(), receiverId, text);
        send(msg);
        LOGGER.info("📤 Message envoyé à user " + receiverId);
    }

    /**
     * Envoie un message dans une conversation (groupe ou privé)
     */
    public void sendConversationMessage(Long conversationId, String text) {
        if (!checkAuth()) return;

        NetworkMessage msg = NetworkMessage.textMessage(currentUser.getId(), null, text);
        msg.setConversationId(conversationId);
        send(msg);
    }

    /**
     * Envoie un message de groupe
     */
    public void sendGroupMessage(Long groupId, String text) {
        if (!checkAuth()) return;

        NetworkMessage msg = new NetworkMessage(MessageType.GROUP_MESSAGE);
        msg.setSenderId(currentUser.getId());
        msg.setConversationId(groupId);
        msg.setContent(text);
        send(msg);
    }

    /**
     * Notifie que l'utilisateur est en train d'écrire
     */
    public void sendTyping(Long conversationId) {
        if (!checkAuth()) return;

        NetworkMessage msg = NetworkMessage.typing(currentUser.getId(), conversationId);
        send(msg);
    }

    /**
     * Envoie une offre d'appel (WebRTC SDP)
     */
    public void sendCallOffer(Long calleeId, String sdpOffer) {
        if (!checkAuth()) return;

        NetworkMessage msg = NetworkMessage.callOffer(currentUser.getId(), calleeId, sdpOffer);
        send(msg);
    }

    /**
     * Répond à un appel (WebRTC SDP Answer)
     */
    public void sendCallAnswer(Long callerId, String sdpAnswer) {
        if (!checkAuth()) return;

        NetworkMessage msg = NetworkMessage.callAnswer(currentUser.getId(), callerId, sdpAnswer);
        send(msg);
    }

    /**
     * Termine un appel
     */
    public void sendCallEnd(Long peerId) {
        if (!checkAuth()) return;

        NetworkMessage msg = new NetworkMessage(MessageType.CALL_END);
        msg.setSenderId(currentUser.getId());
        msg.setReceiverId(peerId);
        send(msg);
    }

    // ═══════════════════════════════════════════════════════════
    // GROUPES
    // ═══════════════════════════════════════════════════════════

    /**
     * Demande la création d'un groupe
     */
    public void createGroup(String name, java.util.List<Long> memberIds) {
        if (!checkAuth()) return;

        NetworkMessage msg = new NetworkMessage(MessageType.GROUP_CREATE);
        msg.setSenderId(currentUser.getId());
        msg.setContent(name);
        msg.setPayload(String.join(",", memberIds.stream().map(String::valueOf).toList()));
        send(msg);
    }

    /**
     * Rejoint un groupe
     */
    public void joinGroup(Long groupId) {
        if (!checkAuth()) return;

        NetworkMessage msg = new NetworkMessage(MessageType.GROUP_JOIN);
        msg.setSenderId(currentUser.getId());
        msg.setConversationId(groupId);
        send(msg);
    }

    /**
     * Quitte un groupe
     */
    public void leaveGroup(Long groupId) {
        if (!checkAuth()) return;

        NetworkMessage msg = new NetworkMessage(MessageType.GROUP_LEAVE);
        msg.setSenderId(currentUser.getId());
        msg.setConversationId(groupId);
        send(msg);
    }

    // ═══════════════════════════════════════════════════════════
    // LISTENERS / CALLBACKS
    // ═══════════════════════════════════════════════════════════

    public void setMessageListener(Consumer<NetworkMessage> listener) {
        this.messageListener = listener;
    }

    public void setUserStatusListener(Consumer<User> listener) {
        this.userStatusListener = listener;
    }

    public void setErrorListener(Consumer<String> listener) {
        this.errorListener = listener;
    }

    public void setConnectionListener(Consumer<Boolean> listener) {
        this.connectionListener = listener;
    }

    public void setTypingListener(Consumer<String> listener) {
        this.typingListener = listener;
    }

    public void setDisconnectListener(Runnable listener) {
        this.disconnectListener = listener;
    }

    // ═══════════════════════════════════════════════════════════
    // GETTERS
    // ═══════════════════════════════════════════════════════════

    public boolean isConnected() {
        return connected && socket != null && !socket.isClosed() && socket.isConnected();
    }

    public boolean isAuthenticated() {
        return authenticated;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    // ═══════════════════════════════════════════════════════════
    // PRIVÉ
    // ═══════════════════════════════════════════════════════════

    private void send(NetworkMessage msg) {
        if (!connected || output == null) {
            LOGGER.warning("Impossible d'envoyer : non connecté");
            return;
        }

        try {
            output.writeObject(msg);
            output.flush();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Erreur envoi message", e);
            disconnect();
        }
    }

    private void startReceiverThread() {
        executor.execute(() -> {
            LOGGER.info("📡 Thread de réception démarré");

            while (connected && !socket.isClosed()) {
                try {
                    Object received = input.readObject();

                    if (received instanceof NetworkMessage) {
                        handleIncomingMessage((NetworkMessage) received);
                    }

                } catch (SocketException | EOFException e) {
                    LOGGER.info("🔌 Connexion fermée par le serveur");
                    break;
                } catch (IOException | ClassNotFoundException e) {
                    if (connected) {
                        LOGGER.log(Level.SEVERE, "Erreur réception", e);
                    }
                    break;
                }
            }

            if (connected) {
                LOGGER.warning("⚠️ Connexion perdue avec le serveur");
                connected = false;
                authenticated = false;

                if (disconnectListener != null) {
                    disconnectListener.run();
                }
                if (connectionListener != null) {
                    connectionListener.accept(false);
                }
            }
        });
    }

    private void handleIncomingMessage(NetworkMessage msg) {
        LOGGER.fine("📥 Reçu : " + msg.getType());

        // ─── Vérifier si c'est une réponse attendue ───
        BlockingQueue<NetworkMessage> queue = pendingResponses.get(msg.getType());
        if (queue != null) {
            queue.offer(msg);
            // Ne pas traiter comme message normal si c'est une réponse
            if (msg.getType() == MessageType.LOGIN_RESPONSE ||
                    msg.getType() == MessageType.REGISTER_RESPONSE) {
                return;
            }
        }

        switch (msg.getType()) {
            case TEXT_MESSAGE, FILE_MESSAGE, IMAGE_MESSAGE, AUDIO_MESSAGE, VIDEO_MESSAGE, GROUP_MESSAGE -> {
                if (messageListener != null) {
                    messageListener.accept(msg);
                }
            }

            case USER_STATUS -> {
                if (userStatusListener != null) {
                    User user = new User();
                    user.setId(msg.getSenderId());
                    user.setStatus(UserStatus.valueOf(msg.getContent()));
                    userStatusListener.accept(user);
                }
            }

            case TYPING, STOP_TYPING -> {
                if (typingListener != null) {
                    typingListener.accept("User " + msg.getSenderId() + " est en train d'écrire...");
                }
            }

            case CALL_OFFER, CALL_ANSWER, CALL_REJECT, CALL_END -> {
                if (messageListener != null) {
                    messageListener.accept(msg);
                }
            }

            case NOTIFICATION -> {
                LOGGER.info("🔔 Notification : " + msg.getContent());
            }

            case ERROR -> {
                LOGGER.warning("❌ Erreur serveur : " + msg.getErrorMessage());
                if (errorListener != null) {
                    errorListener.accept(msg.getErrorMessage());
                }
            }

            case PONG -> {
                LOGGER.fine("🏓 Pong reçu");
            }

            case USER_LIST -> {
                LOGGER.info("👥 Liste utilisateurs : " + msg.getPayload());
            }

            default -> LOGGER.fine("Type traité comme réponse : " + msg.getType());
        }
    }

    /**
     * Attend une réponse du serveur pour un type de message donné.
     * Utilise une BlockingQueue pour éviter les problèmes de listener.
     */
    private NetworkMessage waitForResponse(MessageType expectedType, long timeoutMs) {
        BlockingQueue<NetworkMessage> queue = new LinkedBlockingQueue<>(1);
        pendingResponses.put(expectedType, queue);

        try {
            NetworkMessage response = queue.poll(timeoutMs, TimeUnit.MILLISECONDS);
            return response;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        } finally {
            pendingResponses.remove(expectedType);
        }
    }

    private boolean checkAuth() {
        if (!connected) {
            LOGGER.warning("Non connecté");
            return false;
        }
        if (!authenticated) {
            LOGGER.warning("Non authentifié");
            return false;
        }
        return true;
    }
}