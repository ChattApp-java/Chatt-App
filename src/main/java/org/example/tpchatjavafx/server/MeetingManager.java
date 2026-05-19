package org.example.tpchatjavafx.server;

import org.example.tpchatjavafx.client.model.ChatMessage;
import org.example.tpchatjavafx.common.MessageType;
import org.example.tpchatjavafx.dao.GroupeDAO;
import org.example.tpchatjavafx.dao.GroupeMembreDAO;
import org.example.tpchatjavafx.dao.ReunionDAO;
import org.example.tpchatjavafx.model.Groupe;
import org.example.tpchatjavafx.model.Reunion;
import org.example.tpchatjavafx.model.Utilisateur;

import java.net.InetAddress;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class MeetingManager {
    private final ReunionDAO reunionDAO;
    private final GroupeMembreDAO membreDAO;
    private final GroupeDAO groupeDAO;
    private final UDPRelayServer udpRelayServer;
    private final Map<Integer, MeetingSession> activeMeetings = new ConcurrentHashMap<>();

    public MeetingManager(UDPRelayServer udpRelayServer) {
        this(new ReunionDAO(), new GroupeMembreDAO(), new GroupeDAO(), udpRelayServer);
    }

    MeetingManager(ReunionDAO reunionDAO, GroupeMembreDAO membreDAO, GroupeDAO groupeDAO, UDPRelayServer udpRelayServer) {
        this.reunionDAO = reunionDAO;
        this.membreDAO = membreDAO;
        this.groupeDAO = groupeDAO;
        this.udpRelayServer = udpRelayServer;
    }

    public MeetingSession startMeeting(int groupeId, int initiatorId, String initiatorUsername, String type, ClientHandler handler) throws SQLException {
        System.out.println("[MEETING_MGR] startMeeting() groupe=" + groupeId + " initiator=" + initiatorId + " type=" + type);

        if (!isMemberOrCreator(groupeId, initiatorId)) {
            throw new SecurityException("Utilisateur non membre du groupe.");
        }

        MeetingSession existingSession = activeMeetings.values().stream()
                .filter(s -> s.getGroupeId() == groupeId)
                .findFirst()
                .orElse(null);

        if (existingSession != null) {
            System.out.println("[MEETING_MGR] Reunion deja active en memoire: " + existingSession.getMeetingId());
            if (existingSession.getParticipants().containsKey(initiatorId)) {
                throw new IllegalStateException("Vous etes deja dans une reunion active pour ce groupe.");
            }
            System.out.println("[MEETING_MGR] Rejointure de la reunion existante");
            joinMeeting(existingSession.getMeetingId(), initiatorId, initiatorUsername, handler, 0, 0);
            return existingSession;
        }

        Reunion activeInDb = reunionDAO.findActiveByGroupeId(groupeId);
        if (activeInDb != null) {
            System.out.println("[MEETING_MGR] Reunion fantome trouvee en BDD: " + activeInDb.getId() + ", on la termine et en cree une nouvelle");
            reunionDAO.endMeeting(activeInDb.getId());
            activeMeetings.remove(activeInDb.getId());
            udpRelayServer.unregisterMeeting(activeInDb.getId());
        }

        Reunion reunion = reunionDAO.create(groupeId, initiatorId, type);
        MeetingSession session = new MeetingSession(reunion.getId(), groupeId, initiatorId, reunion.getType());
        activeMeetings.put(reunion.getId(), session);

        System.out.println("[MEETING_MGR] Nouvelle reunion creee: " + reunion.getId());

        joinMeeting(reunion.getId(), initiatorId, initiatorUsername, handler, 0, 0);

        return session;
    }

    public ParticipantInfo joinMeeting(int meetingId, int userId, String username, ClientHandler handler, int udpAudioPort, int udpVideoPort) throws SQLException {
        MeetingSession session = requireSession(meetingId);
        if (!isMemberOrCreator(session.getGroupeId(), userId)) throw new SecurityException("Utilisateur non membre du groupe.");
        reunionDAO.addParticipant(meetingId, userId);

        String address = handler.getSocket().getInetAddress().getHostAddress();
        ParticipantInfo participant = new ParticipantInfo(userId, username, address, udpAudioPort, udpVideoPort, handler);
        session.participants.put(userId, participant);
        try {
            udpRelayServer.registerParticipant(meetingId, userId, InetAddress.getByName(address), udpAudioPort, udpVideoPort);
        } catch (java.net.UnknownHostException e) {
            throw new IllegalStateException("Adresse reseau participant invalide.", e);
        }

        ChatMessage joined = new ChatMessage(MessageType.MEETING_PARTICIPANT_JOINED, username, null, null, serializeParticipants(meetingId));
        joined.setMeetingId(meetingId);
        joined.setGroupId(session.getGroupeId());
        notifyParticipants(session, joined, userId);
        return participant;
    }

    public void updateParticipantMediaPorts(int meetingId, int userId, ClientHandler handler, int udpAudioPort, int udpVideoPort) throws SQLException {
        MeetingSession session = requireSession(meetingId);
        ParticipantInfo current = session.participants.get(userId);
        if (current == null) {
            return;
        }

        int resolvedAudioPort = udpAudioPort > 0 ? udpAudioPort : current.udpAudioPort;
        int resolvedVideoPort = udpVideoPort > 0 ? udpVideoPort : current.udpVideoPort;
        String address = handler.getSocket().getInetAddress().getHostAddress();

        ParticipantInfo refreshed = new ParticipantInfo(
                userId,
                current.username,
                address,
                resolvedAudioPort,
                resolvedVideoPort,
                handler
        );
        session.participants.put(userId, refreshed);

        try {
            udpRelayServer.registerParticipant(meetingId, userId, InetAddress.getByName(address), resolvedAudioPort, resolvedVideoPort);
        } catch (java.net.UnknownHostException e) {
            throw new IllegalStateException("Adresse reseau participant invalide.", e);
        }
    }

    public void leaveMeeting(int meetingId, int userId) throws SQLException {
        MeetingSession session = activeMeetings.get(meetingId);
        if (session == null) return;
        ParticipantInfo removed = session.participants.remove(userId);
        reunionDAO.removeParticipant(meetingId, userId);
        udpRelayServer.unregisterParticipant(meetingId, userId);
        if (removed != null) {
            ChatMessage left = new ChatMessage(MessageType.MEETING_PARTICIPANT_LEFT, removed.username, null, null, serializeParticipants(meetingId));
            left.setMeetingId(meetingId);
            left.setGroupId(session.getGroupeId());
            notifyParticipants(session, left, userId);
        }
        if (session.participants.isEmpty()) {
            reunionDAO.endMeeting(meetingId);
            activeMeetings.remove(meetingId);
            udpRelayServer.unregisterMeeting(meetingId);
        }
    }

    public void endMeeting(int meetingId, int requesterId) throws SQLException {
        MeetingSession session = requireSession(meetingId);
        if (session.getInitiatorId() != requesterId) throw new SecurityException("Seul l'initiateur peut terminer la reunion.");
        ChatMessage ended = new ChatMessage(MessageType.MEETING_ENDED, "SERVER", null, null, String.valueOf(meetingId));
        ended.setMeetingId(meetingId);
        ended.setGroupId(session.getGroupeId());
        notifyParticipants(session, ended, -1);
        reunionDAO.endMeeting(meetingId);
        activeMeetings.remove(meetingId);
        udpRelayServer.unregisterMeeting(meetingId);
    }

    public MeetingSession getActiveMeeting(int meetingId) {
        return activeMeetings.get(meetingId);
    }

    public MeetingSession getActiveMeetingForGroup(int groupeId) {
        return activeMeetings.values().stream().filter(s -> s.getGroupeId() == groupeId).findFirst().orElse(null);
    }

    public List<ParticipantInfo> listParticipants(int meetingId) {
        MeetingSession session = activeMeetings.get(meetingId);
        if (session == null) return List.of();
        return new ArrayList<>(session.participants.values());
    }

    public ChatMessage buildMeetingInfo(MeetingSession session, String to) {
        ChatMessage info = new ChatMessage(
                MessageType.MEETING_INFO,
                "SERVER",
                to,
                null,
                serializeParticipants(session.getMeetingId())
        );
        info.setMeetingId(session.getMeetingId());
        info.setGroupId(session.getGroupeId());
        info.setMeetingType(session.getType());
        info.setServerHost(udpRelayServer.getHost());
        info.setServerUdpAudioPort(udpRelayServer.getAudioPort());
        info.setServerUdpVideoPort(udpRelayServer.getVideoPort());
        return info;
    }

    public String serializeParticipants(int meetingId) {
        return listParticipants(meetingId).stream()
                .map(p -> p.userId + ":" + p.username)
                .collect(Collectors.joining(","));
    }

    public String serializeNonParticipants(int meetingId, int groupeId) throws SQLException {
        MeetingSession session = activeMeetings.get(meetingId);
        if (session == null) return "";
        java.util.Set<Integer> inMeeting = session.participants.keySet();
        return membreDAO.getMembers(groupeId).stream()
                .filter(u -> !inMeeting.contains(u.getId()))
                .map(Utilisateur::getUsername)
                .collect(Collectors.joining(","));
    }

    private MeetingSession requireSession(int meetingId) {
        MeetingSession session = activeMeetings.get(meetingId);
        if (session == null) throw new IllegalArgumentException("Reunion inactive ou introuvable.");
        return session;
    }

    private boolean isMemberOrCreator(int groupeId, int userId) throws SQLException {
        Groupe groupe = groupeDAO.findById(groupeId);
        if (groupe != null && groupe.getCreateurId() == userId) return true;
        return membreDAO.isMember(groupeId, userId);
    }

    public void notifyParticipants(MeetingSession session, ChatMessage msg, int exceptUserId) {
        for (ParticipantInfo participant : session.participants.values()) {
            if (exceptUserId >= 0 && participant.userId == exceptUserId) continue;
            if (participant.handler != null) participant.handler.send(msg);
        }
    }

    public static class MeetingSession {
        private final int meetingId;
        private final int groupeId;
        private final int initiatorId;
        private final String type;
        private final Map<Integer, ParticipantInfo> participants = new ConcurrentHashMap<>();

        MeetingSession(int meetingId, int groupeId, int initiatorId, String type) {
            this.meetingId = meetingId;
            this.groupeId = groupeId;
            this.initiatorId = initiatorId;
            this.type = type;
        }

        public int getMeetingId() { return meetingId; }
        public int getGroupeId() { return groupeId; }
        public int getInitiatorId() { return initiatorId; }
        public String getType() { return type; }
        public Map<Integer, ParticipantInfo> getParticipants() { return Collections.unmodifiableMap(participants); }
    }

    public static class ParticipantInfo {
        public final int userId;
        public final String username;
        public final String address;
        public final int udpAudioPort;
        public final int udpVideoPort;
        public final ClientHandler handler;

        ParticipantInfo(int userId, String username, String address, int udpAudioPort, int udpVideoPort, ClientHandler handler) {
            this.userId = userId;
            this.username = username;
            this.address = address;
            this.udpAudioPort = udpAudioPort;
            this.udpVideoPort = udpVideoPort;
            this.handler = handler;
        }
    }
}
