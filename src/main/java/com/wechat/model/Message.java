package com.wechat.model;

import java.time.LocalDateTime;
import java.util.Objects;

public class Message {
    private Long id;
    private Long conversationId;
    private Long senderId;
    private String content;
    private MessageType type;
    private LocalDateTime timestamp;
    private boolean isRead;
    private String filePath;

    public enum MessageType {
        TEXT, IMAGE, FILE, AUDIO, VIDEO, SYSTEM
    }

    public Message() {
        this.type = MessageType.TEXT;
        this.timestamp = LocalDateTime.now();
        this.isRead = false;
    }

    public Message(Long conversationId, Long senderId, String content, MessageType type) {
        this();
        this.conversationId = conversationId;
        this.senderId = senderId;
        this.content = content;
        this.type = type;
    }

    // Getters
    public Long getId() { return id; }
    public Long getConversationId() { return conversationId; }
    public Long getSenderId() { return senderId; }
    public String getContent() { return content; }
    public MessageType getType() { return type; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public boolean isRead() { return isRead; }
    public String getFilePath() { return filePath; }

    // Setters
    public void setId(Long id) { this.id = id; }
    public void setConversationId(Long conversationId) { this.conversationId = conversationId; }
    public void setSenderId(Long senderId) { this.senderId = senderId; }
    public void setContent(String content) { this.content = content; }
    public void setType(MessageType type) { this.type = type; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    public void setRead(boolean read) { isRead = read; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Message message = (Message) o;
        return Objects.equals(id, message.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Message{id=" + id + ", senderId=" + senderId + ", type=" + type + ", timestamp=" + timestamp + "}";
    }
}