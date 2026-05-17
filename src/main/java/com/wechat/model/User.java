package com.wechat.model;

import java.time.LocalDateTime;
import java.util.Objects;

public class User {
    private Long id;
    private String username;
    private String email;
    private String password;
    private String nickname;
    private String avatar;
    private UserStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime lastSeen;

    public enum UserStatus {
        ONLINE, OFFLINE, AWAY, BUSY
    }

    public User() {
        this.status = UserStatus.OFFLINE;
        this.createdAt = LocalDateTime.now();
    }

    public User(String username, String email, String password, String nickname) {
        this();
        this.username = username;
        this.email = email;
        this.password = password;
        this.nickname = nickname;
    }

    // Getters
    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getPassword() { return password; }
    public String getNickname() { return nickname; }
    public String getAvatar() { return avatar; }
    public UserStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getLastSeen() { return lastSeen; }

    // Setters
    public void setId(Long id) { this.id = id; }
    public void setUsername(String username) { this.username = username; }
    public void setEmail(String email) { this.email = email; }
    public void setPassword(String password) { this.password = password; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public void setAvatar(String avatar) { this.avatar = avatar; }
    public void setStatus(UserStatus status) { this.status = status; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setLastSeen(LocalDateTime lastSeen) { this.lastSeen = lastSeen; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(id, user.id) && Objects.equals(username, user.username);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, username);
    }

    @Override
    public String toString() {
        return "User{id=" + id + ", username='" + username + "', nickname='" + nickname + "', status=" + status + "}";
    }
}