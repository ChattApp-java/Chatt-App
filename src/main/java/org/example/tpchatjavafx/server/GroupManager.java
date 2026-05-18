package org.example.tpchatjavafx.server;

import org.example.tpchatjavafx.client.model.ChatMessage;
import org.example.tpchatjavafx.common.MessageType;
import org.example.tpchatjavafx.dao.GroupeDAO;
import org.example.tpchatjavafx.dao.GroupeMembreDAO;
import org.example.tpchatjavafx.dao.FichierMediaDAO;
import org.example.tpchatjavafx.dao.MessageDAO;
import org.example.tpchatjavafx.dao.UtilisateurDAO;
import org.example.tpchatjavafx.model.Groupe;
import org.example.tpchatjavafx.model.GroupeMembre;
import org.example.tpchatjavafx.model.Message;
import org.example.tpchatjavafx.model.Utilisateur;
import org.example.tpchatjavafx.model.FichierMedia;
import org.example.tpchatjavafx.model.Vocal;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class GroupManager {
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private final GroupeDAO groupeDAO;
    private final GroupeMembreDAO membreDAO;
    private final MessageDAO messageDAO;
    private final UtilisateurDAO utilisateurDAO;
    private final FichierMediaDAO fichierMediaDAO;

    public GroupManager() {
        this(new GroupeDAO(), new GroupeMembreDAO(), new MessageDAO(), new UtilisateurDAO(), new FichierMediaDAO());
    }

    GroupManager(GroupeDAO groupeDAO, GroupeMembreDAO membreDAO, MessageDAO messageDAO, UtilisateurDAO utilisateurDAO, FichierMediaDAO fichierMediaDAO) {
        this.groupeDAO = groupeDAO;
        this.membreDAO = membreDAO;
        this.messageDAO = messageDAO;
        this.utilisateurDAO = utilisateurDAO;
        this.fichierMediaDAO = fichierMediaDAO;
    }

    public Groupe createGroup(String nom, String description, int createurId, List<Integer> memberIds) throws SQLException {
        if (nom == null || nom.isBlank()) throw new IllegalArgumentException("Le nom du groupe est obligatoire.");
        if (utilisateurDAO.findById(createurId) == null) throw new IllegalArgumentException("Createur introuvable.");

        Set<Integer> uniqueMembers = new LinkedHashSet<>();
        uniqueMembers.add(createurId);
        if (memberIds != null) uniqueMembers.addAll(memberIds);
        for (Integer memberId : uniqueMembers) {
            if (memberId == null || utilisateurDAO.findById(memberId) == null) {
                throw new IllegalArgumentException("Membre invalide: " + memberId);
            }
        }

        Groupe groupe = groupeDAO.create(nom.trim(), description, createurId);
        for (Integer memberId : uniqueMembers) {
            String role = memberId == createurId ? GroupeMembre.ROLE_ADMIN : GroupeMembre.ROLE_MEMBRE;
            membreDAO.addMember(groupe.getId(), memberId, role);
        }
        return groupe;
    }

    public void addMember(int groupeId, int requesterId, int newMemberId) throws SQLException {
        requireAdmin(groupeId, requesterId);
        if (utilisateurDAO.findById(newMemberId) == null) throw new IllegalArgumentException("Utilisateur introuvable.");
        membreDAO.addMember(groupeId, newMemberId, GroupeMembre.ROLE_MEMBRE);
    }

    public void removeMember(int groupeId, int requesterId, int memberId) throws SQLException {
        Groupe groupe = requireGroup(groupeId);
        if (requesterId != memberId && !isAdmin(groupeId, requesterId)) {
            throw new SecurityException("Seul un admin peut retirer un autre membre.");
        }
        if (groupe.getCreateurId() == memberId && requesterId != memberId) {
            throw new SecurityException("Le createur ne peut pas etre retire par un autre membre.");
        }
        membreDAO.removeMember(groupeId, memberId);
    }

    public void deleteGroup(int groupeId, int requesterId) throws SQLException {
        Groupe groupe = requireGroup(groupeId);
        if (groupe.getCreateurId() != requesterId && !isAdmin(groupeId, requesterId)) {
            throw new SecurityException("Seul un admin peut supprimer le groupe.");
        }
        ChatMessage notification = new ChatMessage(MessageType.GROUP_DELETE, "SERVER", null, null, String.valueOf(groupeId));
        notification.setGroupId(groupeId);
        ChatServer.broadcastToGroup(groupeId, notification);
        groupeDAO.delete(groupeId);
    }

    public Message saveAndBroadcastGroupMessage(ChatMessage inbound, int senderId, String senderUsername) throws SQLException, java.io.IOException {
        int groupeId = inbound.getGroupId();
        requireMember(groupeId, senderId);
        Message message = new Message();
        message.setContenu(inbound.getContent());
        message.setType(inbound.getType().name());
        message.setExpediteurId(senderId);
        message.setGroupeId(groupeId);
        message.setDateEnvoi(LocalDateTime.now());
        Message saved = messageDAO.saveGroupMessage(message);
        if (inbound.getBinaryData() != null && inbound.getBinaryData().length > 0) {
            saveMedia(saved.getId(), inbound.getContent(), inbound.getType(), inbound.getBinaryData());
        }

        ChatMessage outbound = new ChatMessage(inbound.getType(), senderUsername, null, null, inbound.getContent());
        outbound.setMessageId(saved.getId());
        outbound.setGroupId(groupeId);
        outbound.setBinaryData(inbound.getBinaryData());
        outbound.setTimestamp(LocalDateTime.now().format(TIME_FORMATTER));
        ChatServer.broadcastToGroup(groupeId, outbound);
        return saved;
    }

    public List<Message> getGroupHistory(int groupeId, int requesterId) throws SQLException {
        requireMember(groupeId, requesterId);
        return messageDAO.getMessagesByGroupeIdForUser(groupeId, requesterId);
    }

    public List<Groupe> getGroupsForUser(int userId) throws SQLException {
        return groupeDAO.findByUtilisateurId(userId);
    }

    public void leaveGroup(int groupeId, int userId) throws SQLException {
        requireMember(groupeId, userId);
        membreDAO.removeMember(groupeId, userId);
    }

    public List<Integer> getGroupIdsForUser(int userId) throws SQLException {
        return membreDAO.getGroupIdsForUser(userId);
    }

    public List<Utilisateur> getMembers(int groupeId, int requesterId) throws SQLException {
        requireMember(groupeId, requesterId);
        return membreDAO.getMembers(groupeId);
    }

    public String getRole(int groupeId, int userId) throws SQLException {
        Groupe groupe = groupeDAO.findById(groupeId);
        if (groupe != null && groupe.getCreateurId() == userId) return GroupeMembre.ROLE_ADMIN;
        String role = membreDAO.getRole(groupeId, userId);
        return role == null ? GroupeMembre.ROLE_MEMBRE : role;
    }

    public List<Integer> getMemberIds(int groupeId) throws SQLException {
        return membreDAO.getMemberIds(groupeId);
    }

    public boolean isMember(int groupeId, int userId) throws SQLException {
        return membreDAO.isMember(groupeId, userId);
    }

    public boolean isAdmin(int groupeId, int userId) throws SQLException {
        Groupe groupe = groupeDAO.findById(groupeId);
        if (groupe != null && groupe.getCreateurId() == userId) return true;
        return GroupeMembre.ROLE_ADMIN.equals(membreDAO.getRole(groupeId, userId));
    }

    public String serializeGroups(List<Groupe> groupes, java.util.function.Predicate<Integer> hasMeeting) {
        return groupes.stream()
                .map(groupe -> groupe.getId() + ":" + safe(groupe.getNom()) + ":" + (hasMeeting.test(groupe.getId()) ? "1" : "0"))
                .collect(Collectors.joining(","));
    }

    public String serializeMessages(List<Message> messages) {
        List<String> parts = new ArrayList<>();
        for (Message message : messages) {
            String sender = message.getExpediteur() == null ? String.valueOf(message.getExpediteurId()) : message.getExpediteur().getUsername();
            String time = message.getDateEnvoi() == null ? "" : message.getDateEnvoi().format(TIME_FORMATTER);
            parts.add(sender + ":::" + safeMessage(message.getContenu()) + ":::" + time + ":::" + message.getType());
        }
        return String.join(";;;", parts);
    }

    private void requireAdmin(int groupeId, int requesterId) throws SQLException {
        requireGroup(groupeId);
        if (!isAdmin(groupeId, requesterId)) throw new SecurityException("Permission admin requise.");
    }

    private void requireMember(int groupeId, int userId) throws SQLException {
        Groupe groupe = requireGroup(groupeId);
        if (groupe.getCreateurId() == userId) return;
        if (!membreDAO.isMember(groupeId, userId)) throw new SecurityException("Utilisateur non membre du groupe.");
    }

    private Groupe requireGroup(int groupeId) throws SQLException {
        Groupe groupe = groupeDAO.findById(groupeId);
        if (groupe == null) throw new IllegalArgumentException("Groupe introuvable.");
        return groupe;
    }

    private String safe(String value) {
        return value == null ? "" : value.replace(":", " ").replace(",", " ");
    }

    private String safeMessage(String value) {
        return value == null ? "" : value.replace(":::", " ").replace(";;;", " ");
    }

    private void saveMedia(int messageId, String originalName, MessageType type, byte[] data) throws SQLException, java.io.IOException {
        java.io.File dir = new java.io.File("server_uploads");
        if (!dir.exists()) dir.mkdirs();
        String safeName = System.currentTimeMillis() + "_" + (originalName == null ? "file" : originalName).replaceAll("[^a-zA-Z0-9._-]", "_");
        java.io.File dest = new java.io.File(dir, safeName);
        try (java.io.FileOutputStream out = new java.io.FileOutputStream(dest)) {
            out.write(data);
        }

        FichierMedia media = type == MessageType.GROUP_AUDIO ? new Vocal() : new FichierMedia();
        if (media instanceof Vocal vocal) vocal.setDuree(0);
        media.setMessageId(messageId);
        media.setNomFichier(originalName);
        media.setCheminAcces(dest.getAbsolutePath());
        media.setTaille(data.length);
        media.setType(type == MessageType.GROUP_AUDIO ? "AUDIO" : type == MessageType.GROUP_IMAGE ? "IMAGE" : "FILE");
        fichierMediaDAO.create(media);
    }
}
