package com.chatapp.model;

import java.time.LocalDateTime;

/**
 * Reunion.java
 * Modèle représentant une réunion (appel de groupe) liée à un groupe.
 */
public class Reunion {

    /** Types de réunion possibles. */
    public enum Type {
        AUDIO,
        VIDEO
    }

    /** Statuts possibles d'une réunion. */
    public enum Statut {
        EN_ATTENTE,
        EN_COURS,
        TERMINEE,
        ANNULEE
    }

    private int id;
    private int groupeId;
    private int initiateurId;
    private Type type;
    private Statut statut;
    private LocalDateTime dateDebut;
    private LocalDateTime dateFin; // nullable — null si encore en cours

    // ── Constructeur complet ──────────────────────────────────────
    public Reunion(int id, int groupeId, int initiateurId,
                   Type type, Statut statut,
                   LocalDateTime dateDebut, LocalDateTime dateFin) {
        this.id = id;
        this.groupeId = groupeId;
        this.initiateurId = initiateurId;
        this.type = type;
        this.statut = statut;
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
    }

    // ── Constructeur démarrage (sans id ni dateFin) ───────────────
    public Reunion(int groupeId, int initiateurId, Type type) {
        this.groupeId = groupeId;
        this.initiateurId = initiateurId;
        this.type = type;
        this.statut = Statut.EN_ATTENTE;
        this.dateDebut = LocalDateTime.now();
        this.dateFin = null;
    }

    // ── Getters ───────────────────────────────────────────────────
    public int getId() {
        return id;
    }

    public int getGroupeId() {
        return groupeId;
    }

    public int getInitiateurId() {
        return initiateurId;
    }

    public Type getType() {
        return type;
    }

    public Statut getStatut() {
        return statut;
    }

    public LocalDateTime getDateDebut() {
        return dateDebut;
    }

    public LocalDateTime getDateFin() {
        return dateFin;
    }

    // ── Setters ───────────────────────────────────────────────────
    public void setId(int id) {
        this.id = id;
    }

    public void setGroupeId(int groupeId) {
        this.groupeId = groupeId;
    }

    public void setInitiateurId(int initiateurId) {
        this.initiateurId = initiateurId;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public void setStatut(Statut statut) {
        this.statut = statut;
    }

    public void setDateDebut(LocalDateTime dateDebut) {
        this.dateDebut = dateDebut;
    }

    public void setDateFin(LocalDateTime dateFin) {
        this.dateFin = dateFin;
    }

    @Override
    public String toString() {
        return "Reunion{id=" + id
                + ", groupeId=" + groupeId
                + ", initiateurId=" + initiateurId
                + ", type=" + type
                + ", statut=" + statut
                + ", dateDebut=" + dateDebut
                + ", dateFin=" + dateFin
                + "}";
    }
}
