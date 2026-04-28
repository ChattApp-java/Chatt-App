package org.example.tpchatjavafx.server;

import org.example.tpchatjavafx.client.model.ChatMessage;
import org.example.tpchatjavafx.common.MessageType;
import org.example.tpchatjavafx.dao.ConnexionDAO;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Serveur TCP du chat.
 * Gère les connexions et le routage multi-fenêtres pour un même utilisateur.
 */
public class ChatServer {

    static final int PORT = 5555;

    /** username -> Ensemble de sockets/handlers (pour multi-clients) */
    static final Map<String, Set<ClientHandler>> clients = new ConcurrentHashMap<>();
    
    private static final ConnexionDAO connexionDAO = new ConnexionDAO();

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

    static void registerClient(String username, int userId, ClientHandler handler) {
        clients.computeIfAbsent(username, k -> Collections.synchronizedSet(new HashSet<>())).add(handler);
        System.out.println("[Server] Connecté : " + username + " (Total sessions: " + clients.get(username).size() + ")");
        
        // Mettre à jour la BDD : En ligne
        try {
            connexionDAO.setEnLigne(userId, handler.getSocketId(), true);
        } catch (Exception e) {
            System.err.println("Erreur MàJ statut En ligne: " + e.getMessage());
        }
        
        broadcastUserStatus(username, "EN_LIGNE");
        broadcastUserList();
    }

    static void removeClient(String username, int userId, ClientHandler handler) {
        if (username == null) return;
        Set<ClientHandler> userHandlers = clients.get(username);
        if (userHandlers != null) {
            userHandlers.remove(handler);
            if (userHandlers.isEmpty()) {
                clients.remove(username);
            }
        }
        System.out.println("[Server] Déconnecté : " + username);
        
        // Mettre à jour la BDD : Déconnecté (pour CE socket précis)
        try {
            connexionDAO.setEnLigne(userId, handler.getSocketId(), false);
        } catch (Exception e) {
            System.err.println("Erreur MàJ statut Déconnecté: " + e.getMessage());
        }

        // S'il n'y a plus aucune session pour cet utilisateur, on diffuse son statut hors ligne
        if (!clients.containsKey(username)) {
            broadcastUserStatus(username, "NON_CONNECTE");
            broadcastUserList();
        }
    }

    /** Diffuse la liste globale des utilisateurs connectés (usernames) */
    static void broadcastUserList() {
        String userListContent = String.join(",", clients.keySet());
        ChatMessage msg = new ChatMessage(MessageType.USER_LIST, "SERVER", null, null, userListContent);
        broadcastToAll(msg);
    }
    
    /** Diffuse le statut d'un utilisateur spécifique */
    static void broadcastUserStatus(String username, String status) {
        ChatMessage msg = new ChatMessage(MessageType.STATUS_UPDATE, username, null, null, status);
        broadcastToAll(msg);
    }

    private static void broadcastToAll(ChatMessage msg) {
        clients.values().forEach(handlers -> handlers.forEach(h -> h.send(msg)));
    }

    // ── Routage des messages ───────────────────────────────────

    static void handleMessage(ChatMessage msg, ClientHandler from) {
        if (msg == null) return;
        switch (msg.getType()) {
            case PRIVATE, PRIVATE_AUDIO, PRIVATE_IMAGE, PRIVATE_FILE -> routePrivate(msg, from);
            case VIDEO_CALL_REQUEST, VIDEO_CALL_ACCEPT,
                 VIDEO_CALL_REJECT,  VIDEO_CALL_END, VIDEO_FRAME,
                 VOICE_CALL_REQUEST, VOICE_CALL_ACCEPT,
                 VOICE_CALL_REJECT,  VOICE_CALL_END, VOICE_FRAME    -> forwardToTarget(msg);
            case USER_LIST_REQUEST -> broadcastUserList();
            default -> {} // LOGIN / REGISTER traités dans ClientHandler
        }
    }

    // ── Helpers ───────────────────────────────────────────────

    private static void routePrivate(ChatMessage msg, ClientHandler from) {
        forwardToTarget(msg); // Envoie à toutes les fenêtres du destinataire
        
        // Envoie aussi aux autres fenêtres de l'expéditeur pour synchronisation (s'il en a plusieurs)
        if (from.getUsername() != null) {
            Set<ClientHandler> senderHandlers = clients.get(from.getUsername());
            if (senderHandlers != null) {
                for (ClientHandler h : senderHandlers) {
                    if (h != from) { // Ne pas renvoyer à l'onglet qui a émis
                        h.send(msg);
                    }
                }
            }
        }
    }

    private static void forwardToTarget(ChatMessage msg) {
        Set<ClientHandler> targets = clients.get(msg.getTo());
        if (targets != null && !targets.isEmpty()) {
            for (ClientHandler target : targets) {
                target.send(msg);
            }
        }
    }
}
