package com.chatapp.model;

import java.time.LocalDateTime;



public class User {

    private int id_user;
    private String username;
    private String password;
    private String email;
    private boolean status; // true = en ligne, false = hors ligne
    private LocalDateTime created_at;

    // ── Constructeur complet ──────────────────────────────────
    public User(int id_user, String username, String password,
            String email, boolean status, LocalDateTime created_at) {
        this.id_user = id_user;
        this.username = username;
        this.password = password;
        this.email = email;
        this.status = status;
        this.created_at = created_at;
    }

    // ── Constructeur création (sans id ni date) ───────────────
    public User(String username, String password, String email) {
        this.username = username;
        this.password = password;
        this.email = email;
        this.status = false;
    }

    // ── Getters ───────────────────────────────────────────────
    public int getId_user() {
        return id_user;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getEmail() {
        return email;
    }

    public boolean isStatus() {
        return status;
    }

    public LocalDateTime getCreated_at() {
        return created_at;
    }

    // ── Setters ───────────────────────────────────────────────
    public void setId_user(int id_user) {
        this.id_user = id_user;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setStatus(boolean status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "User{id=" + id_user
                + ", username='" + username + "'"
                + ", status=" + (status ? "En ligne" : "Hors ligne")
                + "}";
    }
}

