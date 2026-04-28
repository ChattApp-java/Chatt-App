package org.example.tpchatjavafx.model;

import java.time.LocalDateTime;
import java.util.Objects;

public class Conversation {
    private int id;
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

    public LocalDateTime getDateCreation() { return dateCreation; }
    public void setDateCreation(LocalDateTime dateCreation) { this.dateCreation = dateCreation; }

    public LocalDateTime getDerniereModification() { return derniereModification; }
    public void setDerniereModification(LocalDateTime derniereModification) { this.derniereModification = derniereModification; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Conversation that = (Conversation) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
