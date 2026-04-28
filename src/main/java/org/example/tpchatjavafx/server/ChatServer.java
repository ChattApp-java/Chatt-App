package org.example.tpchatjavafx.server;

import org.example.tpchatjavafx.client.model.ChatMessage;
import org.example.tpchatjavafx.common.MessageType;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

/**
 * Serveur TCP du chat — port 5555.
 * Gère : auth, messagerie, appels audio/vidéo, diffusion liste utilisateurs.
 */
public class ChatServer {

    static final int PORT = 5555;

    /** username → ClientHandler (utilisateurs connectés et authentifiés) */
    static final Map<String, ClientHandler> clients =
            Collections.synchronizedMap(new LinkedHashMap<>());

    /** groupId → ensemble de ClientHandlers */
    static final Map<String, Set<ClientHandler>> groups =
            Collections.synchronizedMap(new HashMap<>());

    // ── Démarrage ─────────────────────────────────────────────

    public static void main(String[] args) {
        System.out.println("=== Chat Server démarré sur le port " + PORT + " ===");
        try (ServerSocket ss = new ServerSocket(PORT)) {
            while (true) {
                Socket socket = ss.accept();
                System.out.println("[Server] Nouvelle connexion : " + socket.getRemoteSocketAddress());
                new Thread(new ClientHandler(socket)).start();
            }
        } catch (IOException e) {
            System.err.println("[Server] Erreur : " + e.getMessage());
        }
    }

    // ── Gestion des sessions ───────────────────────────────────

    static void registerClient(String username, ClientHandler handler) {
        clients.put(username, handler);
        System.out.println("[Server] Connecté : " + username + " | Total : " + clients.size());
        broadcastUserList();
    }

    static void removeClient(String username) {
        if (username == null) return;
        clients.remove(username);
        synchronized (groups) {
            for (Set<ClientHandler> members : groups.values())
                members.removeIf(h -> username.equals(h.getUsername()));
        }
        System.out.println("[Server] Déconnecté : " + username + " | Total : " + clients.size());
        broadcastUserList();
    }

    /** Diffuse la liste des utilisateurs connectés à tous les clients. */
    static void broadcastUserList() {
        String userListContent;
        synchronized (clients) {
            userListContent = String.join(",", clients.keySet());
        }
        ChatMessage msg = new ChatMessage(
                MessageType.USER_LIST, "SERVER", null, null, userListContent);
        synchronized (clients) {
            for (ClientHandler h : clients.values()) h.send(msg);
        }
    }

    // ── Routage des messages ───────────────────────────────────

    static void handleMessage(ChatMessage msg, ClientHandler from) {
        if (msg == null) return;
        switch (msg.getType()) {
            case PRIVATE, PRIVATE_AUDIO, PRIVATE_IMAGE, PRIVATE_FILE -> routePrivate(msg, from);
            case JOIN_GROUP  -> joinGroup(msg, from);
            case GROUP, GROUP_AUDIO, GROUP_IMAGE, GROUP_FILE         -> routeGroup(msg);
            case VIDEO_CALL_REQUEST, VIDEO_CALL_ACCEPT,
                 VIDEO_CALL_REJECT,  VIDEO_CALL_END, VIDEO_FRAME,
                 VOICE_CALL_REQUEST, VOICE_CALL_ACCEPT,
                 VOICE_CALL_REJECT,  VOICE_CALL_END, VOICE_FRAME    -> forwardToTarget(msg);
            default -> {} // LOGIN / REGISTER traités dans ClientHandler.run()
        }
    }

    // ── Helpers ───────────────────────────────────────────────

    private static void routePrivate(ChatMessage msg, ClientHandler from) {
        ClientHandler target = clients.get(msg.getTo());
        if (target != null) {
            target.send(msg);
        } else {
            from.send(new ChatMessage(
                    MessageType.SYSTEM, "SERVER", msg.getFrom(), null,
                    "Utilisateur '" + msg.getTo() + "' non connecté."));
        }
    }

    private static void joinGroup(ChatMessage msg, ClientHandler handler) {
        String gid = msg.getGroupId();
        if (gid == null || gid.isBlank()) return;
        groups.computeIfAbsent(gid, k -> Collections.synchronizedSet(new HashSet<>())).add(handler);
        handler.send(new ChatMessage(
                MessageType.SYSTEM, "SERVER", handler.getUsername(), gid, "Groupe rejoint : " + gid));
    }

    private static void routeGroup(ChatMessage msg) {
        Set<ClientHandler> members = groups.get(msg.getGroupId());
        if (members == null) return;
        synchronized (members) {
            for (ClientHandler h : members) h.send(msg);
        }
    }

    private static void forwardToTarget(ChatMessage msg) {
        ClientHandler target = clients.get(msg.getTo());
        if (target != null) target.send(msg);
    }
}
