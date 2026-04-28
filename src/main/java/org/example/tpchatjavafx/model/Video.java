package org.example.tpchatjavafx.model;

import java.util.Date;

public class Video extends FichierMedia {
    private String resolution;
    private int duree;

    public Video() {
        super();
        this.setType("VIDEO");
    }

    public Video(int id, String nomFichier, String cheminAcces, long taille, int messageId, Date dateAjout, String resolution, int duree) {
        super(id, "VIDEO", nomFichier, cheminAcces, taille, messageId, dateAjout);
        this.resolution = resolution;
        this.duree = duree;
    }

    public String getResolution() { return resolution; }
    public void setResolution(String resolution) { this.resolution = resolution; }

    public int getDuree() { return duree; }
    public void setDuree(int duree) { this.duree = duree; }
}
