package org.example.tpchatjavafx.server;

import org.example.tpchatjavafx.client.model.ChatMessage;
import org.example.tpchatjavafx.common.MessageType;
import org.example.tpchatjavafx.dao.GroupeDAO;
import org.example.tpchatjavafx.dao.GroupeMembreDAO;
import org.example.tpchatjavafx.dao.MessageDAO;
import org.example.tpchatjavafx.dao.UtilisateurDAO;
import org.example.tpchatjavafx.model.Groupe;
import org.example.tpchatjavafx.model.GroupeMembre;
import org.example.tpchatjavafx.model.Message;
import org.example.tpchatjavafx.model.Utilisateur;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class GroupManager {
    private final GroupeDAO groupeDAO;
    private final GroupeMembreDAO membreDAO;
    private final MessageDAO messageDAO;
    private final UtilisateurDAO utilisateurDAO;

    public GroupManager() {
        this(new GroupeDAO(), new GroupeMembreDAO(), new MessageDAO(), new UtilisateurDAO());
    }

    GroupManager(GroupeDAO groupeDAO, GroupeMembreDAO membreDAO, MessageDAO messageDAO, UtilisateurDAO utilisateurDAO) {
        this.groupeDAO = groupeDAO;
        this.membreDAO = membreDAO;
        this.messageDAO = messageDAO;
        this.utilisateurDAO = utilisateurDAO;
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

    public Message saveAndBroadcastGroupMessage(int groupeId, int senderId, String senderUsername, String content) throws SQLException {
        requireMember(groupeId, senderId);
        Message message = new Message();
        message.setContenu(content);
        message.setType(MessageType.GROUP_MESSAGE.name());
        message.setExpediteurId(senderId);
        message.setGroupeId(groupeId);
        message.setDateEnvoi(LocalDateTime.now());
        Message saved = messageDAO.saveGroupMessage(message);

        ChatMessage outbound = new ChatMessage(MessageType.GROUP_MESSAGE, senderUsername, null, null, content);
        outbound.setMessageId(saved.getId());
        outbound.setGroupId(groupeId);
        ChatServer.broadcastToGroup(groupeId, outbound);
        return saved;
    }

    public List<Message> getGroupHistory(int groupeId, int requesterId) throws SQLException {
        requireMember(groupeId, requesterId);
        return messageDAO.getMessagesByGroupeId(groupeId);
    }

    public List<Groupe> getGroupsForUser(int userId) throws SQLException {
        return groupeDAO.findByUtilisateurId(userId);
    }

    public List<Integer> getGroupIdsForUser(int userId) throws SQLException {
        return membreDAO.getGroupIdsForUser(userId);
    }

    public List<Utilisateur> getMembers(int groupeId, int requesterId) throws SQLException {
        requireMember(groupeId, requesterId);
        return membreDAO.getMembers(groupeId);
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

    public String serializeGroups(List<Groupe> groupes) {
        return groupes.stream().map(Groupe::toString).collect(Collectors.joining(","));
    }

    public String serializeMessages(List<Message> messages) {
        List<String> parts = new ArrayList<>();
        for (Message message : messages) {
            String sender = message.getExpediteur() == null ? String.valueOf(message.getExpediteurId()) : message.getExpediteur().getUsername();
            parts.add(message.getId() + ":" + sender + ":" + safe(message.getContenu()));
        }
        return String.join(",", parts);
    }

    private void requireAdmin(int groupeId, int requesterId) throws SQLException {
        requireGroup(groupeId);
        if (!isAdmin(groupeId, requesterId)) throw new SecurityException("Permission admin requise.");
    }

    private void requireMember(int groupeId, int userId) throws SQLException {
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
}
