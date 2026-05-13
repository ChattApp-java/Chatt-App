package org.example.tpchatjavafx.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Modèle d'une conversation (individuelle ou de groupe).
 * P2 : ajout du type (INDIVIDUEL/GROUPE) et groupeId nullable.
 */
public class Conversation {

    public static final String TYPE_INDIVIDUEL = "INDIVIDUEL";
    public static final String TYPE_GROUPE     = "GROUPE";

    private int id;
    private String type = TYPE_INDIVIDUEL;  // P2 : INDIVIDUEL ou GROUPE
    private Integer groupeId;               // P2 : nullable, lié à Groupe si type=GROUPE
    private LocalDateTime dateCreation;
    private LocalDateTime derniereModification;

    public Conversation() {}

    public Conversation(int id, LocalDateTime dateCreation, LocalDateTime derniereModification) {
        this.id = id;
        this.dateCreation = dateCreation;
        this.derniereModification = derniereModification;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public Integer getGroupeId() { return groupeId; }
    public void setGroupeId(Integer groupeId) { this.groupeId = groupeId; }

    public LocalDateTime getDateCreation() { return dateCreation; }
    public void setDateCreation(LocalDateTime dateCreation) { this.dateCreation = dateCreation; }

    public LocalDateTime getDerniereModification() { return derniereModification; }
    public void setDerniereModification(LocalDateTime derniereModification) {
        this.derniereModification = derniereModification;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Conversation that = (Conversation) o;
        return id == that.id;
    }

    @Override
    public int hashCode() { return Objects.hash(id); }
}
