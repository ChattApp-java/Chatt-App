package serveur;

/**
 * Lance tous les services serveur (signalisation + relais audio/vidéo).
 */
public class LanceurServeur {

    public static void main(String[] args) {

        System.out.println("Démarrage des services...\n");

        // 1. Relais audio TCP
        RelaisAudio relaisAudio = new RelaisAudio();
        Thread audioThread = new Thread(relaisAudio);
        audioThread.setDaemon(true);
        audioThread.setName("Relais-Audio-TCP-5001");
        audioThread.start();

        // 2. Relais vidéo TCP
        RelaisVideo relaisVideo = new RelaisVideo();
        Thread videoThread = new Thread(relaisVideo);
        videoThread.setDaemon(true);
        videoThread.setName("Relais-Video-TCP-5002");
        videoThread.start();

        // 3. Serveur TCP principal (bloquant — lancé en dernier)
        new ServeurPrincipal().demarrer();
    }
}