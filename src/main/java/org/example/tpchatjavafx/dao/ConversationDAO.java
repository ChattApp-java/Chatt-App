package org.example.tpchatjavafx.dao;

import org.example.tpchatjavafx.model.Conversation;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ConversationDAO {

    public Conversation createConversation(int user1Id, int user2Id) throws SQLException {
        // Vérifier si la conversation existe déjà
        Conversation existing = findConversationBetween(user1Id, user2Id);
        if (existing != null) {
            return existing;
        }

        Connection conn = DatabaseConnection.getInstance().getConnection();
        conn.setAutoCommit(false);
        try {
            // Créer la conversation (type INDIVIDUEL par défaut)
            String sqlConv = "INSERT INTO conversation (type) VALUES (?)";
            int convId = -1;
            try (PreparedStatement stmt = conn.prepareStatement(sqlConv, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setString(1, Conversation.TYPE_INDIVIDUEL);
                stmt.executeUpdate();
                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) convId = rs.getInt(1);
                }
            }

            // Ajouter les participants
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
            conv.setType(Conversation.TYPE_INDIVIDUEL);
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
        String sql = "SELECT c.id, c.type, c.groupe_id FROM conversation c " +
                     "JOIN participant_conversation p1 ON c.id = p1.conversation_id " +
                     "JOIN participant_conversation p2 ON c.id = p2.conversation_id " +
                     "WHERE p1.utilisateur_id = ? AND p2.utilisateur_id = ? " +
                     "AND (c.type = 'INDIVIDUEL' OR c.type IS NULL)";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, user1Id);
            stmt.setInt(2, user2Id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapConversation(rs);
                }
            }
        }
        return null;
    }

    /**
     * P2 : Retourne les conversations liées à un groupe spécifique.
     */
    public List<Conversation> getConversationsByGroupeId(int groupeId) throws SQLException {
        List<Conversation> list = new ArrayList<>();
        String sql = "SELECT id, type, groupe_id FROM conversation WHERE groupe_id = ? ORDER BY id DESC";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, groupeId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) list.add(mapConversation(rs));
            }
        }
        return list;
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

    private Conversation mapConversation(ResultSet rs) throws SQLException {
        Conversation conv = new Conversation();
        conv.setId(rs.getInt("id"));
        try {
            String type = rs.getString("type");
            conv.setType(type != null ? type : Conversation.TYPE_INDIVIDUEL);
        } catch (SQLException ignored) { /* colonne absente dans ancien schéma */ }
        try {
            int gid = rs.getInt("groupe_id");
            if (!rs.wasNull()) conv.setGroupeId(gid);
        } catch (SQLException ignored) { /* colonne absente dans ancien schéma */ }
        return conv;
    }
}
