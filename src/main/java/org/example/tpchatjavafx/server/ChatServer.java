package org.example.tpchatjavafx.server;

import org.example.tpchatjavafx.client.model.ChatMessage;
import org.example.tpchatjavafx.common.MessageType;
import org.example.tpchatjavafx.dao.ConnexionDAO;
import org.example.tpchatjavafx.dao.GroupeMembreDAO;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Serveur TCP du chat. Gere les connexions et le routage multi-fenetres.
 */
public class ChatServer {

    static final int PORT = 5555;

    static final Map<String, Set<ClientHandler>> clients = new ConcurrentHashMap<>();
    private static final Map<Integer, Set<ClientHandler>> clientsById = new ConcurrentHashMap<>();
    private static final ConnexionDAO connexionDAO = new ConnexionDAO();
    private static final GroupeMembreDAO groupeMembreDAO = new GroupeMembreDAO();

    private static final UDPRelayServer udpRelayServer = new UDPRelayServer();
    private static final GroupManager groupManager = new GroupManager();
    private static final MeetingManager meetingManager = new MeetingManager(udpRelayServer);

    public static void main(String[] args) {
        try {
            udpRelayServer.start();
            Runtime.getRuntime().addShutdownHook(new Thread(ChatServer::shutdown, "chat-server-shutdown"));
            System.out.println("=== Chat Server demarre sur le port " + PORT + " ===");
            System.out.println("=== UDP Relay audio=" + udpRelayServer.getAudioPort() + " video=" + udpRelayServer.getVideoPort() + " ===");
        } catch (IOException e) {
            System.err.println("[Server] Impossible de demarrer le relais UDP : " + e.getMessage());
            return;
        }

        try (ServerSocket ss = new ServerSocket(PORT)) {
            while (true) {
                Socket socket = ss.accept();
                System.out.println("[Server] Nouvelle connexion : " + socket.getRemoteSocketAddress());
                new Thread(new ClientHandler(socket)).start();
            }
        } catch (IOException e) {
            System.err.println("[Server] Erreur : " + e.getMessage());
        } finally {
            shutdown();
        }
    }

    public static GroupManager getGroupManager() { return groupManager; }
    public static MeetingManager getMeetingManager() { return meetingManager; }
    public static UDPRelayServer getUdpRelayServer() { return udpRelayServer; }

    static void registerClient(String username, int userId, ClientHandler handler) {
        clients.computeIfAbsent(username, k -> Collections.synchronizedSet(new HashSet<>())).add(handler);
        clientsById.computeIfAbsent(userId, k -> Collections.synchronizedSet(new HashSet<>())).add(handler);
        System.out.println("[Server] Connecte : " + username + " (sessions: " + clients.get(username).size() + ")");
        try {
            connexionDAO.setEnLigne(userId, handler.getSocketId(), true);
        } catch (Exception e) {
            System.err.println("Erreur statut en ligne: " + e.getMessage());
        }
        broadcastUserStatus(username, "EN_LIGNE");
        broadcastUserList();
    }

    static void removeClient(String username, int userId, ClientHandler handler) {
        if (username == null) return;
        Set<ClientHandler> userHandlers = clients.get(username);
        if (userHandlers != null) {
            userHandlers.remove(handler);
            if (userHandlers.isEmpty()) clients.remove(username);
        }
        Set<ClientHandler> idHandlers = clientsById.get(userId);
        boolean userStillOnline = false;
        if (idHandlers != null) {
            idHandlers.remove(handler);
            userStillOnline = !idHandlers.isEmpty();
            if (!userStillOnline) clientsById.remove(userId);
        }
        System.out.println("[Server] Deconnecte : " + username);
        try {
            connexionDAO.setSessionClosed(userId, handler.getSocketId(), !userStillOnline);
        } catch (Exception e) {
            System.err.println("Erreur statut hors ligne: " + e.getMessage());
        }
        if (!userStillOnline) {
            broadcastUserStatus(username, "NON_CONNECTE");
            broadcastUserList();
        }
    }

    static void broadcastUserList() {
        ChatMessage msg = new ChatMessage(MessageType.USER_LIST, "SERVER", null, null, String.join(",", clients.keySet()));
        broadcastToAll(msg);
    }

    static void broadcastUserStatus(String username, String status) {
        broadcastToAll(new ChatMessage(MessageType.STATUS_UPDATE, username, null, null, status));
    }

    public static void broadcastToGroup(int groupeId, ChatMessage msg) {
        broadcastToGroupExcept(groupeId, msg, -1);
    }

    public static void broadcastToGroupExcept(int groupeId, ChatMessage msg, int exceptUserId) {
        try {
            for (Integer memberId : groupeMembreDAO.getMemberIds(groupeId)) {
                if (memberId == exceptUserId) continue;
                sendToUserId(memberId, msg);
            }
        } catch (Exception e) {
            System.err.println("Erreur broadcast groupe " + groupeId + ": " + e.getMessage());
        }
    }

    public static void sendToUserId(int userId, ChatMessage msg) {
        Set<ClientHandler> handlers = clientsById.get(userId);
        if (handlers != null) handlers.forEach(h -> h.send(msg));
    }

    private static void broadcastToAll(ChatMessage msg) {
        clients.values().forEach(handlers -> handlers.forEach(h -> h.send(msg)));
    }

    static void handleMessage(ChatMessage msg, ClientHandler from) {
        if (msg == null) return;
        MessageType type = msg.getType();
        if (isPrivateMessage(type)) {
            routePrivate(msg, from);
        } else if (isCallMessage(type)) {
            handleCallMessage(msg, from);
        } else if (type == MessageType.USER_LIST_REQUEST) {
            broadcastUserList();
        } else if (msg.getTo() != null && !msg.getTo().isBlank()) {
            forwardToTarget(msg);
        }
    }

    private static void routePrivate(ChatMessage msg, ClientHandler from) {
        forwardToTarget(msg);
        if (from.getUsername() != null) {
            Set<ClientHandler> senderHandlers = clients.get(from.getUsername());
            if (senderHandlers != null) {
                for (ClientHandler h : senderHandlers) {
                    if (h != from) h.send(msg);
                }
            }
        }
    }

    private static void forwardToTarget(ChatMessage msg) {
        Set<ClientHandler> targets = clients.get(msg.getTo());
        if (targets != null) targets.forEach(target -> target.send(msg));
    }

    private static void handleCallMessage(ChatMessage msg, ClientHandler from) {
        MessageType type = msg.getType();
        if (type == MessageType.CALL_REQUEST) {
            handleCallRequest(msg, from);
        } else if (type == MessageType.CALL_ANSWER) {
            handleCallAnswer(msg, from);
        } else {
            forwardToTarget(msg);
        }
    }

    private static boolean isPrivateMessage(MessageType type) {
        return type == MessageType.PRIVATE
                || type == MessageType.PRIVATE_AUDIO
                || type == MessageType.PRIVATE_IMAGE
                || type == MessageType.PRIVATE_FILE;
    }

    private static boolean isCallMessage(MessageType type) {
        return type == MessageType.VIDEO_CALL_REQUEST
                || type == MessageType.VIDEO_CALL_ACCEPT
                || type == MessageType.VIDEO_CALL_REJECT
                || type == MessageType.VIDEO_CALL_END
                || type == MessageType.VIDEO_FRAME
                || type == MessageType.VOICE_CALL_REQUEST
                || type == MessageType.VOICE_CALL_ACCEPT
                || type == MessageType.VOICE_CALL_REJECT
                || type == MessageType.VOICE_CALL_END
                || type == MessageType.VOICE_FRAME
                || type == MessageType.CALL_REQUEST
                || type == MessageType.CALL_ANSWER
                || type == MessageType.CALL_REJECT
                || type == MessageType.CALL_END
                || type == MessageType.CALL_INCOMING
                || type == MessageType.CALL_INFO;
    }

    private static void handleCallRequest(ChatMessage msg, ClientHandler from) {
        Set<ClientHandler> targets = clients.get(msg.getTo());
        if (targets == null || targets.isEmpty()) {
            from.send(new ChatMessage(MessageType.CALL_REJECT, "SERVER", msg.getFrom(), msg.getConversationId(), "Utilisateur non connecte"));
            return;
        }
        ChatMessage incomingMsg = new ChatMessage(MessageType.CALL_INCOMING, msg.getFrom(), msg.getTo(), msg.getConversationId(), msg.getCallType());
        incomingMsg.setCallType(msg.getCallType());
        targets.forEach(target -> target.send(incomingMsg));
    }

    private static void handleCallAnswer(ChatMessage msg, ClientHandler from) {
        ChatMessage infoMsg = new ChatMessage(MessageType.CALL_INFO, msg.getFrom(), msg.getTo(), msg.getConversationId(), "Connexion P2P etablie");
        infoMsg.setCallType(msg.getCallType());
        infoMsg.setRemoteHost(from.getSocket().getInetAddress().getHostAddress());
        infoMsg.setRemotePort(0);
        forwardToTarget(infoMsg);
        from.send(new ChatMessage(MessageType.CALL_ANSWER, msg.getTo(), msg.getFrom(), msg.getConversationId(), "Appel accepte"));
    }

    private static void shutdown() {
        udpRelayServer.close();
    }
}
