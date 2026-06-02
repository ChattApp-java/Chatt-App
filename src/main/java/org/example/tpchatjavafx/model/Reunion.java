package org.example.tpchatjavafx.model;

import java.time.LocalDateTime;

public class Reunion {
    public static final String TYPE_AUDIO = "AUDIO";
    public static final String TYPE_VIDEO = "VIDEO";
    public static final String STATUT_EN_COURS = "EN_COURS";
    public static final String STATUT_TERMINEE = "TERMINEE";

    private int id;
    private int groupeId;
    private int initiateurId;
    private String type;
    private String statut = STATUT_EN_COURS;
    private LocalDateTime dateDebut;
    private LocalDateTime dateFin;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getGroupeId() { return groupeId; }
    public void setGroupeId(int groupeId) { this.groupeId = groupeId; }

    public int getInitiateurId() { return initiateurId; }
    public void setInitiateurId(int initiateurId) { this.initiateurId = initiateurId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }

    public LocalDateTime getDateDebut() { return dateDebut; }
    public void setDateDebut(LocalDateTime dateDebut) { this.dateDebut = dateDebut; }

    public LocalDateTime getDateFin() { return dateFin; }
    public void setDateFin(LocalDateTime dateFin) { this.dateFin = dateFin; }
}
