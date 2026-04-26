package video;

/**
 * Orchestration complète d'un appel vidéo (émission + réception).
 */
public class GestionnaireAppelVideo {

    private EmetteurVideo emetteur;
    private RecepteurVideo recepteur;
    private boolean appelEnCours = false;

    public void demarrerAppel(String serveurIp, javax.swing.JLabel ecran) throws Exception {
        if (appelEnCours) return;

        emetteur = new EmetteurVideo();
        recepteur = new RecepteurVideo();

        emetteur.demarrer(serveurIp);
        recepteur.demarrer(serveurIp, ecran);

        appelEnCours = true;
        System.out.println("Appel vidéo démarré ✓");
    }

    public void terminerAppel() {
        if (!appelEnCours) return;
        emetteur.arreter();
        recepteur.arreter();
        appelEnCours = false;
        System.out.println("Appel vidéo terminé ✓");
    }

    public boolean estEnAppel() {
        return appelEnCours;
    }
}