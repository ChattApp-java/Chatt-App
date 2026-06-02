package org.example.tpchatjavafx.model;

import java.time.LocalDateTime;

public class GroupeMembre {
    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_MEMBRE = "MEMBRE";

    private int id;
    private int groupeId;
    private int utilisateurId;
    private LocalDateTime dateAjout;
    private String role = ROLE_MEMBRE;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getGroupeId() { return groupeId; }
    public void setGroupeId(int groupeId) { this.groupeId = groupeId; }

    public int getUtilisateurId() { return utilisateurId; }
    public void setUtilisateurId(int utilisateurId) { this.utilisateurId = utilisateurId; }

    public LocalDateTime getDateAjout() { return dateAjout; }
    public void setDateAjout(LocalDateTime dateAjout) { this.dateAjout = dateAjout; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}
