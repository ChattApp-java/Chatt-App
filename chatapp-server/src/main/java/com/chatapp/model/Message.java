package com.chatapp.model;

import java.time.LocalDateTime;

/**
 * Message.java
 * Modèle représentant un message dans la base de données.
 */
public class Message {

    private int id_message;
    private int id_sender;
    private int id_receiver;
    private String contenu;
    private LocalDateTime date_envoi;
    private String statut; // "lu" ou "non_lu"
    private Integer groupeId;   // nullable — null si message individuel
    private Integer reunionId;  // nullable — null si message hors réunion

    // ── Constructeur complet ──────────────────────────────────
    public Message(int id_message, int id_sender, int id_receiver,
            String contenu, LocalDateTime date_envoi, String statut) {
        this.id_message = id_message;
        this.id_sender = id_sender;
        this.id_receiver = id_receiver;
        this.contenu = contenu;
        this.date_envoi = date_envoi;
        this.statut = statut;
        this.groupeId = null;
        this.reunionId = null;
    }

    // ── Constructeur complet avec groupeId et reunionId ───────────
    public Message(int id_message, int id_sender, int id_receiver,
            String contenu, LocalDateTime date_envoi, String statut,
            Integer groupeId, Integer reunionId) {
        this.id_message = id_message;
        this.id_sender = id_sender;
        this.id_receiver = id_receiver;
        this.contenu = contenu;
        this.date_envoi = date_envoi;
        this.statut = statut;
        this.groupeId = groupeId;
        this.reunionId = reunionId;
    }

    // ── Constructeur envoi (sans id ni date) ──────────────────
    public Message(int id_sender, int id_receiver, String contenu) {
        this.id_sender = id_sender;
        this.id_receiver = id_receiver;
        this.contenu = contenu;
        this.statut = "non_lu";
    }

    // ── Getters ───────────────────────────────────────────────
    public int getId_message() {
        return id_message;
    }

    public int getId_sender() {
        return id_sender;
    }

    public int getId_receiver() {
        return id_receiver;
    }

    public String getContenu() {
        return contenu;
    }

    public LocalDateTime getDate_envoi() {
        return date_envoi;
    }

    public String getStatut() {
        return statut;
    }

    // ── Getters groupeId / reunionId ─────────────────────────────
    public Integer getGroupeId() {
        return groupeId;
    }

    public Integer getReunionId() {
        return reunionId;
    }

    // ── Setters ───────────────────────────────────────────────
    public void setId_message(int id_message) {
        this.id_message = id_message;
    }

    public void setStatut(String statut) {
        this.statut = statut;
    }

    public void setDate_envoi(LocalDateTime d) {
        this.date_envoi = d;
    }

    public void setGroupeId(Integer groupeId) {
        this.groupeId = groupeId;
    }

    public void setReunionId(Integer reunionId) {
        this.reunionId = reunionId;
    }

    @Override
    public String toString() {
        return "Message{id=" + id_message
                + ", de=" + id_sender
                + ", vers=" + id_receiver
                + ", contenu='" + contenu + "'"
                + ", statut=" + statut
                + "}";
    }
}

