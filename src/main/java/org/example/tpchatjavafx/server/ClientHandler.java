package org.example.tpchatjavafx.server;

import org.example.tpchatjavafx.client.model.ChatMessage;
import org.example.tpchatjavafx.common.MessageType;
import org.example.tpchatjavafx.dao.ContactDAO;
import org.example.tpchatjavafx.dao.MessageDAO;
import org.example.tpchatjavafx.dao.UtilisateurDAO;
import org.example.tpchatjavafx.dao.ConversationDAO;
import org.example.tpchatjavafx.model.Message;
import org.example.tpchatjavafx.model.Utilisateur;
import org.example.tpchatjavafx.service.AuthService;

import java.io.*;
import java.net.Socket;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * GÃ¨re la connexion d'UN client (thread dÃ©diÃ©).
 * ResponsabilitÃ©s : auth, persistance messages, routage.
 */
public class ClientHandler implements Runnable {

    private final Socket     socket;
    private PrintWriter      out;
    private String           username;
    private int              userId;
    private final String     socketId;
    private static final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

    private final AuthService  authService = new AuthService();
    private final UtilisateurDAO userDAO   = new UtilisateurDAO();
    private final MessageDAO   messageDAO  = new MessageDAO();
    private final ConversationDAO convDAO  = new ConversationDAO();
    private final ContactDAO contactDAO    = new ContactDAO();

    public ClientHandler(Socket socket) {
        this.socket = socket;
        this.socketId = UUID.randomUUID().toString();
    }

    public String getUsername() { return username; }
    public int getUserId() { return userId; }
    public String getSocketId() { return socketId; }
    public Socket getSocket() { return socket; }

    // â”€â”€ Envoi â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    public void send(ChatMessage msg) {
        if (out != null) {
            out.println(msg.serialize());
            out.flush();
        }
    }

    // â”€â”€ Boucle principale â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

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

    // â”€â”€ Traitement d'une ligne â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private void processLine(String line) {
        if (line.isBlank()) return;
        ChatMessage msg = ChatMessage.deserialize(line);
        if (msg == null) return;

        switch (msg.getType()) {
            case LOGIN    -> handleLogin(msg);
            case REGISTER -> handleRegister(msg);
            case CONTACT_ADD -> {
                if (username != null) handleContactAdd(msg);
            }
            case CONTACT_DELETE -> {
                if (username != null) handleContactDelete(msg);
            }
            case DELETE_CONTACT -> {
                if (username != null) handleDeleteContact(msg);
            }
            case CONTACT_LOAD -> {
                if (username != null) handleContactLoad();
            }
            case HISTORY_REQUEST -> {
                if (username != null) handleHistoryRequest(msg);
            }
            case CHAT_CLEAR -> {
                if (username != null) handleClearPrivateChat(msg);
            }
            case CHAT_DELETE_EVERYONE -> {
                if (username != null) handleDeletePrivateChatForEveryone(msg);
            }
            case MESSAGE_CLEAR -> {
                if (username != null) handleClearMessageForMe(msg);
            }
            case MESSAGE_DELETE_EVERYONE -> {
                if (username != null) handleDeleteMessageForEveryone(msg);
            }
            case DELETE_MESSAGE -> {
                if (username != null) handleDeleteMessageForEveryone(msg);
            }
            case MESSAGE_READ -> {
                if (username != null) handleMessageRead(msg);
            }
            case LOGOUT   -> cleanup();

            // ===== APPELS =====
            case CALL_REQUEST -> {
                if (username != null) handleCallRequest(msg);
            }
            case CALL_ANSWER -> {
                if (username != null) handleCallAnswer(msg);
            }
            case CALL_REJECT -> {
                if (username != null) handleCallReject(msg);
            }
            case CALL_END -> {
                if (username != null) handleCallEnd(msg);
            }
            case GROUP_CREATE -> {
                if (username != null) handleGroupCreate(msg);
            }
            case GROUP_ADD_MEMBER, GROUP_MEMBER_ADD -> {
                if (username != null) handleGroupAddMember(msg);
            }
            case GROUP_REMOVE_MEMBER, REMOVE_GROUP_MEMBER, GROUP_MEMBER_REMOVE -> {
                if (username != null) handleGroupRemoveMember(msg);
            }
            case GROUP_LEAVE -> {
                if (username != null) handleGroupLeave(msg);
            }
            case GROUP_LIST -> {
                if (username != null) handleGroupList(msg);
            }
            case GROUP_MEMBERS -> {
                if (username != null) handleGroupMembers(msg);
            }
            case GROUP_MESSAGE, GROUP_AUDIO, GROUP_IMAGE, GROUP_FILE -> {
                if (username != null) handleGroupMessage(msg);
            }
            case GROUP_HISTORY_REQUEST -> {
                if (username != null) handleGroupHistoryRequest(msg);
            }
            case GROUP_CHAT_CLEAR -> {
                if (username != null) handleClearGroupChat(msg);
            }
            case GROUP_CHAT_DELETE_EVERYONE -> {
                if (username != null) handleDeleteGroupChatForEveryone(msg);
            }
            case GROUP_DELETE -> {
                if (username != null) handleGroupDelete(msg);
            }
            case MEETING_START, MEETING_INVITE -> {
                if (username != null) handleMeetingStart(msg);
            }
            case MEETING_JOIN, MEETING_PARTICIPANT_JOINED -> {
                if (username != null) handleMeetingJoin(msg);
            }
            case MEETING_LEAVE, MEETING_PARTICIPANT_LEFT -> {
                if (username != null) handleMeetingLeave(msg);
            }
            case MEETING_END, MEETING_ENDED -> {
                if (username != null) handleMeetingEnd(msg);
            }
            case MEETING_INFO -> {
                if (username != null) handleMeetingInfo(msg);
            }
            case MEETING_MIC_STATE -> {
                if (username != null) handleMicState(msg);
            }
            case MEETING_VIDEO_STATE -> {
                if (username != null) handleVideoState(msg);
            }
            case MEETING_PARTICIPANTS_REQUEST -> {
                if (username != null) handleMeetingParticipantsRequest(msg);
            }
            case MEETING_NON_PARTICIPANTS_REQUEST -> {
                if (username != null) handleMeetingNonParticipantsRequest(msg);
            }
            case MEETING_INVITE_USER -> {
                if (username != null) handleMeetingInviteUser(msg);
            }

            default       -> {
                // N'autoriser que les utilisateurs authentifiÃ©s
                if (username != null) persistAndRoute(msg);
            }
        }
    }

    // â”€â”€ Auth â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private void handleLogin(ChatMessage msg) {
        String uname    = msg.getFrom();
        String password = msg.getContent();

        Utilisateur user = authService.login(uname, password);
        if (user == null) {
            send(new ChatMessage(MessageType.AUTH_FAIL, "SERVER", uname, null, "Identifiants incorrects"));
            return;
        }

        setupSession(user);
    }

    private void handleRegister(ChatMessage msg) {
        String uname = msg.getFrom();
        String[] parts = msg.getContent().split("\\|", 2);
        String password = parts[0];
        String email = parts.length > 1 ? parts[1] : "";

        AuthService.RegisterResult res = authService.register(uname, password, email);
        if (!res.success()) {
            send(new ChatMessage(MessageType.AUTH_FAIL, "SERVER", uname, null, res.reason()));
            return;
        }

        try {
            Utilisateur user = userDAO.findByUsername(uname);
            setupSession(user);
        } catch (Exception e) {
            send(new ChatMessage(MessageType.AUTH_FAIL, "SERVER", uname, null, "Erreur serveur post-inscription"));
        }
    }

    private void handleContactAdd(ChatMessage msg) {
        String contactName = msg.getContent();
        if (contactName == null || contactName.isBlank()) return;

        try {
            Utilisateur contact = userDAO.findByUsername(contactName);
            if (contact == null) {
                send(new ChatMessage(MessageType.ERROR, "SERVER", username, null, "L'utilisateur " + contactName + " n'existe pas."));
                return;
            }

            if (contact.getId() == userId) {
                send(new ChatMessage(MessageType.ERROR, "SERVER", username, null, "Vous ne pouvez pas vous ajouter vous-mÃªme."));
                return;
            }

            boolean added = contactDAO.addContact(userId, contact.getId());
            if (added) {
                handleContactLoad(); // Envoyer la liste mise Ã  jour
            } else {
                send(new ChatMessage(MessageType.ERROR, "SERVER", username, null, "L'utilisateur est dÃ©jÃ  dans vos contacts."));
            }
        } catch (Exception e) {
            send(new ChatMessage(MessageType.ERROR, "SERVER", username, null, "Erreur lors de l'ajout du contact : " + e.getMessage()));
        }
    }

    private void handleContactLoad() {
        try {
            List<Utilisateur> contacts = contactDAO.getContacts(userId);
            String csv = contacts.stream()
                    .map(Utilisateur::getUsername)
                    .reduce((a, b) -> a + "," + b)
                    .orElse("");
            send(new ChatMessage(MessageType.CONTACT_LIST, "SERVER", username, null, csv));
        } catch (Exception e) {
            send(new ChatMessage(MessageType.ERROR, "SERVER", username, null, "Erreur chargement contacts : " + e.getMessage()));
        }
    }

    private void handleContactDelete(ChatMessage msg) {
        String contactName = msg.getContent();
        if (contactName == null || contactName.isBlank()) return;

        try {
            Utilisateur contact = userDAO.findByUsername(contactName);
            if (contact == null) {
                sendError("Le contact " + contactName + " est introuvable.");
                return;
            }

            boolean deleted = contactDAO.deleteContact(userId, contact.getId());
            if (!deleted) {
                sendError("Ce contact n'existe pas dans votre liste.");
                return;
            }
            
            org.example.tpchatjavafx.model.Conversation conv = convDAO.findConversationBetween(userId, contact.getId());
            if (conv != null) {
                messageDAO.deleteConversationMessages(conv.getId());
            }

            handleContactLoad();
        } catch (Exception e) {
            sendError("Erreur lors de la suppression du contact : " + e.getMessage());
        }
    }

    private void handleDeleteContact(ChatMessage msg) {
        try {
            int contactId = parseTargetUserId(msg);
            boolean deleted = contactDAO.hardDelete(contactId, userId);
            if (!deleted) {
                sendError("Ce contact n'existe pas dans votre liste.");
                return;
            }
            
            org.example.tpchatjavafx.model.Conversation conv = convDAO.findConversationBetween(userId, contactId);
            if (conv != null) {
                messageDAO.deleteConversationMessages(conv.getId());
            }

            ChatMessage notification = new ChatMessage(MessageType.DELETE_CONTACT, "SERVER", username, null, String.valueOf(contactId));
            send(notification);
            handleContactLoad();
        } catch (Exception e) {
            sendError("Erreur lors de la suppression definitive du contact : " + e.getMessage());
        }
    }

    private void setupSession(Utilisateur user) {
        this.username = user.getUsername();
        this.userId = user.getId();
        ChatServer.registerClient(username, userId, this);

        // Envoi auth success avec l'ID
        send(new ChatMessage(MessageType.AUTH_SUCCESS, "SERVER", String.valueOf(userId), null, username));
        System.out.println("[Auth] ConnectÃ© : " + username);

        // Push messages non lus
        try {
            List<Message> unread = messageDAO.getUnreadMessages(userId);
            for (Message m : unread) {
                ChatMessage cmsg = new ChatMessage(
                        MessageType.valueOf(m.getType()),
                        m.getExpediteur().getUsername(),
                        this.username,
                        String.valueOf(m.getConversationId()),
                        m.getContenu()
                );
                cmsg.setMessageId(m.getId());
                if (m.getDateEnvoi() != null) {
                    cmsg.setTimestamp(m.getDateEnvoi().format(timeFormatter));
                }
                cmsg.setRead(m.isEstLu());

                // Load binary data if it's a media message
                if (m.getType().contains("PRIVATE_AUDIO") || m.getType().contains("PRIVATE_IMAGE") || m.getType().contains("PRIVATE_FILE")) {
                    try {
                        org.example.tpchatjavafx.model.FichierMedia fm = fichierMediaDAO.findByMessageId(m.getId());
                        if (fm != null && fm.getCheminAcces() != null) {
                            java.io.File file = new java.io.File(fm.getCheminAcces());
                            if (file.exists()) {
                                cmsg.setBinaryData(java.nio.file.Files.readAllBytes(file.toPath()));
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("Erreur chargement mÃ©dia offline: " + e.getMessage());
                    }
                }

                send(cmsg);
                // Marquer lu ? Si le client l'affiche, oui
                messageDAO.markAsRead(m.getId());
            }
        } catch (Exception e) {
            System.err.println("Erreur push messages non lus : " + e.getMessage());
        }
    }

    private final org.example.tpchatjavafx.dao.FichierMediaDAO fichierMediaDAO = new org.example.tpchatjavafx.dao.FichierMediaDAO();

    // â”€â”€ Persistance + routage â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private void persistAndRoute(ChatMessage msg) {
        msg.setTimestamp(LocalDateTime.now().format(timeFormatter));
        if (msg.getType() == MessageType.PRIVATE || msg.getType() == MessageType.PRIVATE_AUDIO ||
                msg.getType() == MessageType.PRIVATE_IMAGE || msg.getType() == MessageType.PRIVATE_FILE) {
            try {
                Utilisateur receiver = userDAO.findByUsername(msg.getTo());
                if (receiver != null) {
                    int convId = -1;
                    if (msg.getConversationId() != null && !msg.getConversationId().isEmpty()) {
                        convId = Integer.parseInt(msg.getConversationId());
                    } else {
                        org.example.tpchatjavafx.model.Conversation conv = convDAO.createConversation(userId, receiver.getId());
                        convId = conv.getId();
                    }

                    Message m = new Message();
                    m.setContenu(msg.getContent() != null ? msg.getContent() : "Fichier");
                    m.setType(msg.getType().name());
                    m.setExpediteurId(userId);
                    m.setDestinataireId(receiver.getId());
                    m.setConversationId(convId);

                    Message saved = messageDAO.create(m);
                    msg.setMessageId(saved.getId());

                    // Handling FichierMedia
                    if (msg.getBinaryData() != null && msg.getBinaryData().length > 0) {
                        String uploadDir = "server_uploads";
                        File dir = new File(uploadDir);
                        if (!dir.exists()) dir.mkdirs();

                        String originalName = msg.getContent() != null ? msg.getContent() : "file.bin";
                        String safeName = System.currentTimeMillis() + "_" + originalName.replaceAll("[^a-zA-Z0-9.-]", "_");
                        File destFile = new File(dir, safeName);

                        try (FileOutputStream fos = new FileOutputStream(destFile)) {
                            fos.write(msg.getBinaryData());
                        }

                        org.example.tpchatjavafx.model.FichierMedia fm;
                        if (msg.getType() == MessageType.PRIVATE_AUDIO) {
                            org.example.tpchatjavafx.model.Vocal vocal = new org.example.tpchatjavafx.model.Vocal();
                            vocal.setDuree(0); // To compute or pass via msg later
                            fm = vocal;
                        } else {
                            fm = new org.example.tpchatjavafx.model.FichierMedia();
                        }

                        fm.setMessageId(saved.getId());
                        fm.setNomFichier(originalName);
                        fm.setCheminAcces(destFile.getAbsolutePath());
                        fm.setTaille(msg.getBinaryData().length);
                        fm.setType(msg.getType() == MessageType.PRIVATE_AUDIO ? "AUDIO" :
                                (msg.getType() == MessageType.PRIVATE_IMAGE ? "IMAGE" : "FILE"));

                        fichierMediaDAO.create(fm);
                    }

                    if (msg.getConversationId() == null || msg.getConversationId().isEmpty()) {
                        byte[] data = msg.getBinaryData();
                        msg = new ChatMessage(msg.getType(), msg.getFrom(), msg.getTo(), String.valueOf(convId), msg.getContent());
                        msg.setMessageId(saved.getId());
                        msg.setBinaryData(data);
                    }
                }
            } catch (Exception e) {
                System.err.println("[Handler] Persistance Ã©chouÃ©e : " + e.getMessage());
                e.printStackTrace();
            }
        }

        ChatServer.handleMessage(msg, this);
    }

    private void handleGroupCreate(ChatMessage msg) {
        try {
            String[] parts = splitContent(msg.getContent(), 3);
            List<Integer> members = parseUserIds(parts.length > 2 ? parts[2] : "");
            org.example.tpchatjavafx.model.Groupe groupe = ChatServer.getGroupManager()
                    .createGroup(parts[0], parts.length > 1 ? parts[1] : "", userId, members);

            ChatMessage created = new ChatMessage(MessageType.GROUP_CREATED, "SERVER", username, null, serializeGroupCreated(groupe));
            created.setGroupId(groupe.getId());
            send(created);
            handleGroupList(msg);

            ChatMessage invite = new ChatMessage(MessageType.GROUP_CREATED, "SERVER", null, null, serializeGroupCreated(groupe));
            invite.setGroupId(groupe.getId());
            ChatServer.broadcastToGroup(groupe.getId(), invite);
        } catch (Exception e) {
            sendError("Creation du groupe impossible : " + e.getMessage());
        }
    }

    private void handleGroupAddMember(ChatMessage msg) {
        try {
            int memberId = parseTargetUserId(msg);
            ChatServer.getGroupManager().addMember(msg.getGroupId(), userId, memberId);
            ChatMessage notification = new ChatMessage(MessageType.GROUP_ADD_MEMBER, username, null, null, usernameForUserId(memberId));
            notification.setGroupId(msg.getGroupId());
            ChatServer.broadcastToGroup(msg.getGroupId(), notification);
            sendGroupListToUser(memberId);
        } catch (Exception e) {
            sendError("Ajout de membre impossible : " + e.getMessage());
        }
    }

    private void handleGroupRemoveMember(ChatMessage msg) {
        try {
            int memberId = parseTargetUserId(msg);
            String memberUsername = usernameForUserId(memberId);
            ChatServer.getGroupManager().removeMember(msg.getGroupId(), userId, memberId);
            ChatMessage notification = new ChatMessage(MessageType.REMOVE_GROUP_MEMBER, "SERVER", null, null,
                    memberUsername + " a ete retire du groupe");
            notification.setGroupId(msg.getGroupId());
            ChatServer.broadcastToGroup(msg.getGroupId(), notification);
            ChatServer.sendToUserId(memberId, notification);
            sendGroupListToUser(memberId);
        } catch (Exception e) {
            sendError("Retrait de membre impossible : " + e.getMessage());
        }
    }

    private void handleGroupLeave(ChatMessage msg) {
        try {
            ChatServer.getGroupManager().leaveGroup(msg.getGroupId(), userId);
            ChatMessage notification = new ChatMessage(MessageType.GROUP_REMOVE_MEMBER, username, null, null, username);
            notification.setGroupId(msg.getGroupId());
            send(notification);
            ChatServer.broadcastToGroup(msg.getGroupId(), notification);
            handleGroupList(msg);
        } catch (Exception e) {
            sendError("Impossible de quitter le groupe : " + e.getMessage());
        }
    }

    private void handleGroupList(ChatMessage msg) {
        try {
            List<org.example.tpchatjavafx.model.Groupe> groups = ChatServer.getGroupManager().getGroupsForUser(userId);
            send(new ChatMessage(MessageType.GROUP_LIST_RESPONSE, "SERVER", username, null, ChatServer.getGroupManager().serializeGroups(groups, gid -> ChatServer.getMeetingManager().getActiveMeetingForGroup(gid) != null)));
        } catch (Exception e) {
            sendError("Chargement des groupes impossible : " + e.getMessage());
        }
    }

    private void handleGroupMembers(ChatMessage msg) {
        try {
            List<Utilisateur> members = ChatServer.getGroupManager().getMembers(msg.getGroupId(), userId);
            String content = members.stream()
                    .map(u -> u.getId() + ":" + u.getUsername() + ":" + safeRole(msg.getGroupId(), u.getId()))
                    .reduce((a, b) -> a + "," + b)
                    .orElse("");
            ChatMessage response = new ChatMessage(MessageType.GROUP_MEMBERS, "SERVER", username, null, content);
            response.setGroupId(msg.getGroupId());
            send(response);
        } catch (Exception e) {
            sendError("Chargement des membres impossible : " + e.getMessage());
        }
    }

    private void handleGroupMessage(ChatMessage msg) {
        try {
            ChatServer.getGroupManager().saveAndBroadcastGroupMessage(msg, userId, username);
        } catch (Exception e) {
            sendError("Message de groupe refuse : " + e.getMessage());
        }
    }

    private void handleGroupHistoryRequest(ChatMessage msg) {
        try {
            List<Message> history = ChatServer.getGroupManager().getGroupHistory(msg.getGroupId(), userId);
            ChatMessage begin = new ChatMessage(MessageType.GROUP_HISTORY_RESPONSE, "SERVER", username, null,
                    history.isEmpty() ? "__EMPTY__" : "__BEGIN__");
            begin.setGroupId(msg.getGroupId());
            send(begin);

            for (Message message : history) {
                String sender = message.getExpediteur() == null
                        ? String.valueOf(message.getExpediteurId())
                        : message.getExpediteur().getUsername();
                ChatMessage item = new ChatMessage(
                        MessageType.GROUP_HISTORY_RESPONSE,
                        sender,
                        username,
                        message.getType(),
                        message.getContenu()
                );
                item.setGroupId(msg.getGroupId());
                item.setMessageId(message.getId());
                if (message.getDateEnvoi() != null) {
                    item.setTimestamp(message.getDateEnvoi().format(timeFormatter));
                }
                attachMediaIfPresent(item, message);
                send(item);
            }
        } catch (Exception e) {
            sendError("Historique de groupe indisponible : " + e.getMessage());
        }
    }

    private void handleClearGroupChat(ChatMessage msg) {
        try {
            ChatServer.getGroupManager().getMembers(msg.getGroupId(), userId);
            messageDAO.markGroupCleared(userId, msg.getGroupId());
        } catch (Exception e) {
            sendError("Impossible d'effacer la discussion du groupe : " + e.getMessage());
        }
    }

    private void handleDeleteGroupChatForEveryone(ChatMessage msg) {
        try {
            ChatServer.getGroupManager().getMembers(msg.getGroupId(), userId);
            messageDAO.deleteGroupMessages(msg.getGroupId());
            ChatMessage notification = new ChatMessage(MessageType.GROUP_CHAT_DELETE_EVERYONE, "SERVER", null, null, "");
            notification.setGroupId(msg.getGroupId());
            ChatServer.broadcastToGroup(msg.getGroupId(), notification);
        } catch (Exception e) {
            sendError("Impossible de supprimer la discussion du groupe : " + e.getMessage());
        }
    }

    private void handleGroupDelete(ChatMessage msg) {
        try {
            ChatServer.getGroupManager().deleteGroup(msg.getGroupId(), userId);
        } catch (Exception e) {
            sendError("Suppression du groupe impossible : " + e.getMessage());
        }
    }

    private void handleMeetingStart(ChatMessage msg) {
        try {
            System.out.println("[SERVER] handleMeetingStart appele");
            System.out.println("[SERVER] msg.getMeetingType() = " + msg.getMeetingType());
            System.out.println("[SERVER] msg.getGroupId() = " + msg.getGroupId());
            System.out.println("[SERVER] userId = " + userId);

            String meetingType = msg.getMeetingType();
            if (meetingType == null || meetingType.isBlank()) {
                System.out.println("[SERVER] ERREUR: meetingType est null ou vide !");
                sendError("Type de reunion non specifie (AUDIO ou VIDEO)");
                return;
            }

            MeetingManager.MeetingSession existing = ChatServer.getMeetingManager().getActiveMeetingForGroup(msg.getGroupId());
            if (existing != null) {
                System.out.println("[SERVER] Reunion deja active (" + existing.getMeetingId() + "), on rejoint...");

                ChatMessage started = new ChatMessage(MessageType.MEETING_STARTED, "SERVER", username, null, "Reunion rejointe");
                started.setGroupId(msg.getGroupId());
                started.setMeetingId(existing.getMeetingId());
                started.setMeetingType(existing.getType());
                send(started);

                send(ChatServer.getMeetingManager().buildMeetingInfo(existing, username));

                handleMeetingJoin(msg);
                return;
            }

            MeetingManager.MeetingSession session = ChatServer.getMeetingManager()
                    .startMeeting(msg.getGroupId(), userId, username, meetingType, this);

            System.out.println("[SERVER] Reunion creee: " + session.getMeetingId());

            ChatMessage started = new ChatMessage(MessageType.MEETING_STARTED, "SERVER", username, null, "Reunion demarree");
            started.setGroupId(msg.getGroupId());
            started.setMeetingId(session.getMeetingId());
            started.setMeetingType(session.getType());
            send(started);

            ChatMessage info = ChatServer.getMeetingManager().buildMeetingInfo(session, username);
            send(info);

            ChatMessage invite = new ChatMessage(MessageType.MEETING_INVITE, username, null, null,
                    "Invitation reunion " + meetingType);
            invite.setGroupId(msg.getGroupId());
            invite.setMeetingId(session.getMeetingId());
            invite.setMeetingType(session.getType());

            System.out.println("[SERVER] Broadcast invitation aux membres du groupe " + msg.getGroupId());
            ChatServer.broadcastToGroupExcept(msg.getGroupId(), invite, userId);

        } catch (Exception e) {
            System.out.println("[SERVER] ERREUR handleMeetingStart: " + e.getMessage());
            e.printStackTrace();
            sendError("Demarrage de reunion impossible : " + e.getMessage());
        }
    }

    private void handleMeetingJoin(ChatMessage msg) {
        try {
            System.out.println("[SERVER] handleMeetingJoin appele");
            int mid = msg.getMeetingId();
            int gid = msg.getGroupId();

            System.out.println("[SERVER] meetingId=" + mid + ", groupId=" + gid);

            if (mid <= 0 && gid > 0) {
                MeetingManager.MeetingSession s = ChatServer.getMeetingManager().getActiveMeetingForGroup(gid);
                if (s != null) {
                    mid = s.getMeetingId();
                    System.out.println("[SERVER] Reunion active trouvee pour groupe " + gid + ": " + mid);
                } else {
                    System.out.println("[SERVER] Aucune reunion active pour groupe " + gid);
                    sendError("Aucune reunion active dans ce groupe");
                    return;
                }
            }

            if (mid <= 0) {
                sendError("ID de reunion invalide");
                return;
            }

            MeetingManager.MeetingSession session = ChatServer.getMeetingManager().getActiveMeeting(mid);
            if (session == null) {
                sendError("Reunion inactive ou introuvable");
                return;
            }

            int udpAudioPort = msg.getUdpAudioPort();
            int udpVideoPort = msg.getUdpVideoPort();

            boolean alreadyParticipant = session.getParticipants().containsKey(userId);
            if (alreadyParticipant) {
                System.out.println("[SERVER] User " + userId + " deja dans la reunion, mise a jour des ports UDP");
                ChatServer.getMeetingManager().updateParticipantMediaPorts(mid, userId, this, udpAudioPort, udpVideoPort);
            } else {
                ChatServer.getMeetingManager().joinMeeting(mid, userId, username, this, udpAudioPort, udpVideoPort);
                System.out.println("[SERVER] " + username + " a rejoint la reunion " + mid);
            }

            send(ChatServer.getMeetingManager().buildMeetingInfo(session, username));

            ChatMessage participants = new ChatMessage(MessageType.MEETING_PARTICIPANTS, "SERVER", username, null,
                    ChatServer.getMeetingManager().serializeParticipants(mid));
            participants.setMeetingId(mid);
            participants.setGroupId(session.getGroupeId());
            send(participants);

        } catch (Exception e) {
            System.out.println("[SERVER] ERREUR handleMeetingJoin: " + e.getMessage());
            e.printStackTrace();
            sendError("Impossible de rejoindre la reunion : " + e.getMessage());
        }
    }

    private void handleMeetingLeave(ChatMessage msg) {
        try {
            ChatServer.getMeetingManager().leaveMeeting(msg.getMeetingId(), userId);
        } catch (Exception e) {
            sendError("Impossible de quitter la reunion : " + e.getMessage());
        }
    }

    private void handleMeetingEnd(ChatMessage msg) {
        try {
            ChatServer.getMeetingManager().endMeeting(msg.getMeetingId(), userId);
        } catch (Exception e) {
            sendError("Impossible de terminer la reunion : " + e.getMessage());
        }
    }

    private void attachMediaIfPresent(ChatMessage chatMessage, Message message) {
        String type = message.getType();
        if (type == null || !(type.contains("AUDIO") || type.contains("IMAGE") || type.contains("FILE"))) return;
        try {
            org.example.tpchatjavafx.model.FichierMedia media = fichierMediaDAO.findByMessageId(message.getId());
            if (media == null || media.getCheminAcces() == null) return;
            java.io.File file = new java.io.File(media.getCheminAcces());
            if (file.exists()) {
                chatMessage.setBinaryData(java.nio.file.Files.readAllBytes(file.toPath()));
            }
        } catch (Exception e) {
            System.err.println("Erreur chargement media historique: " + e.getMessage());
        }
    }

    private void handleMeetingParticipantsRequest(ChatMessage msg) {
        try {
            MeetingManager.MeetingSession session = ChatServer.getMeetingManager().getActiveMeeting(msg.getMeetingId());
            if (session == null) {
                sendError("Reunion introuvable.");
                return;
            }
            ChatMessage participants = new ChatMessage(MessageType.MEETING_PARTICIPANTS, "SERVER", username, null,
                    ChatServer.getMeetingManager().serializeParticipants(msg.getMeetingId()));
            participants.setMeetingId(msg.getMeetingId());
            participants.setGroupId(session.getGroupeId());
            send(participants);
        } catch (Exception e) {
            sendError("Impossible de lister les participants : " + e.getMessage());
        }
    }

    private void handleMeetingNonParticipantsRequest(ChatMessage msg) {
        try {
            MeetingManager.MeetingSession session = ChatServer.getMeetingManager().getActiveMeeting(msg.getMeetingId());
            if (session == null) {
                sendError("Reunion introuvable.");
                return;
            }
            int gid = msg.getGroupId() > 0 ? msg.getGroupId() : session.getGroupeId();
            String csv = ChatServer.getMeetingManager().serializeNonParticipants(msg.getMeetingId(), gid);
            ChatMessage response = new ChatMessage(MessageType.MEETING_NON_PARTICIPANTS, "SERVER", username, null, csv);
            response.setMeetingId(msg.getMeetingId());
            response.setGroupId(gid);
            send(response);
        } catch (Exception e) {
            sendError("Impossible de lister les membres disponibles : " + e.getMessage());
        }
    }

    private void handleMeetingInviteUser(ChatMessage msg) {
        try {
            MeetingManager.MeetingSession session = ChatServer.getMeetingManager().getActiveMeeting(msg.getMeetingId());
            if (session == null) {
                sendError("Reunion introuvable.");
                return;
            }
            org.example.tpchatjavafx.model.Utilisateur target = userDAO.findByUsername(msg.getContent());
            if (target == null) {
                sendError("Utilisateur introuvable : " + msg.getContent());
                return;
            }
            ChatMessage invite = new ChatMessage(MessageType.MEETING_INVITE, username, null, null,
                    "Invitation reunion " + session.getType());
            invite.setGroupId(session.getGroupeId());
            invite.setMeetingId(session.getMeetingId());
            invite.setMeetingType(session.getType());
            ChatServer.sendToUserId(target.getId(), invite);
        } catch (Exception e) {
            sendError("Invitation impossible : " + e.getMessage());
        }
    }

    private void handleMicState(ChatMessage msg) {
        try {
            MeetingManager.MeetingSession session = ChatServer.getMeetingManager().getActiveMeeting(msg.getMeetingId());
            if (session == null) return;
            ChatMessage broadcast = new ChatMessage(MessageType.MEETING_MIC_STATE, username, String.valueOf(userId), null, msg.getContent());
            broadcast.setMeetingId(msg.getMeetingId());
            broadcast.setGroupId(session.getGroupeId());
            ChatServer.getMeetingManager().notifyParticipants(session, broadcast, -1);
        } catch (Exception e) {
            sendError("Etat micro impossible : " + e.getMessage());
        }
    }

    private void handleVideoState(ChatMessage msg) {
        try {
            MeetingManager.MeetingSession session = ChatServer.getMeetingManager().getActiveMeeting(msg.getMeetingId());
            if (session == null) return;
            ChatMessage broadcast = new ChatMessage(MessageType.MEETING_VIDEO_STATE, username, String.valueOf(userId), null, msg.getContent());
            broadcast.setMeetingId(msg.getMeetingId());
            broadcast.setGroupId(session.getGroupeId());
            ChatServer.getMeetingManager().notifyParticipants(session, broadcast, -1);
        } catch (Exception e) {
            sendError("Etat video impossible : " + e.getMessage());
        }
    }

    private void handleMeetingInfo(ChatMessage msg) {
        try {
            int mid = msg.getMeetingId();
            if (mid <= 0 && msg.getGroupId() > 0) {
                MeetingManager.MeetingSession s = ChatServer.getMeetingManager().getActiveMeetingForGroup(msg.getGroupId());
                if (s != null) mid = s.getMeetingId();
            }
            MeetingManager.MeetingSession session = ChatServer.getMeetingManager().getActiveMeeting(mid);
            if (session == null) throw new IllegalArgumentException("Reunion inactive ou introuvable.");
            send(ChatServer.getMeetingManager().buildMeetingInfo(session, username));
        } catch (Exception e) {
            sendError("Informations reunion indisponibles : " + e.getMessage());
        }
    }

    private String[] splitContent(String content, int expected) {
        String[] parts = (content == null ? "" : content).split(";", expected);
        String[] normalized = new String[expected];
        for (int i = 0; i < expected; i++) normalized[i] = i < parts.length ? parts[i].trim() : "";
        return normalized;
    }

    private List<Integer> parseUserIds(String raw) {
        List<Integer> ids = new java.util.ArrayList<>();
        if (raw == null || raw.isBlank()) return ids;
        for (String token : raw.split(",")) {
            String t = token.trim();
            if (t.isBlank()) continue;
            try {
                ids.add(Integer.parseInt(t));
            } catch (NumberFormatException e) {
                // RÃ©solution par pseudo
                try {
                    org.example.tpchatjavafx.model.Utilisateur u = userDAO.findByUsername(t);
                    if (u != null) ids.add(u.getId());
                } catch (Exception ignored) {}
            }
        }
        return ids;
    }

    private int parseTargetUserId(ChatMessage msg) {
        String content = msg.getContent();
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("ID ou pseudo utilisateur manquant.");
        }
        try {
            return Integer.parseInt(content.trim());
        } catch (NumberFormatException e) {
            // Tentative de rÃ©solution par pseudo
            try {
                org.example.tpchatjavafx.model.Utilisateur u = userDAO.findByUsername(content.trim());
                if (u != null) return u.getId();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            throw new IllegalArgumentException("Utilisateur '" + content + "' introuvable.");
        }
    }

    private String usernameForUserId(int id) throws SQLException {
        Utilisateur user = userDAO.findById(id);
        return user == null ? String.valueOf(id) : user.getUsername();
    }

    private void sendError(String message) {
        send(new ChatMessage(MessageType.ERROR, "SERVER", username, null, message));
    }

    // â”€â”€ Nettoyage â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private void sendGroupListToUser(int targetUserId) {
        try {
            List<org.example.tpchatjavafx.model.Groupe> groups = ChatServer.getGroupManager().getGroupsForUser(targetUserId);
            Utilisateur target = userDAO.findById(targetUserId);
            ChatMessage response = new ChatMessage(
                    MessageType.GROUP_LIST_RESPONSE,
                    "SERVER",
                    target == null ? null : target.getUsername(),
                    null,
                    ChatServer.getGroupManager().serializeGroups(groups, gid -> ChatServer.getMeetingManager().getActiveMeetingForGroup(gid) != null)
            );
            ChatServer.sendToUserId(targetUserId, response);
        } catch (Exception e) {
            System.err.println("Erreur rafraichissement groupes utilisateur " + targetUserId + ": " + e.getMessage());
        }
    }

    private String serializeGroupCreated(org.example.tpchatjavafx.model.Groupe groupe) {
        String name = groupe.getNom() == null ? "" : groupe.getNom().replace("|", " ").replace(",", " ");
        return groupe.getId() + "|" + name;
    }

    private String safeRole(int groupId, int memberId) {
        try {
            return ChatServer.getGroupManager().getRole(groupId, memberId);
        } catch (Exception e) {
            return "MEMBRE";
        }
    }

    private void cleanup() {
        if (username != null) {
            ChatServer.removeClient(username, userId, this);
        }
        try { if (!socket.isClosed()) socket.close(); }
        catch (IOException ignored) {}
    }

    private void handleCallRequest(ChatMessage msg) {
        String targetUser = msg.getTo();
        String callType = msg.getCallType();

        System.out.println("[Server] " + username + " appelle " + targetUser +
                " (" + callType + ")");

        Set<ClientHandler> targetHandlers = ChatServer.clients.get(targetUser);
        if (targetHandlers != null && !targetHandlers.isEmpty()) {
            // Utilisateur en ligne - envoyer notification
            ChatMessage notification = new ChatMessage();
            notification.setType(MessageType.CALL_INCOMING);
            notification.setFrom(username);
            notification.setTo(targetUser);
            notification.setCallType(callType);
            notification.setRemoteHost(socket.getInetAddress().getHostAddress());
            notification.setRemotePort(9999); // Port local pour rÃ©ception

            for (ClientHandler handler : targetHandlers) {
                handler.send(notification);
            }
        } else {
            // Utilisateur non connectÃ©
            ChatMessage response = new ChatMessage();
            response.setType(MessageType.ERROR);
            response.setContent("Utilisateur hors ligne");
            send(response);
        }
    }

    private void handleCallAnswer(ChatMessage msg) {
        String callerId = msg.getFrom();
        String targetUser = msg.getTo();

        System.out.println("[Server] " + username + " accepte appel de " + callerId);

        Set<ClientHandler> callerHandlers = ChatServer.clients.get(callerId);
        if (callerHandlers != null && !callerHandlers.isEmpty()) {
            ChatMessage answer = new ChatMessage();
            answer.setType(MessageType.CALL_ANSWER);
            answer.setFrom(username);
            answer.setTo(callerId);
            answer.setCallType(msg.getCallType());
            answer.setRemoteHost(socket.getInetAddress().getHostAddress());
            answer.setRemotePort(10000); // Port local pour rÃ©ception

            for (ClientHandler handler : callerHandlers) {
                handler.send(answer);
            }
        }
    }

    private void handleCallReject(ChatMessage msg) {
        String callerId = msg.getFrom();

        System.out.println("[Server] " + username + " refuse appel de " + callerId);

        Set<ClientHandler> callerHandlers = ChatServer.clients.get(callerId);
        if (callerHandlers != null) {
            ChatMessage rejection = new ChatMessage();
            rejection.setType(MessageType.CALL_REJECT);
            rejection.setFrom(username);
            rejection.setCallType(msg.getCallType());

            for (ClientHandler handler : callerHandlers) {
                handler.send(rejection);
            }
        }
    }

    private void handleCallEnd(ChatMessage msg) {
        System.out.println("[Server] " + username + " termine appel");
        // Notification au client distant (optionnel)
    }

    private void handleHistoryRequest(ChatMessage msg) {
        String otherUsername = msg.getContent();
        try {
            Utilisateur other = userDAO.findByUsername(otherUsername);
            if (other == null) return;

            org.example.tpchatjavafx.model.Conversation conv = convDAO.findConversationBetween(userId, other.getId());
            if (conv != null) {
                List<Message> history = messageDAO.getHistoryForUser(conv.getId(), userId);
                for (Message m : history) {
                    String senderName = m.getExpediteur().getUsername();
                    String recipientName = senderName.equals(username) ? otherUsername : username;

                    ChatMessage syncMsg = new ChatMessage(
                            MessageType.SYNC_HISTORY,
                            senderName,
                            recipientName,
                            m.getType(), // Pass original type (PRIVATE_AUDIO, etc.)
                        m.getContenu()
                );
                syncMsg.setMessageId(m.getId());
                syncMsg.setRead(m.isEstLu());
                if (m.getDateEnvoi() != null) {
                    syncMsg.setTimestamp(m.getDateEnvoi().format(timeFormatter));
                }

                    // Charger les donnÃ©es binaires si c'est un mÃ©dia
                    if (m.getType().contains("AUDIO") || m.getType().contains("IMAGE") || m.getType().contains("FILE")) {
                        try {
                            org.example.tpchatjavafx.model.FichierMedia fm = fichierMediaDAO.findByMessageId(m.getId());
                            if (fm != null && fm.getCheminAcces() != null) {
                                java.io.File file = new java.io.File(fm.getCheminAcces());
                                if (file.exists()) {
                                    syncMsg.setBinaryData(java.nio.file.Files.readAllBytes(file.toPath()));
                                }
                            }
                        } catch (Exception e) {
                            System.err.println("Erreur chargement mÃ©dia historique: " + e.getMessage());
                        }
                    }
                    send(syncMsg);
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur history: " + e.getMessage());
        }
    }

    private void handleClearPrivateChat(ChatMessage msg) {
        String otherUsername = msg.getContent();
        try {
            Utilisateur other = userDAO.findByUsername(otherUsername);
            if (other == null) return;

            org.example.tpchatjavafx.model.Conversation conv = convDAO.findConversationBetween(userId, other.getId());
            if (conv != null) {
                messageDAO.markConversationCleared(userId, conv.getId());
            }
        } catch (SQLException e) {
            sendError("Impossible d'effacer le chat : " + e.getMessage());
        }
    }

    private void handleClearMessageForMe(ChatMessage msg) {
        try {
            messageDAO.markMessageCleared(userId, msg.getMessageId());
        } catch (SQLException e) {
            sendError("Impossible de supprimer le message : " + e.getMessage());
        }
    }

    private void handleMessageRead(ChatMessage msg) {
        try {
            Message message = messageDAO.findById(msg.getMessageId());
            if (message == null || message.getDestinataireId() == null || message.getDestinataireId() != userId) {
                return;
            }
            messageDAO.markAsRead(msg.getMessageId());
            ChatMessage receipt = new ChatMessage(MessageType.MESSAGE_READ, username, null, null, "");
            receipt.setMessageId(msg.getMessageId());
            receipt.setRead(true);
            ChatServer.sendToUserId(message.getExpediteurId(), receipt);
        } catch (SQLException e) {
            sendError("Impossible de marquer le message comme lu : " + e.getMessage());
        }
    }

    private void handleDeleteMessageForEveryone(ChatMessage msg) {
        try {
            Message message = messageDAO.findById(msg.getMessageId());
            if (message == null) return;

            boolean deleted = messageDAO.deleteMessageForEveryone(msg.getMessageId(), userId);
            if (!deleted) return;

            ChatMessage notification = new ChatMessage(MessageType.DELETE_MESSAGE, username, null, null, "");
            notification.setMessageId(msg.getMessageId());
            if (message.getGroupeId() != null && message.getGroupeId() > 0) {
                notification.setGroupId(message.getGroupeId());
                ChatServer.broadcastToGroup(message.getGroupeId(), notification);
                return;
            }

            if (message.getExpediteurId() > 0) {
                ChatServer.sendToUserId(message.getExpediteurId(), notification);
            }
            if (message.getDestinataireId() != null && message.getDestinataireId() > 0) {
                ChatServer.sendToUserId(message.getDestinataireId(), notification);
            }
        } catch (SQLException e) {
            sendError("Impossible de supprimer le message pour tout le monde : " + e.getMessage());
        }
    }

    private void handleDeletePrivateChatForEveryone(ChatMessage msg) {
        String otherUsername = msg.getContent();
        try {
            Utilisateur other = userDAO.findByUsername(otherUsername);
            if (other == null) return;

            org.example.tpchatjavafx.model.Conversation conv = convDAO.findConversationBetween(userId, other.getId());
            if (conv != null) {
                messageDAO.deleteConversationMessages(conv.getId());
            }

            ChatMessage notification = new ChatMessage(MessageType.CHAT_DELETE_EVERYONE, username, otherUsername, null, username);
            ChatServer.sendToUserId(other.getId(), notification);
            ChatServer.sendToUserId(userId, notification);
        } catch (SQLException e) {
            sendError("Impossible de supprimer le chat : " + e.getMessage());
        }
    }
}

