package com.chatapp.server;

import com.chatapp.database.MessageDAO;
import com.chatapp.database.UserDAO;
import com.chatapp.model.Message;
import com.chatapp.model.User;
import com.chatapp.protocol.Protocol;
import com.chatapp.protocol.ProtocolParser;

import java.io.*;
import java.net.Socket;
import java.util.List;

/**
 * ClientHandler.java
 * Gère la communication avec un client connecté.
 * - Authentification via UserDAO (BCrypt)
 * - Persistence des messages via MessageDAO
 * - Parsing sécurisé via ProtocolParser
 * - Validation des entrées et gestion d'erreurs robuste
 */
public class ClientHandler implements Runnable {

    private static final int MAX_USERNAME_LENGTH = 32;
    private static final int MAX_CONTENT_LENGTH  = 2000;

    private final Socket socket;
    private final Server server;
    private final UserDAO userDAO;
    private final MessageDAO messageDAO;
    private String username;
    private boolean authenticated = false;

    private BufferedReader in;
    private PrintWriter   out;

    public ClientHandler(Socket socket, Server server) {
        this.socket = socket;
        this.server = server;
        this.userDAO = new UserDAO();
        this.messageDAO = new MessageDAO();
    }

    @Override
    public void run() {
        try {
            in  = new BufferedReader(
                      new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(
                      new OutputStreamWriter(socket.getOutputStream()), true);

            server.log("Buffer prêt pour : "
                    + socket.getInetAddress().getHostAddress());

            String line;
            while ((line = in.readLine()) != null) {
                try {
                    handleMessage(line.trim());
                } catch (Exception e) {
                    server.log("[ERREUR] Exception non gérée dans handleMessage : " + e.getMessage());
                    sendMessage(Protocol.MSG_STATUS + Protocol.SEP + "Erreur serveur interne");
                }
            }

        } catch (IOException e) {
            server.log("Connexion perdue : "
                    + (username != null ? username
                                        : socket.getInetAddress().getHostAddress()));
        } finally {
            disconnect();
        }
    }

    private void handleMessage(String raw) {
        if (raw == null || raw.isEmpty()) return;

        String[] parts = ProtocolParser.parse(raw);
        if (parts.length == 0) {
            server.log("[WARN] Message malformé reçu — rejeté.");
            return;
        }

        String type = parts[0];
        server.log("REÇU de [" + (username != null ? username : "?") + "] : " + raw);

        switch (type) {

            // ── LOGIN|username|password ─────────────────────────
            case Protocol.LOGIN: {
                if (!ProtocolParser.hasMinParts(parts, 3)) {
                    sendMessage(Protocol.LOGIN_FAIL + Protocol.SEP + "Format invalide");
                    break;
                }
                String name = parts[1].trim();
                String pass = parts[2];

                if (!isValidUsername(name)) {
                    sendMessage(Protocol.LOGIN_FAIL + Protocol.SEP + "Nom d'utilisateur invalide");
                    break;
                }

                // Vérifier auth via base de données
                User user = userDAO.authentifier(name, pass);
                if (user == null) {
                    sendMessage(Protocol.LOGIN_FAIL + Protocol.SEP + "Identifiants incorrects");
                    server.log("LOGIN refusé (auth échouée) pour : " + name);
                    break;
                }

                // Vérifier si déjà connecté
                if (server.isUsernameTaken(name)) {
                    sendMessage(Protocol.LOGIN_FAIL + Protocol.SEP + "Déjà connecté ailleurs");
                    server.log("LOGIN refusé (déjà connecté) pour : " + name);
                    break;
                }

                this.username = name;
                this.authenticated = true;
                server.registerClient(this);

                sendMessage(Protocol.LOGIN_OK + Protocol.SEP + username);
                sendMessage(Protocol.USER_LIST + Protocol.SEP + server.getConnectedUsernames());
                server.broadcast(Protocol.USER_JOINED + Protocol.SEP + username, this);

                server.log("LOGIN OK : " + username
                        + " | Connectés : " + server.getClientCount());
                break;
            }

            // ── REGISTER|username|password|email ────────────────
            case Protocol.REGISTER: {
                if (!ProtocolParser.hasMinParts(parts, 4)) {
                    sendMessage(Protocol.REGISTER_FAIL + Protocol.SEP + "Format invalide");
                    break;
                }
                String regName  = parts[1].trim();
                String regPass  = parts[2];
                String regEmail = parts[3].trim();

                if (!isValidUsername(regName)) {
                    sendMessage(Protocol.REGISTER_FAIL + Protocol.SEP + "Nom d'utilisateur invalide");
                    break;
                }
                if (regPass.length() < 4) {
                    sendMessage(Protocol.REGISTER_FAIL + Protocol.SEP + "Mot de passe trop court (min 4 caractères)");
                    break;
                }

                User newUser = new User(regName, regPass, regEmail);
                try {
                    boolean ok = userDAO.inscrire(newUser);
                    if (ok) {
                        sendMessage(Protocol.REGISTER_OK + Protocol.SEP + regName);
                        server.log("REGISTER OK : " + regName);
                    } else {
                        sendMessage(Protocol.REGISTER_FAIL + Protocol.SEP + "Nom d'utilisateur déjà pris");
                        server.log("REGISTER FAIL (existe) : " + regName);
                    }
                } catch (java.sql.SQLException e) {
                    server.log("[DB ERROR] Inscription : " + e.getMessage());
                    sendMessage(Protocol.REGISTER_FAIL + Protocol.SEP + "Erreur base de données : " + e.getMessage());
                }
                break;
            }

            // ── Les commandes suivantes nécessitent authentification ──
            case Protocol.LOGOUT:
                if (!checkAuth()) break;
                server.log("LOGOUT demandé par : " + username);
                disconnect();
                break;

            case Protocol.MSG:
                if (!checkAuth()) break;
                if (!ProtocolParser.hasMinParts(parts, 3)) break;
                routeMessage(parts[1], Protocol.MSG_RECV,
                        username, parts[2]);
                break;

            case Protocol.MSG_ACK:
                if (!checkAuth()) break;
                if (!ProtocolParser.hasMinParts(parts, 2)) break;
                routeSimple(parts[1], Protocol.MSG_ACK, username);
                break;

            case Protocol.MSG_READ:
                if (!checkAuth()) break;
                if (!ProtocolParser.hasMinParts(parts, 3)) break;
                handleMsgRead(parts[1], parts[2]);
                break;

            case Protocol.TYPING_START:
                if (!checkAuth()) break;
                if (!ProtocolParser.hasMinParts(parts, 2)) break;
                routeSimple(parts[1], Protocol.TYPING_START, username);
                break;

            case Protocol.TYPING_STOP:
                if (!checkAuth()) break;
                if (!ProtocolParser.hasMinParts(parts, 2)) break;
                routeSimple(parts[1], Protocol.TYPING_STOP, username);
                break;

            case Protocol.CALL_REQUEST:
                if (!checkAuth()) break;
                if (!ProtocolParser.hasMinParts(parts, 4)) break;
                routeCallRequest(parts[1], parts[2], parts[3]);
                break;

            case Protocol.CALL_ACCEPT:
                if (!checkAuth()) break;
                if (!ProtocolParser.hasMinParts(parts, 3)) break;
                routeCallAccept(parts[1], parts[2]);
                break;

            case Protocol.CALL_REJECT:
                if (!checkAuth()) break;
                if (!ProtocolParser.hasMinParts(parts, 3)) break;
                routeCallReject(parts[1], parts[2]);
                break;

            case Protocol.CALL_END:
                if (!checkAuth()) break;
                if (!ProtocolParser.hasMinParts(parts, 3)) break;
                routeCallEnd(parts[2]);
                break;

            case Protocol.FILE:
                if (!checkAuth()) break;
                if (!ProtocolParser.hasMinParts(parts, 4)) break;
                routeFile(parts[1], parts[2], parts[3]);
                break;

            case Protocol.AUDIO_MSG:
                if (!checkAuth()) break;
                if (!ProtocolParser.hasMinParts(parts, 3)) break;
                routeAudio(parts[1], parts[2]);
                break;

            default:
                server.log("Message inconnu reçu de [" + username + "] : " + type);
        }
    }

    // ══════════════════════════════════════════════════════════
    //  ROUTING HELPERS  (avec persistence DB)
    // ══════════════════════════════════════════════════════════

    private void routeMessage(String dest, String type, String sender, String content) {
        if (content.length() > MAX_CONTENT_LENGTH) {
            sendMessage(Protocol.MSG_STATUS + Protocol.SEP + "Message trop long (max " + MAX_CONTENT_LENGTH + ")");
            return;
        }

        // ── 1. Récupérer les IDs utilisateurs pour persistance ──
        User senderUser = userDAO.getUserByUsername(sender);
        User destUser   = userDAO.getUserByUsername(dest);

        if (senderUser != null && destUser != null) {
            Message msg = new Message(
                    senderUser.getId_user(),
                    destUser.getId_user(),
                    content
            );
            int msgId = messageDAO.sauvegarderMessage(msg);
            if (msgId > 0) {
                server.log("[DB] Message #" + msgId + " persisté : " + sender + " → " + dest);
            }
        }

        // ── 2. Router le message en temps réel ──
        ClientHandler target = server.getClient(dest);
        if (target != null) {
            target.sendMessage(ProtocolParser.build(type, sender, content));
            server.log("MSG routé : " + sender + " → " + dest);
        } else {
            server.log("MSG stocké (dest hors ligne) : " + sender + " → " + dest);
        }
    }

    /**
     * Gère MSG_READ : met à jour le statut en base + notifie l'expéditeur.
     * Format client : MSG_READ|destinataire|monPseudo
     * → On marque comme lus les messages de destinataire → monPseudo
     */
    private void handleMsgRead(String destinataire, String reader) {
        // Sécurité : reader doit correspondre à l'utilisateur authentifié
        if (!reader.equals(username)) {
            server.log("[WARN] MSG_READ usurpé : " + reader + " != " + username);
            return;
        }

        User readerUser = userDAO.getUserByUsername(reader);
        User senderUser = userDAO.getUserByUsername(destinataire);

        if (readerUser == null || senderUser == null) {
            server.log("[WARN] MSG_READ : utilisateur introuvable");
            return;
        }

        // Marquer tous les messages de senderUser vers readerUser comme lus
        messageDAO.marquerTousLus(senderUser.getId_user(), readerUser.getId_user());
        server.log("[DB] Messages marqués comme lus : " + destinataire + " → " + reader);

        // Notifier l'expéditeur original que ses messages ont été lus
        ClientHandler senderHandler = server.getClient(destinataire);
        if (senderHandler != null) {
            senderHandler.sendMessage(Protocol.MSG_READ + Protocol.SEP + reader);
        }
    }

    private void routeSimple(String dest, String type, String sender) {
        ClientHandler target = server.getClient(dest);
        if (target != null) target.sendMessage(type + Protocol.SEP + sender);
    }

    private void routeCallRequest(String caller, String callee, String callType) {
        if (!caller.equals(username)) {
            server.log("[WARN] CALL_REQUEST usurpé : " + caller + " != " + username);
            return;
        }
        ClientHandler target = server.getClient(callee);
        if (target != null) {
            target.sendMessage(Protocol.CALL_INCOMING + Protocol.SEP + caller + Protocol.SEP + callType);
            server.log("CALL_REQUEST : " + caller + " → " + callee + " [" + callType + "]");
        } else {
            sendMessage(Protocol.CALL_REJECTED + Protocol.SEP + callee);
            server.log("CALL_REQUEST échoué : " + callee + " hors ligne");
        }
    }

    private void routeCallAccept(String caller, String acceptor) {
        if (!acceptor.equals(username)) return;
        ClientHandler callerHandler = server.getClient(caller);
        if (callerHandler != null) {
            callerHandler.sendMessage(ProtocolParser.build(Protocol.CALL_ACCEPTED,
                    acceptor, String.valueOf(Protocol.PORT_AUDIO), String.valueOf(Protocol.PORT_VIDEO)));
            server.log("CALL accepté : " + acceptor + " accepte l'appel de " + caller);
        }
    }

    private void routeCallReject(String caller, String refuser) {
        if (!refuser.equals(username)) return;
        ClientHandler callerHandler = server.getClient(caller);
        if (callerHandler != null) {
            callerHandler.sendMessage(Protocol.CALL_REJECTED + Protocol.SEP + refuser);
            server.log("CALL refusé : " + refuser + " refuse l'appel de " + caller);
        }
    }

    private void routeCallEnd(String other) {
        ClientHandler otherHandler = server.getClient(other);
        if (otherHandler != null) {
            otherHandler.sendMessage(Protocol.CALL_ENDED + Protocol.SEP + username);
            server.log("CALL terminé entre " + username + " et " + other);
        }
    }

    private void routeFile(String dest, String fileName, String b64) {
        if (b64.length() > Protocol.MAX_FILE_SIZE * 1.4) { // Base64 ~33% overhead
            sendMessage(Protocol.MSG_STATUS + Protocol.SEP + "Fichier trop volumineux (max " + (Protocol.MAX_FILE_SIZE / 1024 / 1024) + " MB)");
            return;
        }
        ClientHandler target = server.getClient(dest);
        if (target != null) {
            target.sendMessage(ProtocolParser.build(Protocol.FILE_RECV, username, fileName, b64));
            server.log("FILE routé : " + username + " → " + dest + " [" + fileName + "]");
        } else {
            server.log("FILE non livré : destinataire [" + dest + "] introuvable");
        }
    }

    private void routeAudio(String dest, String b64) {
        ClientHandler target = server.getClient(dest);
        if (target != null) {
            target.sendMessage(ProtocolParser.build(Protocol.AUDIO_RECV, username, b64));
            server.log("AUDIO_MSG routé : " + username + " → " + dest);
        } else {
            server.log("AUDIO_MSG non livré : destinataire [" + dest + "] introuvable");
        }
    }

    // ══════════════════════════════════════════════════════════
    //  VALIDATION
    // ══════════════════════════════════════════════════════════

    private boolean checkAuth() {
        if (!authenticated) {
            sendMessage(Protocol.LOGIN_FAIL + Protocol.SEP + "Authentification requise");
            return false;
        }
        return true;
    }

    private boolean isValidUsername(String name) {
        return name != null
                && !name.isEmpty()
                && name.length() <= MAX_USERNAME_LENGTH
                && name.matches("^[a-zA-Z0-9_]+$");
    }

    // ══════════════════════════════════════════════════════════
    //  ENVOI / DÉCONNEXION
    // ══════════════════════════════════════════════════════════

    public synchronized void sendMessage(String message) {
        if (out != null) {
            out.println(message);
            server.log("ENVOYÉ à [" + (username != null ? username : "?")
                    + "] : " + message);
        }
    }

    private void disconnect() {
        if (username != null) {
            userDAO.deconnecter(username);
            server.removeClient(this);
            server.broadcast(Protocol.USER_LEFT + Protocol.SEP + username, this);
            server.log("Déconnexion de : " + username);
            username = null;
        }
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            server.log("[WARN] Erreur fermeture socket : " + e.getMessage());
        }
    }

    public String getUsername() {
        return username;
    }
}
