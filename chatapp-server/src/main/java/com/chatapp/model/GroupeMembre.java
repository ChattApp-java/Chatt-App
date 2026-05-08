package com.chatapp.model;

import java.time.LocalDateTime;

/**
 * GroupeMembre.java
 * Modèle de liaison entre un groupe et un utilisateur (membre).
 */
public class GroupeMembre {

    /** Rôles possibles d'un membre dans un groupe. */
    public enum Role {
        ADMIN,
        MEMBRE
    }

    private int id;
    private int groupeId;
    private int utilisateurId;
    private LocalDateTime dateAjout;
    private Role role;

    // ── Constructeur complet ──────────────────────────────────────
    public GroupeMembre(int id, int groupeId, int utilisateurId,
                        LocalDateTime dateAjout, Role role) {
        this.id = id;
        this.groupeId = groupeId;
        this.utilisateurId = utilisateurId;
        this.dateAjout = dateAjout;
        this.role = role;
    }

    // ── Constructeur ajout (sans id ni date) ─────────────────────
    public GroupeMembre(int groupeId, int utilisateurId, Role role) {
        this.groupeId = groupeId;
        this.utilisateurId = utilisateurId;
        this.role = role;
        this.dateAjout = LocalDateTime.now();
    }

    // ── Getters ───────────────────────────────────────────────────
    public int getId() {
        return id;
    }

    public int getGroupeId() {
        return groupeId;
    }

    public int getUtilisateurId() {
        return utilisateurId;
    }

    public LocalDateTime getDateAjout() {
        return dateAjout;
    }

    public Role getRole() {
        return role;
    }

    // ── Setters ───────────────────────────────────────────────────
    public void setId(int id) {
        this.id = id;
    }

    public void setGroupeId(int groupeId) {
        this.groupeId = groupeId;
    }

    public void setUtilisateurId(int utilisateurId) {
        this.utilisateurId = utilisateurId;
    }

    public void setDateAjout(LocalDateTime dateAjout) {
        this.dateAjout = dateAjout;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    @Override
    public String toString() {
        return "GroupeMembre{id=" + id
                + ", groupeId=" + groupeId
                + ", utilisateurId=" + utilisateurId
                + ", role=" + role
                + ", dateAjout=" + dateAjout
                + "}";
    }
}
