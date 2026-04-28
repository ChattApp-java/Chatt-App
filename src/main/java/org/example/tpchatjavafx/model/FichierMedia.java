package org.example.tpchatjavafx.model;

import java.util.Date;

public class FichierMedia {
    private int id;
    private String type; // AUDIO, VIDEO, IMAGE, FILE
    private String nomFichier;
    private String cheminAcces;
    private long taille;
    private int messageId;
    private Date dateAjout;

    public FichierMedia() {}

    public FichierMedia(int id, String type, String nomFichier, String cheminAcces, long taille, int messageId, Date dateAjout) {
        this.id = id;
        this.type = type;
        this.nomFichier = nomFichier;
        this.cheminAcces = cheminAcces;
        this.taille = taille;
        this.messageId = messageId;
        this.dateAjout = dateAjout;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getNomFichier() { return nomFichier; }
    public void setNomFichier(String nomFichier) { this.nomFichier = nomFichier; }

    public String getCheminAcces() { return cheminAcces; }
    public void setCheminAcces(String cheminAcces) { this.cheminAcces = cheminAcces; }

    public long getTaille() { return taille; }
    public void setTaille(long taille) { this.taille = taille; }

    public int getMessageId() { return messageId; }
    public void setMessageId(int messageId) { this.messageId = messageId; }

    public Date getDateAjout() { return dateAjout; }
    public void setDateAjout(Date dateAjout) { this.dateAjout = dateAjout; }
}
