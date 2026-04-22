
public class TestLoopback {
    public static void main(String[] args) throws Exception {

        CapteurAudio capteur = new CapteurAudio();
        LecteurAudio lecteur = new LecteurAudio();

        capteur.demarrer();
        lecteur.demarrer();

        System.out.println("Parle dans le micro... (5 secondes)");

        // Calcul : 44100 Hz × 2 bytes × 1 canal = 88200 bytes/sec
        // 5 secondes = 5 × 88200 / 4096 ≈ 107 chunks
        for (int i = 0; i < 107; i++) {
            byte[] chunk = capteur.lireChunk();
            lecteur.jouer(chunk);  // tu entends ta voix en direct
        }

        capteur.arreter();
        lecteur.arreter();
        System.out.println("Test terminé ✓");
    }
}