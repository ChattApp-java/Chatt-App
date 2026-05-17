package com.wechat.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Group {
    private Long id;
    private String name;
    private String description;
    private Long adminId;
    private List<Long> memberIds;
    private String avatar;
    private LocalDateTime createdAt;

    public Group() {
        this.memberIds = new ArrayList<>();
        this.createdAt = LocalDateTime.now();
    }

    public Group(String name, String description, Long adminId, List<Long> memberIds) {
        this();
        this.name = name;
        this.description = description;
        this.adminId = adminId;
        this.memberIds = new ArrayList<>(memberIds);
    }

    // Getters
    public Long getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public Long getAdminId() { return adminId; }
    public List<Long> getMemberIds() { return new ArrayList<>(memberIds); }
    public String getAvatar() { return avatar; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    // Setters
    public void setId(Long id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setDescription(String description) { this.description = description; }
    public void setAdminId(Long adminId) { this.adminId = adminId; }
    public void setMemberIds(List<Long> memberIds) { this.memberIds = new ArrayList<>(memberIds); }
    public void setAvatar(String avatar) { this.avatar = avatar; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public void addMember(Long userId) {
        if (!memberIds.contains(userId)) {
            memberIds.add(userId);
        }
    }

    public void removeMember(Long userId) {
        memberIds.remove(userId);
    }

    public boolean isMember(Long userId) {
        return memberIds.contains(userId);
    }

    public boolean isAdmin(Long userId) {
        return adminId.equals(userId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Group group = (Group) o;
        return Objects.equals(id, group.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Group{id=" + id + ", name='" + name + "', members=" + memberIds.size() + "}";
    }
}