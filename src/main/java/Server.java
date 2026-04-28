import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentHashMap;

public class Server {

    // Liste Thread-Safe des clients connectés (Clé: Username, Valeur: ClientHandler)
    private final ConcurrentHashMap<String, ClientHandler> clients = new ConcurrentHashMap<>();
    private ServerSocket serverSocket;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    public void start() {
        try {
            // 1. Identification du port + mise en écoute
            serverSocket = new ServerSocket(Protocol.PORT_SIGNALING);

            log("╔══════════════════════════════════════════╗");
            log("║     Serveur CHATAPP-like démarré         ║");
            log("║  Port TCP (signalisation) : " + Protocol.PORT_SIGNALING + "         ║");
            log("║  Port UDP (audio)         : " + Protocol.PORT_AUDIO  + "         ║");
            log("║  Port UDP (vidéo)         : " + Protocol.PORT_VIDEO  + "         ║");
            log("╚══════════════════════════════════════════╝");

            // 2. Boucle d'acceptation
            acceptLoop();

        } catch (IOException e) {
            log("ERREUR démarrage serveur : " + e.getMessage());
        }
    }

    private void acceptLoop() {
        log("En attente de connexions...\n");

        while (!serverSocket.isClosed()) {
            try {
                // 3. Accepter la connexion d'un client
                Socket clientSocket = serverSocket.accept();

                String ip = clientSocket.getInetAddress().getHostAddress();
                log("Nouvelle connexion entrante depuis : " + ip);

                // 4. Créer un thread dédié pour ce client
                ClientHandler handler = new ClientHandler(clientSocket, this);
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

    // --- SYNCHRONISATION AVEC LE CLIENTHANDLER ---

    public void registerClient(ClientHandler handler) {
        if (handler.getNomUtilisateurConnecte() != null) {
            clients.put(handler.getNomUtilisateurConnecte(), handler);
            log("Utilisateur enregistré : " + handler.getNomUtilisateurConnecte()
                    + " | Total connectés : " + clients.size());
        }
    }

    public void removeClient(ClientHandler handler) {
        if (handler.getNomUtilisateurConnecte() != null) {
            clients.remove(handler.getNomUtilisateurConnecte());
            log("Utilisateur supprimé : " + handler.getNomUtilisateurConnecte()
                    + " | Total connectés : " + clients.size());
        }
    }

    /**
     * NOUVELLE MÉTHODE : Cherche le destinataire dans la HashMap et lui envoie le message.
     */
    public boolean envoyerAClient(String destinataire, String messageFormate) {
        ClientHandler client = clients.get(destinataire);
        if (client != null) {
            client.envoyerMessage(messageFormate);
            return true; // Le message a bien été transmis
        }
        return false; // L'utilisateur n'est pas connecté à ce serveur
    }

    // ---------------------------------------------

    public boolean isUsernameTaken(String username) {
        return clients.containsKey(username);
    }

    public ClientHandler getClient(String username) {
        return clients.get(username);
    }

    public String getConnectedUsernames() {
        return String.join(",", clients.keySet());
    }

    public void broadcast(String message, ClientHandler exclude) {
        int count = 0;
        for (ClientHandler handler : clients.values()) {
            if (handler != exclude) {
                handler.envoyerMessage(message); // Corrigé : sendMessage -> envoyerMessage
                count++;
            }
        }
        log("Broadcast envoyé à " + count + " client(s) : " + message);
    }

    public int getClientCount() {
        return clients.size();
    }

    public void log(String message) {
        String time = LocalDateTime.now().format(FORMATTER);
        System.out.println("[" + time + "] " + message);
    }

    public static void main(String[] args) {
        new Server().start();
    }
}