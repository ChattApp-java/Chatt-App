package org.example.tpchatjavafx.dao;

import org.example.tpchatjavafx.model.Message;
import org.example.tpchatjavafx.model.Utilisateur;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MessageDAO {

    public Message create(Message message) throws SQLException {
        String sql = "INSERT INTO message (contenu, type, expediteur_id, destinataire_id, conversation_id) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, message.getContenu());
            stmt.setString(2, message.getType());
            stmt.setInt(3, message.getExpediteurId());
            if (message.getDestinataireId() != null) {
                stmt.setInt(4, message.getDestinataireId());
            } else {
                stmt.setNull(4, Types.INTEGER);
            }
            stmt.setInt(5, message.getConversationId());
            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    message.setId(rs.getInt(1));
                }
            }
        }
        return message;
    }

    public List<Message> getHistory(int conversationId) throws SQLException {
        List<Message> history = new ArrayList<>();
        String sql = "SELECT m.*, u.username FROM message m " +
                     "JOIN utilisateur u ON m.expediteur_id = u.id " +
                     "WHERE m.conversation_id = ? ORDER BY m.date_envoi ASC";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, conversationId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    history.add(mapResultSetToMessage(rs));
                }
            }
        }
        return history;
    }

    public List<Message> getUnreadMessages(int userId) throws SQLException {
        List<Message> unread = new ArrayList<>();
        String sql = "SELECT m.*, u.username FROM message m " +
                     "JOIN utilisateur u ON m.expediteur_id = u.id " +
                     "WHERE m.destinataire_id = ? AND m.est_lu = FALSE ORDER BY m.date_envoi ASC";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    unread.add(mapResultSetToMessage(rs));
                }
            }
        }
        return unread;
    }

    public void markAsRead(int messageId) throws SQLException {
        String sql = "UPDATE message SET est_lu = TRUE WHERE id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, messageId);
            stmt.executeUpdate();
        }
    }

    private Message mapResultSetToMessage(ResultSet rs) throws SQLException {
        Message m = new Message();
        m.setId(rs.getInt("id"));
        m.setContenu(rs.getString("contenu"));
        m.setDateEnvoi(rs.getTimestamp("date_envoi").toLocalDateTime());
        m.setType(rs.getString("type"));
        m.setExpediteurId(rs.getInt("expediteur_id"));
        m.setDestinataireId(rs.getInt("destinataire_id"));
        if (rs.wasNull()) {
            m.setDestinataireId(null);
        }
        m.setConversationId(rs.getInt("conversation_id"));
        m.setEstLu(rs.getBoolean("est_lu"));
        
        // Map transient field
        Utilisateur expediteur = new Utilisateur();
        expediteur.setId(m.getExpediteurId());
        expediteur.setUsername(rs.getString("username"));
        m.setExpediteur(expediteur);

        return m;
    }
}
