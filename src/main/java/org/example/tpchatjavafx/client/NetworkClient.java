package org.example.tpchatjavafx.client;

import javafx.application.Platform;
import org.example.tpchatjavafx.client.model.ChatMessage;
import org.example.tpchatjavafx.common.MessageType;

import java.io.*;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

/**
 * Client TCP — gère la connexion au serveur, l'auth et la réception de messages.
 */
public class NetworkClient {

    private final String serverHost;
    private final int    serverPort;

    private Socket      socket;
    private PrintWriter out;
    private String      username;

    // ── Callbacks ────────────────────────────────────────────
    private Consumer<ChatMessage>  onMessageReceived;
    private Consumer<String>       onAuthSuccess;   // reçoit le username
    private Consumer<String>       onAuthFail;      // reçoit la raison
    private Consumer<List<String>> onUserListReceived;
    private Consumer<ChatMessage>  onUserStatusChanged;
    private Runnable               onConnectionLost;

    public NetworkClient(String serverHost, int serverPort) {
        this.serverHost = serverHost;
        this.serverPort = serverPort;
    }

    // ── Setters callbacks ────────────────────────────────────
    public void setOnMessageReceived(Consumer<ChatMessage> cb)   { onMessageReceived   = cb; }
    public void setOnAuthSuccess(Consumer<String> cb)             { onAuthSuccess        = cb; }
    public void setOnAuthFail(Consumer<String> cb)                { onAuthFail           = cb; }
    public void setOnUserListReceived(Consumer<List<String>> cb)  { onUserListReceived   = cb; }
    public void setOnUserStatusChanged(Consumer<ChatMessage> cb)  { onUserStatusChanged  = cb; }
    public void setOnConnectionLost(Runnable cb)                  { onConnectionLost     = cb; }

    // ── Connexion ─────────────────────────────────────────────

    /** Ouvre la socket TCP et démarre le thread d'écoute. Ne se logue PAS encore. */
    public void connect() throws IOException {
        socket = new Socket(serverHost, serverPort);
        out    = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
        startListenerThread();
    }

    // ── Auth ─────────────────────────────────────────────────

    public void login(String username, String password) {
        this.username = username;
        send(new ChatMessage(MessageType.LOGIN, username, null, null, password));
    }

    /** content = "password|email" */
    public void register(String username, String password, String email) {
        this.username = username;
        String content = password + "|" + (email == null ? "" : email);
        send(new ChatMessage(MessageType.REGISTER, username, null, null, content));
    }

    public void requestUserList() {
        send(new ChatMessage(MessageType.USER_LIST_REQUEST, username, "SERVER", null, ""));
    }

    // ── Envoi ────────────────────────────────────────────────

    public void send(ChatMessage msg) {
        if (out != null) {
            out.println(msg.serialize());
            out.flush();
        }
    }

    // ── Getters ──────────────────────────────────────────────

    public String getUsername() { return username; }

    public void close() {
        try { if (socket != null) socket.close(); }
        catch (IOException ignored) {}
    }

    // ── Thread d'écoute ───────────────────────────────────────

    private void startListenerThread() {
        Thread t = new Thread(() -> {
            try (BufferedReader in = new BufferedReader(
                         new InputStreamReader(socket.getInputStream()))) {
                String line;
                while ((line = in.readLine()) != null) {
                    dispatch(ChatMessage.deserialize(line));
                }
            } catch (IOException ignored) {
            } finally {
                Platform.runLater(() -> {
                    if (onConnectionLost != null) onConnectionLost.run();
                });
            }
        });
        t.setDaemon(true);
        t.start();
    }

    /** Dispatch d'un message reçu vers le bon callback. */
    private void dispatch(ChatMessage msg) {
        if (msg == null) return;
        Platform.runLater(() -> {
            switch (msg.getType()) {
                case AUTH_SUCCESS -> {
                    username = msg.getContent();
                    if (onAuthSuccess != null) onAuthSuccess.accept(username);
                }
                case AUTH_FAIL -> {
                    if (onAuthFail != null) onAuthFail.accept(msg.getContent());
                }
                case USER_LIST -> {
                    if (onUserListReceived != null) {
                        String raw = msg.getContent();
                        List<String> users = (raw == null || raw.isBlank())
                                ? List.of()
                                : Arrays.asList(raw.split(","));
                        onUserListReceived.accept(users);
                    }
                }
                case STATUS_UPDATE -> {
                    if (onUserStatusChanged != null) onUserStatusChanged.accept(msg);
                }
                default -> {
                    if (onMessageReceived != null) onMessageReceived.accept(msg);
                }
            }
        });
    }


}
