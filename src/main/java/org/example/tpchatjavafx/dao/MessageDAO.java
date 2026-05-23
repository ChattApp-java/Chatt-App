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

    public List<Message> getHistoryForUser(int conversationId, int utilisateurId) throws SQLException {
        List<Message> history = new ArrayList<>();
        String sql = "SELECT m.*, u.username FROM message m " +
                "JOIN utilisateur u ON m.expediteur_id = u.id " +
                "LEFT JOIN conversation_clear_state ccs ON ccs.conversation_id = m.conversation_id " +
                "AND ccs.utilisateur_id = ? " +
                "LEFT JOIN message_clear_state mcs ON mcs.message_id = m.id AND mcs.utilisateur_id = ? " +
                "WHERE m.conversation_id = ? AND m.groupe_id IS NULL " +
                "AND mcs.message_id IS NULL " +
                "AND (ccs.cleared_at IS NULL OR m.dateEnvoi > ccs.cleared_at) " +
                "ORDER BY m.dateEnvoi ASC";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, utilisateurId);
            stmt.setInt(2, utilisateurId);
            stmt.setInt(3, conversationId);
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

    public List<Message> getMessagesByGroupeIdForUser(int groupeId, int utilisateurId) throws SQLException {
        List<Message> history = new ArrayList<>();
        String sql = "SELECT m.*, u.username FROM message m " +
                "JOIN utilisateur u ON m.expediteur_id = u.id " +
                "LEFT JOIN groupe_clear_state gcs ON gcs.groupe_id = m.groupe_id " +
                "AND gcs.utilisateur_id = ? " +
                "LEFT JOIN message_clear_state mcs ON mcs.message_id = m.id AND mcs.utilisateur_id = ? " +
                "WHERE m.groupe_id = ? " +
                "AND mcs.message_id IS NULL " +
                "AND (gcs.cleared_at IS NULL OR m.dateEnvoi > gcs.cleared_at) " +
                "ORDER BY m.dateEnvoi ASC";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, utilisateurId);
            stmt.setInt(2, utilisateurId);
            stmt.setInt(3, groupeId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) history.add(mapResultSetToMessage(rs));
            }
        }
        return history;
    }

    public void markConversationCleared(int utilisateurId, int conversationId) throws SQLException {
        String sql = "INSERT INTO conversation_clear_state (utilisateur_id, conversation_id, cleared_at) " +
                "VALUES (?, ?, CURRENT_TIMESTAMP) " +
                "ON DUPLICATE KEY UPDATE cleared_at = CURRENT_TIMESTAMP";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, utilisateurId);
            stmt.setInt(2, conversationId);
            stmt.executeUpdate();
        }
    }

    public void markGroupCleared(int utilisateurId, int groupeId) throws SQLException {
        String sql = "INSERT INTO groupe_clear_state (utilisateur_id, groupe_id, cleared_at) " +
                "VALUES (?, ?, CURRENT_TIMESTAMP) " +
                "ON DUPLICATE KEY UPDATE cleared_at = CURRENT_TIMESTAMP";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, utilisateurId);
            stmt.setInt(2, groupeId);
            stmt.executeUpdate();
        }
    }

    public void deleteConversationMessages(int conversationId) throws SQLException {
        String sql = "DELETE FROM message WHERE conversation_id = ? AND groupe_id IS NULL";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, conversationId);
            stmt.executeUpdate();
        }
    }

    public void deleteGroupMessages(int groupeId) throws SQLException {
        String sql = "DELETE FROM message WHERE groupe_id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, groupeId);
            stmt.executeUpdate();
        }
    }

    public Message findById(int messageId) throws SQLException {
        String sql = "SELECT m.*, u.username FROM message m " +
                "JOIN utilisateur u ON m.expediteur_id = u.id WHERE m.id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, messageId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return mapResultSetToMessage(rs);
            }
        }
        return null;
    }

    public void markMessageCleared(int utilisateurId, int messageId) throws SQLException {
        String sql = "INSERT IGNORE INTO message_clear_state (utilisateur_id, message_id) " +
                "SELECT ?, m.id FROM message m " +
                "LEFT JOIN groupe_membre gm ON gm.groupe_id = m.groupe_id AND gm.utilisateur_id = ? " +
                "WHERE m.id = ? AND (m.expediteur_id = ? OR m.destinataire_id = ? OR gm.utilisateur_id IS NOT NULL)";
        String auditSql = "INSERT IGNORE INTO message_delete_for_me (utilisateur_id, message_id) " +
                "SELECT ?, m.id FROM message m " +
                "LEFT JOIN groupe_membre gm ON gm.groupe_id = m.groupe_id AND gm.utilisateur_id = ? " +
                "WHERE m.id = ? AND (m.expediteur_id = ? OR m.destinataire_id = ? OR gm.utilisateur_id IS NOT NULL)";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, utilisateurId);
            stmt.setInt(2, utilisateurId);
            stmt.setInt(3, messageId);
            stmt.setInt(4, utilisateurId);
            stmt.setInt(5, utilisateurId);
            stmt.executeUpdate();
            try (PreparedStatement auditStmt = conn.prepareStatement(auditSql)) {
                auditStmt.setInt(1, utilisateurId);
                auditStmt.setInt(2, utilisateurId);
                auditStmt.setInt(3, messageId);
                auditStmt.setInt(4, utilisateurId);
                auditStmt.setInt(5, utilisateurId);
                auditStmt.executeUpdate();
            } catch (SQLException e) {
                if (!isMissingTable(e)) throw e;
            }
        }
    }

    public boolean deleteMessageForEveryone(int messageId, int utilisateurId) throws SQLException {
        String auditSql = "INSERT INTO message_delete_for_everyone " +
                "(message_id, deleted_by_id, conversation_id, groupe_id, original_type) " +
                "SELECT m.id, ?, m.conversation_id, m.groupe_id, m.type FROM message m " +
                "LEFT JOIN groupe_membre gm ON gm.groupe_id = m.groupe_id AND gm.utilisateur_id = ? " +
                "WHERE m.id = ? AND (m.expediteur_id = ? OR m.destinataire_id = ? OR gm.utilisateur_id IS NOT NULL)";
        String sql = "DELETE m FROM message m " +
                "LEFT JOIN groupe_membre gm ON gm.groupe_id = m.groupe_id AND gm.utilisateur_id = ? " +
                "WHERE m.id = ? AND (m.expediteur_id = ? OR m.destinataire_id = ? OR gm.utilisateur_id IS NOT NULL)";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement auditStmt = conn.prepareStatement(auditSql);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            auditStmt.setInt(1, utilisateurId);
            auditStmt.setInt(2, utilisateurId);
            auditStmt.setInt(3, messageId);
            auditStmt.setInt(4, utilisateurId);
            auditStmt.setInt(5, utilisateurId);
            try {
                boolean allowed = auditStmt.executeUpdate() > 0;
                if (!allowed) return false;
            } catch (SQLException e) {
                if (!isMissingTable(e)) throw e;
            }

            return hardDelete(messageId, utilisateurId);
        }
    }

    public boolean hardDelete(int messageId, int userId) throws SQLException {
        String sql = "DELETE m FROM message m " +
                "LEFT JOIN groupe_membre gm ON gm.groupe_id = m.groupe_id " +
                "AND gm.utilisateur_id = ? AND gm.role = 'ADMIN' " +
                "LEFT JOIN groupe g ON g.id = m.groupe_id AND g.createur_id = ? " +
                "WHERE m.id = ? AND (m.expediteur_id = ? OR gm.utilisateur_id IS NOT NULL OR g.createur_id IS NOT NULL)";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, userId);
            stmt.setInt(3, messageId);
            stmt.setInt(4, userId);
            return stmt.executeUpdate() > 0;
        }
    }

    public void saveMessageSelection(int utilisateurId, List<Integer> messageIds, String selectionGroup) throws SQLException {
        if (messageIds == null || messageIds.isEmpty()) return;
        String group = (selectionGroup == null || selectionGroup.isBlank())
                ? String.valueOf(System.currentTimeMillis())
                : selectionGroup;
        String sql = "INSERT INTO message_selection (utilisateur_id, message_id, selection_group) " +
                "SELECT ?, m.id, ? FROM message m " +
                "LEFT JOIN groupe_membre gm ON gm.groupe_id = m.groupe_id AND gm.utilisateur_id = ? " +
                "WHERE m.id = ? AND (m.expediteur_id = ? OR m.destinataire_id = ? OR gm.utilisateur_id IS NOT NULL)";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (Integer messageId : messageIds) {
                if (messageId == null || messageId <= 0) continue;
                stmt.setInt(1, utilisateurId);
                stmt.setString(2, group);
                stmt.setInt(3, utilisateurId);
                stmt.setInt(4, messageId);
                stmt.setInt(5, utilisateurId);
                stmt.setInt(6, utilisateurId);
                try {
                    stmt.addBatch();
                } catch (SQLException e) {
                    if (!isMissingTable(e)) throw e;
                }
            }
            try {
                stmt.executeBatch();
            } catch (SQLException e) {
                if (!isMissingTable(e)) throw e;
            }
        }
    }

    private boolean isMissingTable(SQLException e) {
        return e.getErrorCode() == 1146 || "42S02".equals(e.getSQLState());
    }

    public List<Message> getUnreadMessages(int userId) throws SQLException {
        List<Message> unread = new ArrayList<>();
        String sql = "SELECT m.*, u.username FROM message m " +
                "JOIN utilisateur u ON m.expediteur_id = u.id " +
                "LEFT JOIN conversation_clear_state ccs ON ccs.conversation_id = m.conversation_id " +
                "AND ccs.utilisateur_id = ? " +
                "LEFT JOIN message_clear_state mcs ON mcs.message_id = m.id AND mcs.utilisateur_id = ? " +
                "WHERE m.destinataire_id = ? AND m.groupe_id IS NULL AND m.estLu = FALSE " +
                "AND mcs.message_id IS NULL " +
                "AND (ccs.cleared_at IS NULL OR m.dateEnvoi > ccs.cleared_at) " +
                "ORDER BY m.dateEnvoi ASC";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, userId);
            stmt.setInt(3, userId);
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
        try {
            m.setDeleted(rs.getBoolean("is_deleted"));
            Timestamp deletedAt = rs.getTimestamp("deleted_at");
            if (deletedAt != null) m.setDeletedAt(deletedAt.toLocalDateTime());
            m.setDeletedBy(readNullableInt(rs, "deleted_by"));
        } catch (SQLException ignored) {

        }

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
