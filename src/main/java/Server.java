import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;


public class Server {


    private final ConcurrentHashMap<String, ClientHandler> clients
            = new ConcurrentHashMap<>();

    private ServerSocket serverSocket;

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    public void start() {
        try {
            // 1. Identification du port + mise en écoute
            serverSocket = new ServerSocket(Protocol.PORT_SIGNALING);

            log("╔══════════════════════════════════════════╗");
            log("║     Serveur CHATAPP-like démarré        ║");
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


    public void registerClient(ClientHandler handler) {
        clients.put(handler.getUsername(), handler);
        log("Utilisateur enregistré : " + handler.getUsername()
                + " | Total connectés : " + clients.size());
    }

    /**
     * Supprime un client déconnecté.
     */
    public void removeClient(ClientHandler handler) {
        if (handler.getUsername() != null) {
            clients.remove(handler.getUsername());
            log("Utilisateur supprimé : " + handler.getUsername()
                    + " | Total connectés : " + clients.size());
        }
    }

    public boolean isUsernameTaken(String username) {
        return clients.containsKey(username);
    }


    public ClientHandler getClient(String username) {
        return clients.get(username);
    }


    public String getConnectedUsernames() {
        return clients.keySet().stream()
                .collect(Collectors.joining(","));
    }


    public void broadcast(String message, ClientHandler exclude) {
        int count = 0;
        for (ClientHandler handler : clients.values()) {
            if (handler != exclude) {
                handler.sendMessage(message);
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

