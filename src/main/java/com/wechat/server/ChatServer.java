package com.wechat.server;

import com.wechat.common.Protocol;
import com.wechat.common.Protocol.NetworkMessage;
import com.wechat.common.Protocol.MessageType;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Serveur WeChat principal - Écoute sur le port 5000
 * Gère les connexions clients et la distribution des messages.
 */
public class ChatServer {

    private static final Logger LOGGER = Logger.getLogger(ChatServer.class.getName());
    private static final int PORT = Protocol.SERVER_PORT;
    private static final int MAX_THREADS = 100;

    private ServerSocket serverSocket;
    private ExecutorService executorService;
    private volatile boolean running = false;

    // Map des clients connectés : userId → ClientHandler
    private final Map<Long, ClientHandler> connectedClients = new ConcurrentHashMap<>();

    // Map des sockets anonymes (avant authentification)
    private final Map<Socket, ClientHandler> pendingClients = new ConcurrentHashMap<>();

    public ChatServer() {
        this.executorService = Executors.newFixedThreadPool(MAX_THREADS);
    }

    /**
     * Démarre le serveur
     */
    public void start() {
        try {
            serverSocket = new ServerSocket(PORT);
            running = true;
            LOGGER.info("═══════════════════════════════════════════════════");
            LOGGER.info("  🚀 SERVEUR WECHAT DÉMARRÉ");
            LOGGER.info("  📡 Port : " + PORT);
            LOGGER.info("  🧵 Threads max : " + MAX_THREADS);
            LOGGER.info("═══════════════════════════════════════════════════");

            // Thread de maintenance (ping/pong, nettoyage)
            startMaintenanceThread();

            // Boucle d'acceptation des connexions
            while (running && !serverSocket.isClosed()) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    LOGGER.info("📥 Nouvelle connexion : " + clientSocket.getInetAddress().getHostAddress()
                            + ":" + clientSocket.getPort());

                    ClientHandler handler = new ClientHandler(clientSocket, this);
                    pendingClients.put(clientSocket, handler);
                    executorService.execute(handler);

                } catch (IOException e) {
                    if (running) {
                        LOGGER.log(Level.SEVERE, "Erreur acceptation connexion", e);
                    }
                }
            }

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Impossible de démarrer le serveur sur le port " + PORT, e);
        }
    }

    /**
     * Arrête le serveur proprement
     */
    public void stop() {
        LOGGER.info("🛑 Arrêt du serveur...");
        running = false;

        // Déconnecter tous les clients
        connectedClients.values().forEach(ClientHandler::disconnect);
        pendingClients.values().forEach(ClientHandler::disconnect);
        connectedClients.clear();
        pendingClients.clear();

        // Fermer le pool de threads
        executorService.shutdown();

        // Fermer le socket serveur
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Erreur fermeture serveur", e);
        }

        LOGGER.info("✅ Serveur arrêté.");
    }

    /**
     * Enregistre un client authentifié
     */
    public synchronized void registerClient(Long userId, ClientHandler handler) {
        // Déconnecter l'ancienne session si existante
        ClientHandler old = connectedClients.get(userId);
        if (old != null) {
            old.disconnect();
            LOGGER.info("🔄 Ancienne session déconnectée pour user " + userId);
        }

        connectedClients.put(userId, handler);
        pendingClients.remove(handler.getSocket());

        LOGGER.info("✅ Client authentifié : userId=" + userId
                + " | Connectés : " + connectedClients.size());

        // Notifier les autres utilisateurs
        broadcastUserStatus(userId, "ONLINE");
    }

    /**
     * Déconnecte un client
     */
    public synchronized void unregisterClient(Long userId, ClientHandler handler) {
        if (userId != null) {
            connectedClients.remove(userId);
            LOGGER.info("👋 Client déconnecté : userId=" + userId
                    + " | Restants : " + connectedClients.size());
            broadcastUserStatus(userId, "OFFLINE");
        }
        pendingClients.remove(handler.getSocket());
    }

    /**
     * Envoie un message à un client spécifique
     */
    public void sendToUser(Long userId, NetworkMessage message) {
        ClientHandler handler = connectedClients.get(userId);
        if (handler != null && handler.isConnected()) {
            handler.sendMessage(message);
        } else {
            LOGGER.warning("❌ Impossible d'envoyer à user " + userId + " : hors ligne");
        }
    }

    /**
     * Diffuse un message à tous les clients connectés
     */
    public void broadcast(NetworkMessage message) {
        connectedClients.values().forEach(h -> {
            if (h.isConnected()) {
                h.sendMessage(message);
            }
        });
    }

    /**
     * Diffuse à tous sauf l'expéditeur
     */
    public void broadcastExcept(Long excludeUserId, NetworkMessage message) {
        connectedClients.forEach((uid, handler) -> {
            if (!uid.equals(excludeUserId) && handler.isConnected()) {
                handler.sendMessage(message);
            }
        });
    }

    /**
     * Diffuse un message de groupe
     */
    public void broadcastToGroup(java.util.List<Long> memberIds, NetworkMessage message) {
        memberIds.forEach(memberId -> sendToUser(memberId, message));
    }

    /**
     * Vérifie si un utilisateur est en ligne
     */
    public boolean isUserOnline(Long userId) {
        ClientHandler handler = connectedClients.get(userId);
        return handler != null && handler.isConnected();
    }

    /**
     * Retourne le nombre de clients connectés
     */
    public int getConnectedCount() {
        return connectedClients.size();
    }

    /**
     * Retourne la liste des IDs connectés
     */
    public java.util.List<Long> getOnlineUserIds() {
        return new java.util.ArrayList<>(connectedClients.keySet());
    }

    // ===== PRIVÉ =====

    private void broadcastUserStatus(Long userId, String status) {
        NetworkMessage statusMsg = NetworkMessage.userStatus(userId, status);
        broadcastExcept(userId, statusMsg);
    }

    private void startMaintenanceThread() {
        Thread maintenance = new Thread(() -> {
            while (running) {
                try {
                    Thread.sleep(30000); // Toutes les 30 secondes

                    // Nettoyer les connexions mortes
                    connectedClients.entrySet().removeIf(entry -> !entry.getValue().isConnected());
                    pendingClients.entrySet().removeIf(entry -> !entry.getValue().isConnected());

                    LOGGER.info("🔄 Maintenance : " + connectedClients.size()
                            + " clients connectés, " + pendingClients.size() + " en attente");

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        maintenance.setDaemon(true);
        maintenance.setName("Server-Maintenance");
        maintenance.start();
    }

    public boolean isRunning() {
        return running;
    }
}