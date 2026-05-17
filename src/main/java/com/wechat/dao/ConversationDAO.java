package com.wechat.dao;

import com.wechat.model.Conversation;
import com.wechat.model.Conversation.ConversationType;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class ConversationDAO {

    private final Connection connection;

    public ConversationDAO() {
        this.connection = DatabaseConnection.getInstance().getConnection();
    }

    public Conversation save(Conversation conversation) throws SQLException {
        String sql = "INSERT INTO conversations (type, name, participant_ids, last_message_id, last_message_preview, last_message_time, unread_count, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, conversation.getType().name());
            stmt.setString(2, conversation.getName());
            stmt.setString(3, conversation.getParticipantIds().stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(",")));
            stmt.setObject(4, conversation.getLastMessageId());
            stmt.setString(5, conversation.getLastMessagePreview());
            stmt.setTimestamp(6, conversation.getLastMessageTime() != null ?
                    Timestamp.valueOf(conversation.getLastMessageTime()) : null);
            stmt.setInt(7, conversation.getUnreadCount());
            stmt.setTimestamp(8, Timestamp.valueOf(conversation.getCreatedAt()));

            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    conversation.setId(rs.getLong(1));
                }
            }
        }
        return conversation;
    }

    public Conversation createPrivate(Long userId1, Long userId2) throws SQLException {
        // Vérifie si une conversation privée existe déjà
        Optional<Conversation> existing = findPrivateBetweenUsers(userId1, userId2);
        if (existing.isPresent()) {
            return existing.get();
        }

        Conversation conv = new Conversation();
        conv.setType(ConversationType.PRIVATE);
        conv.setName(""); // Les noms privés sont gérés côté client
        conv.addParticipant(userId1);
        conv.addParticipant(userId2);
        return save(conv);
    }

    public Conversation createGroup(String name, List<Long> memberIds) throws SQLException {
        if (memberIds == null || memberIds.size() < 2) {
            throw new IllegalArgumentException("Un groupe doit avoir au moins 2 membres");
        }

        Conversation conv = new Conversation();
        conv.setType(ConversationType.GROUP);
        conv.setName(name);
        for (Long memberId : memberIds) {
            conv.addParticipant(memberId);
        }
        return save(conv);
    }

    public Optional<Conversation> findById(Long id) throws SQLException {
        String sql = "SELECT * FROM conversations WHERE id = ?";
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

    public List<Conversation> findByUser(Long userId) throws SQLException {
        List<Conversation> conversations = new ArrayList<>();
        String sql = "SELECT * FROM conversations WHERE FIND_IN_SET(?, participant_ids) ORDER BY last_message_time DESC";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    conversations.add(mapResultSet(rs));
                }
            }
        }
        return conversations;
    }

    public Optional<Conversation> findPrivateBetweenUsers(Long userId1, Long userId2) throws SQLException {
        String sql = "SELECT * FROM conversations WHERE type = 'PRIVATE' AND " +
                "(FIND_IN_SET(?, participant_ids) AND FIND_IN_SET(?, participant_ids)) AND " +
                "LENGTH(participant_ids) - LENGTH(REPLACE(participant_ids, ',', '')) = 1";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, userId1);
            stmt.setLong(2, userId2);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSet(rs));
                }
            }
        }
        return Optional.empty();
    }

    public void updateLastMessage(Long conversationId, Long messageId, String preview, LocalDateTime time) throws SQLException {
        String sql = "UPDATE conversations SET last_message_id = ?, last_message_preview = ?, last_message_time = ? WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setObject(1, messageId);
            stmt.setString(2, preview);
            stmt.setTimestamp(3, Timestamp.valueOf(time));
            stmt.setLong(4, conversationId);
            stmt.executeUpdate();
        }
    }

    public void updateUnreadCount(Long conversationId, int count) throws SQLException {
        String sql = "UPDATE conversations SET unread_count = ? WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, count);
            stmt.setLong(2, conversationId);
            stmt.executeUpdate();
        }
    }

    public void delete(Long id) throws SQLException {
        String sql = "DELETE FROM conversations WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, id);
            stmt.executeUpdate();
        }
    }

    private Conversation mapResultSet(ResultSet rs) throws SQLException {
        Conversation conversation = new Conversation();
        conversation.setId(rs.getLong("id"));
        conversation.setType(ConversationType.valueOf(rs.getString("type")));
        conversation.setName(rs.getString("name"));

        String participantStr = rs.getString("participant_ids");
        if (participantStr != null && !participantStr.isEmpty()) {
            List<Long> ids = Arrays.stream(participantStr.split(","))
                    .map(Long::parseLong)
                    .collect(Collectors.toList());
            conversation.setParticipantIds(ids);
        }

        conversation.setLastMessageId(rs.getLong("last_message_id"));
        if (rs.wasNull()) conversation.setLastMessageId(null);

        conversation.setLastMessagePreview(rs.getString("last_message_preview"));

        Timestamp lastMsgTime = rs.getTimestamp("last_message_time");
        if (lastMsgTime != null) {
            conversation.setLastMessageTime(lastMsgTime.toLocalDateTime());
        }

        conversation.setUnreadCount(rs.getInt("unread_count"));
        conversation.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        return conversation;
    }
}