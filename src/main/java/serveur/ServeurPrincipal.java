package serveur;

import commun.Protocole;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Serveur TCP principal de signalisation.
 */
public class ServeurPrincipal {

    private final ConcurrentHashMap<String, GestionnaireClient> clients = new ConcurrentHashMap<>();
    private ServerSocket serverSocket;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    public void demarrer() {
        try {
            serverSocket = new ServerSocket(Protocole.PORT_SIGNALING);

            log("╔══════════════════════════════════════════╗");
            log("║     Serveur CHATAPP démarré             ║");
            log("║  Port TCP (signalisation) : " + Protocole.PORT_SIGNALING + "         ║");
            log("║  Port TCP (audio)         : " + Protocole.PORT_AUDIO  + "         ║");
            log("║  Port TCP (vidéo)         : " + Protocole.PORT_VIDEO  + "         ║");
            log("╚══════════════════════════════════════════╝");

            boucleAcceptation();

        } catch (IOException e) {
            log("ERREUR démarrage serveur : " + e.getMessage());
        }
    }

    private void boucleAcceptation() {
        log("En attente de connexions...\n");

        while (!serverSocket.isClosed()) {
            try {
                Socket clientSocket = serverSocket.accept();
                String ip = clientSocket.getInetAddress().getHostAddress();
                log("Nouvelle connexion depuis : " + ip);

                GestionnaireClient handler = new GestionnaireClient(clientSocket, this);
                Thread thread = new Thread(handler);
                thread.setDaemon(true);
                thread.setName("Client-" + ip);
                thread.start();

            } catch (IOException e) {
                if (!serverSocket.isClosed()) {
                    log("ERREUR accept : " + e.getMessage());
                }
            }
        }
    }

    public void enregistrerClient(GestionnaireClient handler) {
        clients.put(handler.getUsername(), handler);
        log("Utilisateur enregistré : " + handler.getUsername()
                + " | Total : " + clients.size());
    }

    public void supprimerClient(GestionnaireClient handler) {
        if (handler.getUsername() != null) {
            clients.remove(handler.getUsername());
            log("Utilisateur supprimé : " + handler.getUsername()
                    + " | Total : " + clients.size());
        }
    }

    public boolean isUsernameTaken(String username) {
        return clients.containsKey(username);
    }

    public GestionnaireClient getClient(String username) {
        return clients.get(username);
    }

    public String getConnectedUsernames() {
        return clients.keySet().stream().collect(Collectors.joining(","));
    }

    public void broadcast(String message, GestionnaireClient exclude) {
        int count = 0;
        for (GestionnaireClient handler : clients.values()) {
            if (handler != exclude) {
                handler.envoyerMessage(message);
                count++;
            }
        }
        log("Broadcast envoyé à " + count + " client(s)");
    }

    public int getClientCount() {
        return clients.size();
    }

    public void log(String message) {
        String time = LocalDateTime.now().format(FORMATTER);
        System.out.println("[" + time + "] " + message);
    }

    public static void main(String[] args) {
        new ServeurPrincipal().demarrer();
    }
}