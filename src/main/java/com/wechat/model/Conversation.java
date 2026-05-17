package com.wechat.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Conversation {
    private Long id;
    private ConversationType type;
    private String name;
    private List<Long> participantIds;
    private Long lastMessageId;
    private String lastMessagePreview;
    private LocalDateTime lastMessageTime;
    private int unreadCount;
    private LocalDateTime createdAt;

    public enum ConversationType {
        PRIVATE, GROUP
    }

    public Conversation() {
        this.participantIds = new ArrayList<>();
        this.type = ConversationType.PRIVATE;
        this.unreadCount = 0;
        this.createdAt = LocalDateTime.now();
    }

    public Conversation(ConversationType type, String name, List<Long> participantIds) {
        this();
        this.type = type;
        this.name = name;
        this.participantIds = new ArrayList<>(participantIds);
    }

    // Getters
    public Long getId() { return id; }
    public ConversationType getType() { return type; }
    public String getName() { return name; }
    public List<Long> getParticipantIds() { return new ArrayList<>(participantIds); }
    public Long getLastMessageId() { return lastMessageId; }
    public String getLastMessagePreview() { return lastMessagePreview; }
    public LocalDateTime getLastMessageTime() { return lastMessageTime; }
    public int getUnreadCount() { return unreadCount; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    // Setters
    public void setId(Long id) { this.id = id; }
    public void setType(ConversationType type) { this.type = type; }
    public void setName(String name) { this.name = name; }
    public void setParticipantIds(List<Long> participantIds) { this.participantIds = new ArrayList<>(participantIds); }
    public void setLastMessageId(Long lastMessageId) { this.lastMessageId = lastMessageId; }
    public void setLastMessagePreview(String lastMessagePreview) { this.lastMessagePreview = lastMessagePreview; }
    public void setLastMessageTime(LocalDateTime lastMessageTime) { this.lastMessageTime = lastMessageTime; }
    public void setUnreadCount(int unreadCount) { this.unreadCount = unreadCount; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public void addParticipant(Long userId) {
        if (!participantIds.contains(userId)) {
            participantIds.add(userId);
        }
    }

    public void removeParticipant(Long userId) {
        participantIds.remove(userId);
    }

    public void incrementUnread() {
        this.unreadCount++;
    }

    public void resetUnread() {
        this.unreadCount = 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Conversation that = (Conversation) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Conversation{id=" + id + ", type=" + type + ", name='" + name + "', participants=" + participantIds.size() + "}";
    }
}