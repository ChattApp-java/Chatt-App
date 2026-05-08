package org.example.tpchatjavafx.model;

import java.util.Date;

public class














Appel {
    private int id;
    private String statut; // EN_COURS, TERMINE, MANQUE, REJETE
    private Date dateHeure;
    private int duree; // en secondes
    private String typeAppel; // VOCAL, VIDEO
    private int expediteurId; 
    private int destinataireId;

    public Appel() {}

    public Appel(int id, String statut, Date dateHeure, int duree, String typeAppel, int expediteurId, int destinataireId) {
        this.id = id;
        this.statut = statut;
        this.dateHeure = dateHeure;
        this.duree = duree;
        this.typeAppel = typeAppel;
        this.expediteurId = expediteurId;
        this.destinataireId = destinataireId;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }

    public Date getDateHeure() { return dateHeure; }
    public void setDateHeure(Date dateHeure) { this.dateHeure = dateHeure; }

    public int getDuree() { return duree; }
    public void setDuree(int duree) { this.duree = duree; }

    public String getTypeAppel() { return typeAppel; }
    public void setTypeAppel(String typeAppel) { this.typeAppel = typeAppel; }

    public int getExpediteurId() { return expediteurId; }
    public void setExpediteurId(int expediteurId) { this.expediteurId = expediteurId; }
    
    public int getDestinataireId() { return destinataireId; }
    public void setDestinataireId(int destinataireId) { this.destinataireId = destinataireId; }
}
