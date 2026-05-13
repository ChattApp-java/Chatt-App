package org.example.tpchatjavafx.dao;

import org.example.tpchatjavafx.model.Message;
import org.example.tpchatjavafx.model.Utilisateur;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MessageDAO {

    public Message create(Message message) throws SQLException {
        String sql = "INSERT INTO message (contenu, type, expediteur_id, destinataire_id, conversation_id, groupe_id, reunion_id) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bindCommonFields(stmt, message);
            if (message.getConversationId() > 0) stmt.setInt(5, message.getConversationId());
            else stmt.setNull(5, Types.INTEGER);
            setNullableInt(stmt, 6, message.getGroupeId());
            setNullableInt(stmt, 7, message.getReunionId());
            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) message.setId(rs.getInt(1));
            }
        }
        return message;
    }

    public Message saveGroupMessage(Message message) throws SQLException {
        message.setDestinataireId(null);
        if (message.getGroupeId() == null || message.getGroupeId() <= 0) {
            throw new SQLException("groupe_id requis pour un message de groupe.");
        }
        return create(message);
    }

    public List<Message> getHistory(int conversationId) throws SQLException {
        List<Message> history = new ArrayList<>();
        String sql = "SELECT m.*, u.username FROM message m " +
                "JOIN utilisateur u ON m.expediteur_id = u.id " +
                "WHERE m.conversation_id = ? AND m.groupe_id IS NULL ORDER BY m.dateEnvoi ASC";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, conversationId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) history.add(mapResultSetToMessage(rs));
            }
        }
        return history;
    }

    public List<Message> getMessagesByGroupeId(int groupeId) throws SQLException {
        return getMessagesByGroupeId(groupeId, 0, 0);
    }

    public List<Message> getMessagesByGroupeId(int groupeId, int limit, int offset) throws SQLException {
        List<Message> history = new ArrayList<>();
        String sql = "SELECT m.*, u.username FROM message m " +
                "JOIN utilisateur u ON m.expediteur_id = u.id " +
                "WHERE m.groupe_id = ? ORDER BY m.dateEnvoi ASC" +
                (limit > 0 ? " LIMIT ? OFFSET ?" : "");
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, groupeId);
            if (limit > 0) {
                stmt.setInt(2, limit);
                stmt.setInt(3, Math.max(offset, 0));
            }
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) history.add(mapResultSetToMessage(rs));
            }
        }
        return history;
    }

    public List<Message> getUnreadMessages(int userId) throws SQLException {
        List<Message> unread = new ArrayList<>();
        String sql = "SELECT m.*, u.username FROM message m " +
                "JOIN utilisateur u ON m.expediteur_id = u.id " +
                "WHERE m.destinataire_id = ? AND m.groupe_id IS NULL AND m.estLu = FALSE ORDER BY m.dateEnvoi ASC";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) unread.add(mapResultSetToMessage(rs));
            }
        }
        return unread;
    }

    public void markAsRead(int messageId) throws SQLException {
        String sql = "UPDATE message SET estLu = TRUE WHERE id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, messageId);
            stmt.executeUpdate();
        }
    }

    private void bindCommonFields(PreparedStatement stmt, Message message) throws SQLException {
        stmt.setString(1, message.getContenu());
        stmt.setString(2, message.getType());
        stmt.setInt(3, message.getExpediteurId());
        setNullableInt(stmt, 4, message.getDestinataireId());
    }

    private void setNullableInt(PreparedStatement stmt, int index, Integer value) throws SQLException {
        if (value == null) stmt.setNull(index, Types.INTEGER);
        else stmt.setInt(index, value);
    }

    private Message mapResultSetToMessage(ResultSet rs) throws SQLException {
        Message m = new Message();
        m.setId(rs.getInt("id"));
        m.setContenu(rs.getString("contenu"));
        Timestamp sentAt = rs.getTimestamp("dateEnvoi");
        if (sentAt != null) m.setDateEnvoi(sentAt.toLocalDateTime());
        m.setType(rs.getString("type"));
        m.setExpediteurId(rs.getInt("expediteur_id"));
        m.setDestinataireId(readNullableInt(rs, "destinataire_id"));
        Integer conversationId = readNullableInt(rs, "conversation_id");
        m.setConversationId(conversationId == null ? 0 : conversationId);
        m.setGroupeId(readNullableInt(rs, "groupe_id"));
        m.setReunionId(readNullableInt(rs, "reunion_id"));
        m.setEstLu(rs.getBoolean("estLu"));

        Utilisateur expediteur = new Utilisateur();
        expediteur.setId(m.getExpediteurId());
        expediteur.setUsername(rs.getString("username"));
        m.setExpediteur(expediteur);
        return m;
    }

    private Integer readNullableInt(ResultSet rs, String column) throws SQLException {
        int value = rs.getInt(column);
        return rs.wasNull() ? null : value;
    }
}
