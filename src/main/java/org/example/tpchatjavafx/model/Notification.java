package org.example.tpchatjavafx.model;

import java.util.Date;

public class Notification {
    private int id;
    private int utilisateurId;
    private String contenu;
    private String type; // NOUVEAU_MESSAGE, APPEL_MANQUE, DEMANDE_CONTACT
    private boolean estLue;
    private Date dateCreation;

    public Notification() {}

    public Notification(int id, int utilisateurId, String contenu, String type, boolean estLue, Date dateCreation) {
        this.id = id;
        this.utilisateurId = utilisateurId;
        this.contenu = contenu;
        this.type = type;
        this.estLue = estLue;
        this.dateCreation = dateCreation;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUtilisateurId() { return utilisateurId; }
    public void setUtilisateurId(int utilisateurId) { this.utilisateurId = utilisateurId; }

    public String getContenu() { return contenu; }
    public void setContenu(String contenu) { this.contenu = contenu; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public boolean isEstLue() { return estLue; }
    public void setEstLue(boolean estLue) { this.estLue = estLue; }

    public Date getDateCreation() { return dateCreation; }
    public void setDateCreation(Date dateCreation) { this.dateCreation = dateCreation; }
}
