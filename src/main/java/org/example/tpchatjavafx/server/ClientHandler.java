package org.example.tpchatjavafx.server;

import org.example.tpchatjavafx.auth.AuthService;
import org.example.tpchatjavafx.client.model.ChatMessage;
import org.example.tpchatjavafx.common.MessageType;
import org.example.tpchatjavafx.database.MessageDAO;
import org.example.tpchatjavafx.database.UserDAO;
import org.example.tpchatjavafx.model.User;

import java.io.*;
import java.net.Socket;

/**
 * Gère la connexion d'UN client (thread dédié).
 * Responsabilités : auth, persistance messages, routage.
 */
public class ClientHandler implements Runnable {

    private final Socket     socket;
    private PrintWriter      out;
    private String           username;

    private final AuthService  authService = new AuthService();
    private final UserDAO      userDAO     = new UserDAO();
    private final MessageDAO   messageDAO  = new MessageDAO();

    public ClientHandler(Socket socket) { this.socket = socket; }

    public String getUsername() { return username; }

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

        User user = authService.login(uname, password);
        if (user == null) {
            send(new ChatMessage(MessageType.AUTH_FAIL, "SERVER", uname, null,
                    "Identifiants incorrects"));
            return;
        }
        this.username = user.getUsername();
        ChatServer.registerClient(username, this);
        send(new ChatMessage(MessageType.AUTH_SUCCESS, "SERVER", username, null, username));
        System.out.println("[Auth] Login OK : " + username);
    }

    private void handleRegister(ChatMessage msg) {
        String uname = msg.getFrom();
        // content = "password|email"
        String[] parts    = msg.getContent().split("\\|", 2);
        String   password = parts[0];
        String   email    = parts.length > 1 ? parts[1] : "";

        AuthService.RegisterResult res = authService.register(uname, password, email);
        if (!res.success()) {
            send(new ChatMessage(MessageType.AUTH_FAIL, "SERVER", uname, null, res.reason()));
            return;
        }
        this.username = uname;
        ChatServer.registerClient(username, this);
        send(new ChatMessage(MessageType.AUTH_SUCCESS, "SERVER", username, null, username));
        System.out.println("[Auth] Register OK : " + username);
    }

    // ── Persistance + routage ─────────────────────────────────

    private void persistAndRoute(ChatMessage msg) {
        // Persistance des messages texte privés
        if (msg.getType() == MessageType.PRIVATE
                && msg.getTo() != null && !msg.getTo().isBlank()) {
            try {
                User sender   = userDAO.findByUsername(msg.getFrom());
                User receiver = userDAO.findByUsername(msg.getTo());
                if (sender != null && receiver != null) {
                    messageDAO.saveMessage(sender.getId(), receiver.getId(), msg.getContent());
                }
            } catch (Exception e) {
                System.err.println("[Handler] Persistance échouée : " + e.getMessage());
            }
        }
        ChatServer.handleMessage(msg, this);
    }

    // ── Nettoyage ─────────────────────────────────────────────

    private void cleanup() {
        ChatServer.removeClient(username);
        username = null;
        try { if (!socket.isClosed()) socket.close(); }
        catch (IOException ignored) {}
    }
}
