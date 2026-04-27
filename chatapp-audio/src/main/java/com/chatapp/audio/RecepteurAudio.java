package com.chatapp.audio;

import javax.sound.sampled.*;
import java.net.*;
import java.io.*;


public class RecepteurAudio {

    private Socket socket;
    private InputStream entree;
    private LecteurAudio lecteur;
    private volatile boolean enCours = false;
    private Thread threadReception;

    // Appelé par GestionnaireAppelAudio avec le socket déjà ouvert
    public void demarrer(Socket socketExistant) throws Exception {
        this.socket = socketExistant;
        this.entree = socket.getInputStream();
        this.lecteur = new LecteurAudio();
        lecteur.demarrer();
        enCours = true;

        threadReception = new Thread(() -> {
            while (enCours) {
                try {
                    byte[] chunk = lireChunkComplet(CapteurAudio.CHUNK_SIZE);
                    lecteur.jouer(chunk);
                } catch (IOException e) {
                    enCours = false;
                }
            }
        }, "Thread-Reception-Audio");

        threadReception.setDaemon(true);
        threadReception.start();
    }

    // Boucle obligatoire : TCP peut envoyer les bytes en plusieurs morceaux
    private byte[] lireChunkComplet(int taille) throws IOException {
        byte[] buffer = new byte[taille];
        int total = 0;
        while (total < taille) {
            int lu = entree.read(buffer, total, taille - total);
            if (lu == -1) throw new IOException("Connexion fermée");
            total += lu;
        }
        return buffer;
    }

    public void arreter() {
        enCours = false;
        lecteur.arreter();
    }
}

