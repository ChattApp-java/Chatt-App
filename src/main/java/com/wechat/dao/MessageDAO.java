package com.wechat.dao;

import com.wechat.model.Message;
import com.wechat.model.Message.MessageType;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MessageDAO {

    private final Connection connection;

    public MessageDAO() {
        this.connection = DatabaseConnection.getInstance().getConnection();
    }

    public Message save(Message message) throws SQLException {
        String sql = "INSERT INTO messages (conversation_id, sender_id, content, type, timestamp, is_read, file_path) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setLong(1, message.getConversationId());
            stmt.setLong(2, message.getSenderId());
            stmt.setString(3, message.getContent());
            stmt.setString(4, message.getType().name());
            stmt.setTimestamp(5, Timestamp.valueOf(message.getTimestamp()));
            stmt.setBoolean(6, message.isRead());
            stmt.setString(7, message.getFilePath());

            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    message.setId(rs.getLong(1));
                }
            }
        }
        return message;
    }

    public Optional<Message> findById(Long id) throws SQLException {
        String sql = "SELECT * FROM messages WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSet(rs));
                }
            }
        }
        return Optional.empty();
    }

    public List<Message> findByConversation(Long conversationId) throws SQLException {
        List<Message> messages = new ArrayList<>();
        String sql = "SELECT * FROM messages WHERE conversation_id = ? ORDER BY timestamp ASC";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, conversationId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    messages.add(mapResultSet(rs));
                }
            }
        }
        return messages;
    }

    public List<Message> findByConversationUnread(Long conversationId, Long userId) throws SQLException {
        List<Message> messages = new ArrayList<>();
        String sql = "SELECT * FROM messages WHERE conversation_id = ? AND sender_id != ? AND is_read = false ORDER BY timestamp ASC";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, conversationId);
            stmt.setLong(2, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    messages.add(mapResultSet(rs));
                }
            }
        }
        return messages;
    }

    public int getUnreadCount(Long conversationId, Long userId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM messages WHERE conversation_id = ? AND sender_id != ? AND is_read = false";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, conversationId);
            stmt.setLong(2, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    public void markAsRead(Long messageId) throws SQLException {
        String sql = "UPDATE messages SET is_read = true WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, messageId);
            stmt.executeUpdate();
        }
    }

    public void markConversationAsRead(Long conversationId, Long userId) throws SQLException {
        String sql = "UPDATE messages SET is_read = true WHERE conversation_id = ? AND sender_id != ? AND is_read = false";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, conversationId);
            stmt.setLong(2, userId);
            stmt.executeUpdate();
        }
    }

    public void delete(Long id) throws SQLException {
        String sql = "DELETE FROM messages WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, id);
            stmt.executeUpdate();
        }
    }

    private Message mapResultSet(ResultSet rs) throws SQLException {
        Message message = new Message();
        message.setId(rs.getLong("id"));
        message.setConversationId(rs.getLong("conversation_id"));
        message.setSenderId(rs.getLong("sender_id"));
        message.setContent(rs.getString("content"));
        message.setType(MessageType.valueOf(rs.getString("type")));
        message.setTimestamp(rs.getTimestamp("timestamp").toLocalDateTime());
        message.setRead(rs.getBoolean("is_read"));
        message.setFilePath(rs.getString("file_path"));
        return message;
    }
}