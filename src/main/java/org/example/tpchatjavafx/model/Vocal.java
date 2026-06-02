package org.example.tpchatjavafx.model;

import java.util.Date;

public class Vocal extends FichierMedia {
    private int duree;

    public Vocal() {
        super();
        this.setType("VOCAL");
    }

    public Vocal(int id, String nomFichier, String cheminAcces, long taille, int messageId, Date dateAjout, int duree) {
        super(id, "VOCAL", nomFichier, cheminAcces, taille, messageId, dateAjout);
        this.duree = duree;
    }

    public int getDuree() { return duree; }
    public void setDuree(int duree) { this.duree = duree; }
}
