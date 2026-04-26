package client;

import audio.GestionnaireAppelAudio;
import video.GestionnaireAppelVideo;

import javax.swing.*;

/**
 * Gère les appels audio et vidéo côté client.
 */
public class GestionnaireAppelClient {

    private GestionnaireAppelAudio audio;
    private GestionnaireAppelVideo video;
    private boolean appelActif = false;
    private String typeAppel = null;

    public void demarrerAppelAudio(String serveurIp) throws Exception {
        audio = new GestionnaireAppelAudio();
        audio.demarrerAppel(serveurIp);
        appelActif = true;
        typeAppel = "AUDIO";
    }

    public void demarrerAppelVideo(String serveurIp, JLabel ecran) throws Exception {
        video = new GestionnaireAppelVideo();
        video.demarrerAppel(serveurIp, ecran);
        appelActif = true;
        typeAppel = "VIDEO";
    }

    public void terminerAppel() {
        if ("AUDIO".equals(typeAppel) && audio != null) {
            audio.terminerAppel();
        } else if ("VIDEO".equals(typeAppel) && video != null) {
            video.terminerAppel();
        }
        appelActif = false;
        typeAppel = null;
    }

    public boolean estEnAppel() {
        return appelActif;
    }

    public String getTypeAppel() {
        return typeAppel;
    }
}