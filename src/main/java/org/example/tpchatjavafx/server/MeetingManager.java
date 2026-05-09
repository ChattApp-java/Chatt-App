package org.example.tpchatjavafx.server;

import org.example.tpchatjavafx.client.model.ChatMessage;
import org.example.tpchatjavafx.common.MessageType;
import org.example.tpchatjavafx.dao.GroupeMembreDAO;
import org.example.tpchatjavafx.dao.ReunionDAO;
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
    private final UDPRelayServer udpRelayServer;
    private final Map<Integer, MeetingSession> activeMeetings = new ConcurrentHashMap<>();

    public MeetingManager(UDPRelayServer udpRelayServer) {
        this(new ReunionDAO(), new GroupeMembreDAO(), udpRelayServer);
    }

    MeetingManager(ReunionDAO reunionDAO, GroupeMembreDAO membreDAO, UDPRelayServer udpRelayServer) {
        this.reunionDAO = reunionDAO;
        this.membreDAO = membreDAO;
        this.udpRelayServer = udpRelayServer;
    }

    public MeetingSession startMeeting(int groupeId, int initiatorId, String initiatorUsername, String type, ClientHandler handler) throws SQLException {
        if (!membreDAO.isMember(groupeId, initiatorId)) throw new SecurityException("Utilisateur non membre du groupe.");
        Reunion active = reunionDAO.findActiveByGroupeId(groupeId);
        if (active != null || activeMeetings.values().stream().anyMatch(s -> s.getGroupeId() == groupeId)) {
            throw new IllegalStateException("Une reunion est deja active pour ce groupe.");
        }
        Reunion reunion = reunionDAO.create(groupeId, initiatorId, type);
        MeetingSession session = new MeetingSession(reunion.getId(), groupeId, initiatorId, reunion.getType());
        activeMeetings.put(reunion.getId(), session);
        joinMeeting(reunion.getId(), initiatorId, initiatorUsername, handler, 0, 0);
        return session;
    }

    public ParticipantInfo joinMeeting(int meetingId, int userId, String username, ClientHandler handler, int udpAudioPort, int udpVideoPort) throws SQLException {
        MeetingSession session = requireSession(meetingId);
        if (!membreDAO.isMember(session.getGroupeId(), userId)) throw new SecurityException("Utilisateur non membre du groupe.");
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
        ChatMessage info = new ChatMessage(MessageType.MEETING_INFO, "SERVER", to, null, "UDP relay ready");
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

    private MeetingSession requireSession(int meetingId) {
        MeetingSession session = activeMeetings.get(meetingId);
        if (session == null) throw new IllegalArgumentException("Reunion inactive ou introuvable.");
        return session;
    }

    private void notifyParticipants(MeetingSession session, ChatMessage msg, int exceptUserId) {
        for (ParticipantInfo participant : session.participants.values()) {
            if (participant.userId != exceptUserId && participant.handler != null) participant.handler.send(msg);
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
