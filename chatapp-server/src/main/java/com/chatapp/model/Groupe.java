package com.chatapp.model;

import java.time.LocalDateTime;

/**
 * Groupe.java
 * Modèle représentant un groupe de discussion.
 */
public class Groupe {

    private int id;
    private String nom;
    private String description;
    private int createurId;
    private LocalDateTime dateCreation;

    // ── Constructeur complet ──────────────────────────────────────
    public Groupe(int id, String nom, String description,
                  int createurId, LocalDateTime dateCreation) {
        this.id = id;
        this.nom = nom;
        this.description = description;
        this.createurId = createurId;
        this.dateCreation = dateCreation;
    }

    // ── Constructeur création (sans id ni date) ───────────────────
    public Groupe(String nom, String description, int createurId) {
        this.nom = nom;
        this.description = description;
        this.createurId = createurId;
        this.dateCreation = LocalDateTime.now();
    }

    // ── Getters ───────────────────────────────────────────────────
    public int getId() {
        return id;
    }

    public String getNom() {
        return nom;
    }

    public String getDescription() {
        return description;
    }

    public int getCreateurId() {
        return createurId;
    }

    public LocalDateTime getDateCreation() {
        return dateCreation;
    }

    // ── Setters ───────────────────────────────────────────────────
    public void setId(int id) {
        this.id = id;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setCreateurId(int createurId) {
        this.createurId = createurId;
    }

    public void setDateCreation(LocalDateTime dateCreation) {
        this.dateCreation = dateCreation;
    }

    @Override
    public String toString() {
        return "Groupe{id=" + id
                + ", nom='" + nom + "'"
                + ", createurId=" + createurId
                + ", dateCreation=" + dateCreation
                + "}";
    }
}
