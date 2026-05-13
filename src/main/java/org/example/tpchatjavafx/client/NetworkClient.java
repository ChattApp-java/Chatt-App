package org.example.tpchatjavafx.client;

import javafx.application.Platform;
import org.example.tpchatjavafx.client.audio.AudioTransmissionService;
import org.example.tpchatjavafx.client.controller.MeetingController;
import org.example.tpchatjavafx.client.model.ChatMessage;
import org.example.tpchatjavafx.client.video.MeetingVideoCapture;
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
    private int         userId;

    // ── Callbacks ────────────────────────────────────────────
    private Consumer<ChatMessage>  onMessageReceived;
    private Consumer<ChatMessage>  onAuthSuccess;   // reçoit le message complet
    private Consumer<String>       onAuthFail;      // reçoit la raison
    private Consumer<List<String>> onUserListReceived;
    private Consumer<List<String>> onContactListReceived;
    private Consumer<ChatMessage>  onHistoryReceived;
    private Consumer<ChatMessage>  onUserStatusChanged;
    private Runnable               onConnectionLost;
    private Consumer<String>       onError;
    private AudioTransmissionService meetingAudioService;
    private MeetingVideoCapture      meetingVideoCapture;
    private MeetingController        activeMeetingController;

    public void setActiveMeetingController(MeetingController controller) {
        this.activeMeetingController = controller;
    }

    public NetworkClient(String serverHost, int serverPort) {
        this.serverHost = serverHost;
        this.serverPort = serverPort;
    }

    // ── Setters callbacks ────────────────────────────────────
    public void setOnMessageReceived(Consumer<ChatMessage> cb)   { onMessageReceived   = cb; }
    public void setOnAuthSuccess(Consumer<ChatMessage> cb)        { onAuthSuccess        = cb; }
    public void setOnAuthFail(Consumer<String> cb)                { onAuthFail           = cb; }
    public void setOnUserListReceived(Consumer<List<String>> cb)  { onUserListReceived   = cb; }
    public void setOnContactListReceived(Consumer<List<String>> cb) { onContactListReceived = cb; }
    public void setOnUserStatusChanged(Consumer<ChatMessage> cb)  { onUserStatusChanged  = cb; }
    public void setOnHistoryReceived(Consumer<ChatMessage> cb)    { onHistoryReceived    = cb; }
    public void setOnConnectionLost(Runnable cb)                  { onConnectionLost     = cb; }
    public void setOnError(Consumer<String> cb)                   { onError              = cb; }

    private Consumer<ChatMessage> onIncomingCall;
    private Consumer<ChatMessage> onCallAnswered;
    private Consumer<ChatMessage> onCallRejected;

    private Consumer<ChatMessage> onMeetingInvite;
    private Consumer<ChatMessage> onMeetingStarted;
    private Consumer<ChatMessage> onMeetingEnded;
    private Consumer<ChatMessage> onMeetingParticipantJoined;
    private Consumer<ChatMessage> onMeetingParticipantLeft;
    private Consumer<ChatMessage> onMeetingInfo;
    // ── Callbacks Groupes ─────────────────────────────────────
    private Consumer<ChatMessage> onGroupCreated;
    private Consumer<ChatMessage> onGroupListResponse;
    private Consumer<ChatMessage> onGroupMessage;
    private Consumer<ChatMessage> onGroupMemberAdded;
    private Consumer<ChatMessage> onGroupMemberRemoved;
    private Consumer<ChatMessage> onGroupMembersResponse;
    private Consumer<ChatMessage> onGroupHistoryResponse;

    public void setOnIncomingCall(Consumer<ChatMessage> cb) {
        this.onIncomingCall = cb;
    }

    public void setOnCallAnswered(Consumer<ChatMessage> cb) {
        this.onCallAnswered = cb;
    }

    public void setOnCallRejected(Consumer<ChatMessage> cb) {
        this.onCallRejected = cb;
    }

    public void setOnMeetingInvite(Consumer<ChatMessage> cb) {
        this.onMeetingInvite = cb;
    }

    public void setOnMeetingStarted(Consumer<ChatMessage> cb) {
        this.onMeetingStarted = cb;
    }

    public void setOnMeetingEnded(Consumer<ChatMessage> cb) {
        this.onMeetingEnded = cb;
    }

    public void setOnMeetingParticipantJoined(Consumer<ChatMessage> cb) {
        this.onMeetingParticipantJoined = cb;
    }

    public void setOnMeetingParticipantLeft(Consumer<ChatMessage> cb) {
        this.onMeetingParticipantLeft = cb;
    }

    public void setOnMeetingInfo(Consumer<ChatMessage> cb) {
        this.onMeetingInfo = cb;
    }
    public void setOnGroupCreated(Consumer<ChatMessage> cb)         { this.onGroupCreated = cb; }
    public void setOnGroupListResponse(Consumer<ChatMessage> cb)    { this.onGroupListResponse = cb; }
    public void setOnGroupMessage(Consumer<ChatMessage> cb)         { this.onGroupMessage = cb; }
    public void setOnGroupMemberAdded(Consumer<ChatMessage> cb)     { this.onGroupMemberAdded = cb; }
    public void setOnGroupMemberRemoved(Consumer<ChatMessage> cb)   { this.onGroupMemberRemoved = cb; }
    public void setOnGroupMembersResponse(Consumer<ChatMessage> cb) { this.onGroupMembersResponse = cb; }
    public void setOnGroupHistoryResponse(Consumer<ChatMessage> cb) { this.onGroupHistoryResponse = cb; }
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

    public void addContact(String contactUsername) {
        send(new ChatMessage(MessageType.CONTACT_ADD, username, "SERVER", null, contactUsername));
    }

    public void requestContacts() {
        send(new ChatMessage(MessageType.CONTACT_LOAD, username, "SERVER", null, ""));
    }

    public void requestHistory(String otherUser) {
        send(new ChatMessage(MessageType.HISTORY_REQUEST, username, "SERVER", null, otherUser));
    }

    public void startMeeting(int groupId, String meetingType) {
        ChatMessage msg = new ChatMessage(MessageType.MEETING_INVITE, username, "SERVER", null, "Démarrage réunion");
        msg.setGroupId(groupId);
        msg.setMeetingType(meetingType);
        send(msg);
    }

    public void joinMeeting(int meetingId) {
        ChatMessage msg = new ChatMessage(MessageType.MEETING_PARTICIPANT_JOINED, username, "SERVER", null, "Rejoint réunion");
        msg.setMeetingId(meetingId);
        send(msg);
    }

    public void leaveMeeting(int meetingId) {
        ChatMessage msg = new ChatMessage(MessageType.MEETING_PARTICIPANT_LEFT, username, "SERVER", null, "Quitte réunion");
        msg.setMeetingId(meetingId);
        send(msg);
    }

    public void endMeeting(int meetingId) {
        ChatMessage msg = new ChatMessage(MessageType.MEETING_ENDED, username, "SERVER", null, "Fin de réunion");
        msg.setMeetingId(meetingId);
        send(msg);
    }

    public void startMeetingAudio(int localPort, int serverPort, String serverHost) throws Exception {
        startMeetingAudio(0, localPort, serverPort, serverHost);
    }

    public void startMeetingAudio(int meetingId, int localPort, int serverPort, String serverHost) throws Exception {
        if (meetingAudioService != null) {
            meetingAudioService.stop();
        }
        meetingAudioService = new AudioTransmissionService();
        meetingAudioService.initiate(serverHost, serverPort, localPort, meetingId, userId);
    }

    public void startMeetingVideo(int localPort, int serverPort, String serverHost) throws Exception {
        startMeetingVideo(0, localPort, serverPort, serverHost);
    }

    public void startMeetingVideo(int meetingId, int localPort, int serverPort, String serverHost) throws Exception {
        if (meetingVideoCapture != null) {
            meetingVideoCapture.stop();
        }
        meetingVideoCapture = new MeetingVideoCapture();
        meetingVideoCapture.setOnRemoteFrame((senderId, frame) -> {
            if (activeMeetingController != null) {
                activeMeetingController.updateParticipantFrame(String.valueOf(senderId), frame);
            }
        });
        meetingVideoCapture.start(serverHost, serverPort, localPort, meetingId, userId);
    }

    public void sendMeetingAudioFrame(byte[] frame) {
        if (meetingAudioService != null) {
            meetingAudioService.sendAudioFrame(frame);
        }
    }

    public void sendMeetingVideoFrame(byte[] frame) {
        if (meetingVideoCapture != null) {
            meetingVideoCapture.sendFrame(frame);
        }
    }

    // ── Envoi ────────────────────────────────────────────────
    public void createGroup(String nom, String description) {
        createGroup(nom, description, java.util.List.of());
    }

    /**
     * Crée un groupe avec une liste initiale de membres (IDs séparés par virgule).
     * Format serveur : "nom;description;id1,id2,id3"
     */
    public void createGroup(String name, String description, java.util.List<String> members) {
        String membersCsv = (members == null) ? "" : String.join(",", members);
        String content = name + ";" + (description == null ? "" : description) + ";" + membersCsv;
        send(new ChatMessage(MessageType.GROUP_CREATE, username, "SERVER", null, content));
    }

    public void sendGroupMessage(int groupId, String content) {
        ChatMessage msg = new ChatMessage(MessageType.GROUP_MESSAGE, username, "SERVER", null, content);
        msg.setGroupId(groupId);
        send(msg);
    }

    public void sendGroupMedia(int groupId, MessageType type, String fileName, byte[] data) {
        ChatMessage msg = new ChatMessage(type, username, "SERVER", null, fileName);
        msg.setGroupId(groupId);
        msg.setBinaryData(data);
        send(msg);
    }

    public void addGroupMember(int groupId, String pseudo) {
        ChatMessage msg = new ChatMessage(MessageType.GROUP_ADD_MEMBER, username, "SERVER", null, pseudo);
        msg.setGroupId(groupId);
        send(msg);
    }

    public void leaveGroup(int groupId) {
        ChatMessage msg = new ChatMessage(MessageType.GROUP_LEAVE, username, "SERVER", null, "");
        msg.setGroupId(groupId);
        send(msg);
    }

    public void requestGroupList() {
        send(new ChatMessage(MessageType.GROUP_LIST, username, "SERVER", null, ""));
    }

    public void requestGroupHistory(int groupId) {
        ChatMessage msg = new ChatMessage(MessageType.GROUP_HISTORY_REQUEST, username, "SERVER", null, "");
        msg.setGroupId(groupId);
        send(msg);
    }

    public void requestGroupMembers(int groupId) {
        ChatMessage msg = new ChatMessage(MessageType.GROUP_MEMBERS, username, "SERVER", null, "");
        msg.setGroupId(groupId);
        send(msg);
    }

    public void startGroupMeeting(int groupId, String meetingType) {
        ChatMessage msg = new ChatMessage(MessageType.MEETING_START, username, "SERVER", null, meetingType);
        msg.setGroupId(groupId);
        send(msg);
    }
    public void send(ChatMessage msg) {
        if (out != null) {
            out.println(msg.serialize());
            out.flush();
        }
    }

    // ── Getters ──────────────────────────────────────────────

    private int parseIntSafe(String value, int defaultValue) {
        try {
            return value == null || value.isBlank() ? defaultValue : Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public String getUsername() { return username; }
    public int getUserId() { return userId; }

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
                    userId = parseIntSafe(msg.getTo(), 0);
                    if (onAuthSuccess != null) onAuthSuccess.accept(msg);
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
                case CONTACT_LIST -> {
                    if (onContactListReceived != null) {
                        String raw = msg.getContent();
                        List<String> users = (raw == null || raw.isBlank())
                                ? List.of()
                                : Arrays.asList(raw.split(","));
                        onContactListReceived.accept(users);
                    }
                }
                case ERROR -> {
                    if (onError != null) onError.accept(msg.getContent());
                }
                case SYNC_HISTORY -> {
                    if (onHistoryReceived != null) onHistoryReceived.accept(msg);
                }
                case CALL_INCOMING -> {
                    if (onIncomingCall != null) {
                        onIncomingCall.accept(msg);
                    }
                }
                case CALL_ANSWER -> {
                    if (onCallAnswered != null) {
                        onCallAnswered.accept(msg);
                    }
                }
                case CALL_REJECT -> {
                    if (onCallRejected != null) {
                        onCallRejected.accept(msg);
                    }
                }
                case MEETING_INVITE -> {
                    if (onMeetingInvite != null) {
                        onMeetingInvite.accept(msg);
                    }
                }
                case MEETING_STARTED -> {
                    if (onMeetingStarted != null) {
                        onMeetingStarted.accept(msg);
                    }
                }
                case MEETING_ENDED -> {
                    if (onMeetingEnded != null) {
                        onMeetingEnded.accept(msg);
                    }
                }
                case MEETING_PARTICIPANT_JOINED -> {
                    if (onMeetingParticipantJoined != null) {
                        onMeetingParticipantJoined.accept(msg);
                    }
                    if (activeMeetingController != null) {
                        activeMeetingController.syncParticipants(msg.getContent());
                    }
                }
                case MEETING_PARTICIPANT_LEFT -> {
                    if (onMeetingParticipantLeft != null) {
                        onMeetingParticipantLeft.accept(msg);
                    }
                    if (activeMeetingController != null) {
                        activeMeetingController.removeParticipant(msg.getFrom(), msg.getFrom());
                    }
                }
                case MEETING_PARTICIPANTS -> {
                    if (activeMeetingController != null) {
                        activeMeetingController.syncParticipants(msg.getContent());
                    }
                }
                case MEETING_INFO -> {
                    if (onMeetingInfo != null) {
                        onMeetingInfo.accept(msg);
                    }
                }
                case MEETING_AUDIO_FRAME -> {
                    if (activeMeetingController != null) {
                        activeMeetingController.addAudioFrame(msg.getFrom(), msg.getBinaryData());
                    }
                }
                case MEETING_VIDEO_FRAME -> {
                    if (activeMeetingController != null) {
                        activeMeetingController.updateParticipantFrame(msg.getFrom(), msg.getBinaryData());
                    }
                }
                case GROUP_CREATED -> {
                    if (onGroupCreated != null) onGroupCreated.accept(msg);
                }
                case GROUP_LIST_RESPONSE -> {
                    if (onGroupListResponse != null) onGroupListResponse.accept(msg);
                }
                case GROUP_MESSAGE, GROUP_AUDIO, GROUP_IMAGE, GROUP_FILE -> {
                    if (onGroupMessage != null) onGroupMessage.accept(msg);
                }
                case GROUP_MEMBERS -> {
                    if (onGroupMembersResponse != null) onGroupMembersResponse.accept(msg);
                }
                case GROUP_ADD_MEMBER, GROUP_MEMBER_ADD -> {
                    if (onGroupMemberAdded != null) onGroupMemberAdded.accept(msg);
                }
                case GROUP_REMOVE_MEMBER, GROUP_MEMBER_REMOVE -> {
                    if (onGroupMemberRemoved != null) onGroupMemberRemoved.accept(msg);
                }
                case GROUP_HISTORY_RESPONSE -> {
                    if (onGroupHistoryResponse != null) onGroupHistoryResponse.accept(msg);
                }
                default -> {
                    if (onMessageReceived != null) onMessageReceived.accept(msg);
                }
            }
        });
    }


}
