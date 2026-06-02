package org.example.tpchatjavafx.model;

import java.time.LocalDateTime;
import java.util.Objects;

public class Groupe {
    private int id;
    private String nom;
    private String description;
    private int createurId;
    private LocalDateTime dateCreation;

    public Groupe() {}

    public Groupe(int id, String nom, String description, int createurId, LocalDateTime dateCreation) {
        this.id = id;
        this.nom = nom;
        this.description = description;
        this.createurId = createurId;
        this.dateCreation = dateCreation;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public int getCreateurId() { return createurId; }
    public void setCreateurId(int createurId) { this.createurId = createurId; }

    public LocalDateTime getDateCreation() { return dateCreation; }
    public void setDateCreation(LocalDateTime dateCreation) { this.dateCreation = dateCreation; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Groupe groupe)) return false;
        return id == groupe.id;
    }

    @Override
    public int hashCode() { return Objects.hash(id); }

    @Override
    public String toString() {
        return id + ":" + nullSafe(nom) + ":" + nullSafe(description) + ":" + createurId;
    }

    private String nullSafe(String value) {
        return value == null ? "" : value.replace(":", " ").replace(",", " ");
    }
}
