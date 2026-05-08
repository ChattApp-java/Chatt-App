package org.example.tpchatjavafx.dao;

import org.example.tpchatjavafx.model.Conversation;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ConversationDAO {

    public Conversation createConversation(int user1Id, int user2Id) throws SQLException {
        // Check si la conversation est deja existe
        Conversation existing = findConversationBetween(user1Id, user2Id);
        if (existing != null) {
            return existing;
        }

        Connection conn = DatabaseConnection.getInstance().getConnection();
        conn.setAutoCommit(false);
        try {
            // Create conversation
            String sqlConv = "INSERT INTO conversation () VALUES ()"; // Values default
            int convId = -1;
            try (PreparedStatement stmt = conn.prepareStatement(sqlConv, Statement.RETURN_GENERATED_KEYS)) {
                stmt.executeUpdate();
                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        convId = rs.getInt(1);
                    }
                }
            }

            // Add participants
            String sqlPart = "INSERT INTO participant_conversation (conversation_id, utilisateur_id) VALUES (?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(sqlPart)) {
                stmt.setInt(1, convId);
                stmt.setInt(2, user1Id);
                stmt.addBatch();
                stmt.setInt(1, convId);
                stmt.setInt(2, user2Id);
                stmt.addBatch();
                stmt.executeBatch();
            }

            conn.commit();
            Conversation conv = new Conversation();
            conv.setId(convId);
            return conv;
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
            conn.close();
        }
    }

    public Conversation findConversationBetween(int user1Id, int user2Id) throws SQLException {
        String sql = "SELECT c.id FROM conversation c " +
                     "JOIN participant_conversation p1 ON c.id = p1.conversation_id " +
                     "JOIN participant_conversation p2 ON c.id = p2.conversation_id " +
                     "WHERE p1.utilisateur_id = ? AND p2.utilisateur_id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, user1Id);
            stmt.setInt(2, user2Id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Conversation conv = new Conversation();
                    conv.setId(rs.getInt("id"));
                    return conv;
                }
            }
        }
        return null;
    }

    public List<Integer> getParticipants(int conversationId) throws SQLException {
        List<Integer> parts = new ArrayList<>();
        String sql = "SELECT utilisateur_id FROM participant_conversation WHERE conversation_id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, conversationId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    parts.add(rs.getInt("utilisateur_id"));
                }
            }
        }
        return parts;
    }
}
