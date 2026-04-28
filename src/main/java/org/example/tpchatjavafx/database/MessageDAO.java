package org.example.tpchatjavafx.database;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Accès base de données pour la table {@code messages}.
 */
public class MessageDAO {

    /** Sauvegarde un message entre deux utilisateurs (par IDs). */
    public boolean saveMessage(int senderId, int receiverId, String content) {
        String sql = "INSERT INTO messages (sender_id, receiver_id, content) VALUES (?, ?, ?)";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, senderId);
            ps.setInt(2, receiverId);
            ps.setString(3, content);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("[MessageDAO] saveMessage: " + e.getMessage());
            return false;
        }
    }

    /** Récupère les 50 derniers messages entre deux utilisateurs. */
    public List<String[]> getConversation(int userId1, int userId2) {
        String sql = """
            SELECT u.username AS sender, m.content, m.sent_at
            FROM messages m
            JOIN users u ON u.id = m.sender_id
            WHERE (m.sender_id=? AND m.receiver_id=?)
               OR (m.sender_id=? AND m.receiver_id=?)
            ORDER BY m.sent_at DESC
            LIMIT 50
        """;
        List<String[]> list = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId1); ps.setInt(2, userId2);
            ps.setInt(3, userId2); ps.setInt(4, userId1);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new String[]{
                    rs.getString("sender"),
                    rs.getString("content"),
                    rs.getString("sent_at")
                });
            }
        } catch (SQLException e) {
            System.err.println("[MessageDAO] getConversation: " + e.getMessage());
        }
        return list;
    }
}
