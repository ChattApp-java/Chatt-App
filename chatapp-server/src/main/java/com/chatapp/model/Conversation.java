package com.chatapp.model;

import java.time.LocalDateTime;

/**
 * Conversation.java
 * Modèle représentant une conversation (individuelle ou de groupe).
 */
public class Conversation {

    /** Type de conversation. */
    public enum Type {
        INDIVIDUEL,
        GROUPE
    }

    private int id;
    private Type type;
    private Integer groupeId;           // nullable — null si type == INDIVIDUEL
    private int utilisateur1Id;
    private int utilisateur2Id;
    private LocalDateTime dateCreation;
    private LocalDateTime dernierMessage;

    // ── Constructeur complet ──────────────────────────────────────
    public Conversation(int id, Type type, Integer groupeId,
                        int utilisateur1Id, int utilisateur2Id,
                        LocalDateTime dateCreation, LocalDateTime dernierMessage) {
        this.id = id;
        this.type = type;
        this.groupeId = groupeId;
        this.utilisateur1Id = utilisateur1Id;
        this.utilisateur2Id = utilisateur2Id;
        this.dateCreation = dateCreation;
        this.dernierMessage = dernierMessage;
    }

    // ── Constructeur conversation individuelle ────────────────────
    public Conversation(int utilisateur1Id, int utilisateur2Id) {
        this.type = Type.INDIVIDUEL;
        this.groupeId = null;
        this.utilisateur1Id = utilisateur1Id;
        this.utilisateur2Id = utilisateur2Id;
        this.dateCreation = LocalDateTime.now();
    }

    // ── Constructeur conversation de groupe ───────────────────────
    public Conversation(int groupeId) {
        this.type = Type.GROUPE;
        this.groupeId = groupeId;
        this.utilisateur1Id = 0;
        this.utilisateur2Id = 0;
        this.dateCreation = LocalDateTime.now();
    }

    // ── Getters ───────────────────────────────────────────────────
    public int getId() {
        return id;
    }

    public Type getType() {
        return type;
    }

    public Integer getGroupeId() {
        return groupeId;
    }

    public int getUtilisateur1Id() {
        return utilisateur1Id;
    }

    public int getUtilisateur2Id() {
        return utilisateur2Id;
    }

    public LocalDateTime getDateCreation() {
        return dateCreation;
    }

    public LocalDateTime getDernierMessage() {
        return dernierMessage;
    }

    // ── Setters ───────────────────────────────────────────────────
    public void setId(int id) {
        this.id = id;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public void setGroupeId(Integer groupeId) {
        this.groupeId = groupeId;
    }

    public void setUtilisateur1Id(int utilisateur1Id) {
        this.utilisateur1Id = utilisateur1Id;
    }

    public void setUtilisateur2Id(int utilisateur2Id) {
        this.utilisateur2Id = utilisateur2Id;
    }

    public void setDateCreation(LocalDateTime dateCreation) {
        this.dateCreation = dateCreation;
    }

    public void setDernierMessage(LocalDateTime dernierMessage) {
        this.dernierMessage = dernierMessage;
    }

    @Override
    public String toString() {
        return "Conversation{id=" + id
                + ", type=" + type
                + ", groupeId=" + groupeId
                + ", u1=" + utilisateur1Id
                + ", u2=" + utilisateur2Id
                + "}";
    }
}
