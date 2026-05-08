package com.chatapp.model;

import java.time.LocalDateTime;

/**
 * Appel.java
 * Modèle représentant un appel (audio/vidéo), individuel ou lié à une réunion.
 */
public class Appel {

    /** Types d'appel possibles. */
    public enum TypeAppel {
        AUDIO,
        VIDEO
    }

    /** Statuts possibles d'un appel. */
    public enum StatutAppel {
        EN_ATTENTE,
        ACCEPTE,
        REFUSE,
        MANQUE,
        TERMINE
    }

    private int id;
    private int appelantId;
    private int recepteurId;
    private TypeAppel type;
    private StatutAppel statut;
    private LocalDateTime dateDebut;
    private LocalDateTime dateFin;       // nullable
    private Integer reunionId;           // nullable — lié à une Reunion si appel de groupe
    private boolean estReunion;          // false par défaut (appel individuel)

    // ── Constructeur complet ──────────────────────────────────────
    public Appel(int id, int appelantId, int recepteurId,
                 TypeAppel type, StatutAppel statut,
                 LocalDateTime dateDebut, LocalDateTime dateFin,
                 Integer reunionId, boolean estReunion) {
        this.id = id;
        this.appelantId = appelantId;
        this.recepteurId = recepteurId;
        this.type = type;
        this.statut = statut;
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
        this.reunionId = reunionId;
        this.estReunion = estReunion;
    }

    // ── Constructeur appel individuel (sans id, dateFin, reunion) ─
    public Appel(int appelantId, int recepteurId, TypeAppel type) {
        this.appelantId = appelantId;
        this.recepteurId = recepteurId;
        this.type = type;
        this.statut = StatutAppel.EN_ATTENTE;
        this.dateDebut = LocalDateTime.now();
        this.dateFin = null;
        this.reunionId = null;
        this.estReunion = false;
    }

    // ── Constructeur appel de réunion ─────────────────────────────
    public Appel(int appelantId, int recepteurId, TypeAppel type, int reunionId) {
        this(appelantId, recepteurId, type);
        this.reunionId = reunionId;
        this.estReunion = true;
    }

    // ── Getters ───────────────────────────────────────────────────
    public int getId() {
        return id;
    }

    public int getAppelantId() {
        return appelantId;
    }

    public int getRecepteurId() {
        return recepteurId;
    }

    public TypeAppel getType() {
        return type;
    }

    public StatutAppel getStatut() {
        return statut;
    }

    public LocalDateTime getDateDebut() {
        return dateDebut;
    }

    public LocalDateTime getDateFin() {
        return dateFin;
    }

    public Integer getReunionId() {
        return reunionId;
    }

    public boolean isEstReunion() {
        return estReunion;
    }

    // ── Setters ───────────────────────────────────────────────────
    public void setId(int id) {
        this.id = id;
    }

    public void setAppelantId(int appelantId) {
        this.appelantId = appelantId;
    }

    public void setRecepteurId(int recepteurId) {
        this.recepteurId = recepteurId;
    }

    public void setType(TypeAppel type) {
        this.type = type;
    }

    public void setStatut(StatutAppel statut) {
        this.statut = statut;
    }

    public void setDateDebut(LocalDateTime dateDebut) {
        this.dateDebut = dateDebut;
    }

    public void setDateFin(LocalDateTime dateFin) {
        this.dateFin = dateFin;
    }

    public void setReunionId(Integer reunionId) {
        this.reunionId = reunionId;
        this.estReunion = (reunionId != null);
    }

    public void setEstReunion(boolean estReunion) {
        this.estReunion = estReunion;
    }

    @Override
    public String toString() {
        return "Appel{id=" + id
                + ", appelantId=" + appelantId
                + ", recepteurId=" + recepteurId
                + ", type=" + type
                + ", statut=" + statut
                + ", estReunion=" + estReunion
                + ", reunionId=" + reunionId
                + "}";
    }
}
