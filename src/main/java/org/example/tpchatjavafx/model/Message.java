package org.example.tpchatjavafx.model;

import java.time.LocalDateTime;
import java.util.Objects;

public class Message {
    private int id;
    private String contenu;
    private LocalDateTime dateEnvoi;
    private String type; // TEXTE, AUDIO, VIDEO, SYSTEME
    private int expediteurId;
    private Integer destinataireId; // Peut être null si groupe
    private int conversationId;
    private Integer groupeId;
    private Integer reunionId;
    private boolean estLu;
    private boolean isDeleted;
    private LocalDateTime deletedAt;
    private Integer deletedBy;

    // Champs transient (non persistés) pour l'affichage
    private transient Utilisateur expediteur;

    public Message() {}

    public Message(int id, String contenu, LocalDateTime dateEnvoi, String type, int expediteurId, Integer destinataireId, int conversationId, boolean estLu) {
        this.id = id;
        this.contenu = contenu;
        this.dateEnvoi = dateEnvoi;
        this.type = type;
        this.expediteurId = expediteurId;
        this.destinataireId = destinataireId;
        this.conversationId = conversationId;
        this.estLu = estLu;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getContenu() { return contenu; }
    public void setContenu(String contenu) { this.contenu = contenu; }

    public LocalDateTime getDateEnvoi() { return dateEnvoi; }
    public void setDateEnvoi(LocalDateTime dateEnvoi) { this.dateEnvoi = dateEnvoi; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public int getExpediteurId() { return expediteurId; }
    public void setExpediteurId(int expediteurId) { this.expediteurId = expediteurId; }

    public Integer getDestinataireId() { return destinataireId; }
    public void setDestinataireId(Integer destinataireId) { this.destinataireId = destinataireId; }

    public int getConversationId() { return conversationId; }
    public void setConversationId(int conversationId) { this.conversationId = conversationId; }

    public Integer getGroupeId() { return groupeId; }
    public void setGroupeId(Integer groupeId) { this.groupeId = groupeId; }

    public Integer getReunionId() { return reunionId; }
    public void setReunionId(Integer reunionId) { this.reunionId = reunionId; }

    public boolean isEstLu() { return estLu; }
    public void setEstLu(boolean estLu) { this.estLu = estLu; }

    public boolean isDeleted() { return isDeleted; }
    public void setDeleted(boolean deleted) { isDeleted = deleted; }

    public LocalDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(LocalDateTime deletedAt) { this.deletedAt = deletedAt; }

    public Integer getDeletedBy() { return deletedBy; }
    public void setDeletedBy(Integer deletedBy) { this.deletedBy = deletedBy; }

    public Utilisateur getExpediteur() { return expediteur; }
    public void setExpediteur(Utilisateur expediteur) { this.expediteur = expediteur; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Message message = (Message) o;
        return id == message.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
