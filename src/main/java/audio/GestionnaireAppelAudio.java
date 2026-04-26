package audio;

/**
 * Orchestration complète d'un appel audio (émission + réception).
 */
public class GestionnaireAppelAudio {

    private EmetteurAudio emetteur;
    private RecepteurAudio recepteur;
    private boolean appelEnCours = false;

    public void demarrerAppel(String serveurIp) throws Exception {
        if (appelEnCours) return;

        emetteur = new EmetteurAudio();
        recepteur = new RecepteurAudio();

        emetteur.demarrer(serveurIp);
        recepteur.demarrer(serveurIp);

        appelEnCours = true;
        System.out.println("Appel audio démarré ✓");
    }

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