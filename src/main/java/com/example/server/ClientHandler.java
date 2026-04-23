package com.example.server;

import com.example.protocol.Protocol;

import java.io.*;
import java.net.Socket;


public class ClientHandler implements Runnable {

    private final Socket socket;
    private final Server server;
    private String username;

    // ── Buffer d'entrée/sortie ────────────────────────────────
    private BufferedReader in;   // lit les messages du client
    private PrintWriter   out;  // écrit les messages au client


    public ClientHandler(Socket socket, Server server) {
        this.socket = socket;
        this.server = server;
    }

 
    @Override
    public void run() {
        try {
            // Préparer le buffer d'entrée pour lire les messages
            in  = new BufferedReader(
                      new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(
                      new OutputStreamWriter(socket.getOutputStream()), true);

            server.log("Buffer prêt pour : "
                    + socket.getInetAddress().getHostAddress());

            // Lire les messages en continu
            String line;
            while ((line = in.readLine()) != null) {
                handleMessage(line.trim());
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

        // Découper le message : TYPE|param1|param2|...
        String[] parts = raw.split("\\" + Protocol.SEP, -1);
        String type = parts[0];

        server.log("REÇU de [" + (username != null ? username : "?") + "] : " + raw);

        switch (type) {

            // ──────────────────────────────────────────────────
            // LOGIN|username
            // ──────────────────────────────────────────────────
            case Protocol.LOGIN: {
                if (parts.length < 2) break;
                String name = parts[1].trim();

                if (name.isEmpty() || server.isUsernameTaken(name)) {
                    // Username déjà pris → refus
                    sendMessage(Protocol.LOGIN_FAIL + Protocol.SEP
                            + "Nom d'utilisateur indisponible");
                    server.log("LOGIN refusé pour : " + name);
                    return;
                }

                // Enregistrer le client
                this.username = name;
                server.registerClient(this);

                // 1. Confirmer la connexion
                sendMessage(Protocol.LOGIN_OK + Protocol.SEP + username);

                // 2. Envoyer la liste des utilisateurs connectés
                sendMessage(Protocol.USER_LIST + Protocol.SEP
                        + server.getConnectedUsernames());

                // 3. Notifier tous les autres
                server.broadcast(Protocol.USER_JOINED + Protocol.SEP
                        + username, this);

                server.log("LOGIN OK : " + username
                        + " | Connectés : " + server.getClientCount());
                break;
            }

            // ──────────────────────────────────────────────────
            // LOGOUT|username
            // ──────────────────────────────────────────────────
            case Protocol.LOGOUT: {
                server.log("LOGOUT demandé par : " + username);
                disconnect();
                break;
            }

            // ──────────────────────────────────────────────────
            // MSG|destinataire|contenu
            // ──────────────────────────────────────────────────
            case Protocol.MSG: {
                if (parts.length < 3) break;
                String dest    = parts[1];
                String content = parts[2];

                // Trouver le destinataire et lui envoyer le message
                ClientHandler target = server.getClient(dest);
                if (target != null) {
                    target.sendMessage(Protocol.MSG_RECV + Protocol.SEP
                            + username + Protocol.SEP + content);
                    server.log("MSG routé : " + username + " → " + dest);
                } else {
                    server.log("MSG non livré : destinataire [" + dest + "] introuvable");
                }
                break;
            }

            // ──────────────────────────────────────────────────
            // CALL_REQUEST|appelant|destinataire|type(AUDIO/VIDEO)
            // ──────────────────────────────────────────────────
            case Protocol.CALL_REQUEST: {
                if (parts.length < 4) break;
                String caller   = parts[1];
                String callee   = parts[2];
                String callType = parts[3];

                ClientHandler target = server.getClient(callee);
                if (target != null) {
                    // Informer le destinataire de l'appel entrant
                    target.sendMessage(Protocol.CALL_INCOMING + Protocol.SEP
                            + caller + Protocol.SEP + callType);
                    server.log("CALL_REQUEST : " + caller
                            + " → " + callee + " [" + callType + "]");
                } else {
                    // Destinataire hors ligne → refus automatique
                    sendMessage(Protocol.CALL_REJECTED + Protocol.SEP + callee);
                    server.log("CALL_REQUEST échoué : " + callee + " hors ligne");
                }
                break;
            }

            // ──────────────────────────────────────────────────
            // CALL_ACCEPT|appelant|accepteur
            // ──────────────────────────────────────────────────
            case Protocol.CALL_ACCEPT: {
                if (parts.length < 3) break;
                String caller   = parts[1];
                String acceptor = parts[2];

                ClientHandler callerHandler = server.getClient(caller);
                if (callerHandler != null) {
                    // Informer l'appelant + donner les ports UDP
                    callerHandler.sendMessage(
                            Protocol.CALL_ACCEPTED + Protocol.SEP
                            + acceptor        + Protocol.SEP
                            + Protocol.PORT_AUDIO + Protocol.SEP
                            + Protocol.PORT_VIDEO);
                    server.log("CALL accepté : " + acceptor
                            + " accepte l'appel de " + caller);
                }
                break;
            }

            // ──────────────────────────────────────────────────
            // CALL_REJECT|appelant|refuseur
            // ──────────────────────────────────────────────────
            case Protocol.CALL_REJECT: {
                if (parts.length < 3) break;
                String caller  = parts[1];
                String refuser = parts[2];

                ClientHandler callerHandler = server.getClient(caller);
                if (callerHandler != null) {
                    callerHandler.sendMessage(Protocol.CALL_REJECTED
                            + Protocol.SEP + refuser);
                    server.log("CALL refusé : " + refuser
                            + " refuse l'appel de " + caller);
                }
                break;
            }

            // ──────────────────────────────────────────────────
            // CALL_END|username|autre
            // ──────────────────────────────────────────────────
            case Protocol.CALL_END: {
                if (parts.length < 3) break;
                String other = parts[2];

                ClientHandler otherHandler = server.getClient(other);
                if (otherHandler != null) {
                    otherHandler.sendMessage(Protocol.CALL_ENDED
                            + Protocol.SEP + username);
                    server.log("CALL terminé entre " + username + " et " + other);
                }
                break;
            }

            // ──────────────────────────────────────────────────
            // Message inconnu
            // ──────────────────────────────────────────────────
            default:
                server.log("Message inconnu reçu de ["
                        + username + "] : " + type);
        }
    }

    // ══════════════════════════════════════════════════════════
    //  ENVOI D'UN MESSAGE AU CLIENT
    // ══════════════════════════════════════════════════════════
    public synchronized void sendMessage(String message) {
        if (out != null) {
            out.println(message);
            server.log("ENVOYÉ à [" + (username != null ? username : "?")
                    + "] : " + message);
        }
    }

    // ══════════════════════════════════════════════════════════
    //  DÉCONNEXION PROPRE
    // ══════════════════════════════════════════════════════════
    private void disconnect() {
        if (username != null) {
            server.removeClient(this);
            server.broadcast(Protocol.USER_LEFT + Protocol.SEP
                    + username, this);
            server.log("Déconnexion de : " + username);
            username = null;
        }
        try {
            if (!socket.isClosed()) socket.close();
        } catch (IOException ignored) {}
    }

    // ══════════════════════════════════════════════════════════
    //  GETTER
    // ══════════════════════════════════════════════════════════
    public String getUsername() {
        return username;
    }
    
    
}


