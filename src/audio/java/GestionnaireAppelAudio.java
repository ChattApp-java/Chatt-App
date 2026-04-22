import javax.sound.sampled.*;
import java.net.*;
import java.io.*;


public class GestionnaireAppelAudio {

    private EmetteurAudio emetteur;
    private RecepteurAudio recepteur;
    private boolean appelEnCours = false;

    // Tes coéquipiers appellent cette méthode quand un appel démarre
    // socketAppel = le socket TCP déjà établi entre les deux clients
    public void demarrerAppel(Socket socketAppel) throws Exception {
        if (appelEnCours) return;

        emetteur   = new EmetteurAudio();
        recepteur  = new RecepteurAudio();

        emetteur.demarrer(socketAppel);   // capture micro + envoie
        recepteur.demarrer(socketAppel);  // reçoit + joue

        appelEnCours = true;
        System.out.println("Appel audio démarré ✓");
    }

    // Tes coéquipiers appellent cette méthode quand l'appel se termine
    public void terminerAppel() {
        if (!appelEnCours) return;
        emetteur.arreter();
        recepteur.arreter();
        appelEnCours = false;
        System.out.println("Appel audio terminé ✓");
    }

    public boolean estEnAppel() {
        return appelEnCours;
    }
}