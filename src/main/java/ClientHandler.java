import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientHandler implements Runnable {

    private final Socket clientSocket;
    private final Server server;
    private BufferedReader in;
    private PrintWriter out;

    private String nomUtilisateurConnecte;

    // Instanciation des DAO pour gérer la base de données
    private final UserDAO userDAO = new UserDAO();
    private final MessageDAO messageDAO = new MessageDAO();

    public ClientHandler(Socket socket, Server server) {
        this.clientSocket = socket;
        this.server = server;
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            out = new PrintWriter(clientSocket.getOutputStream(), true);

            String messageRecu;

            // Boucle principale d'écoute du client
            while ((messageRecu = in.readLine()) != null) {

                // Utilisation de votre ProtocolParser pour découper le message de manière sécurisée
                String[] parts = ProtocolParser.parse(messageRecu);

                if (parts.length == 0) continue;

                // Récupération sécurisée du type de commande
                String type = ProtocolParser.getPart(parts, 0);

                switch (type) {

                    case Protocol.LOGIN:
                        // Format attendu: LOGIN|username|password
                        if (ProtocolParser.hasMinParts(parts, 3)) {
                            String username = ProtocolParser.getPart(parts, 1);
                            String password = ProtocolParser.getPart(parts, 2);
                            gererLogin(username, password);
                        }
                        break;

                    case Protocol.MSG:
                        // Format attendu: MSG|expediteur|destinataire|contenu
                        if (ProtocolParser.hasMinParts(parts, 4)) {
                            String sender = ProtocolParser.getPart(parts, 1);
                            String dest = ProtocolParser.getPart(parts, 2);
                            String content = ProtocolParser.getPart(parts, 3);

                            routeMessage(dest, sender, content);
                        }
                        break;

                    case Protocol.LOGOUT:
                        fermerConnexion();
                        break;

                    case Protocol.HISTORY_REQ:
                        // Format attendu: HISTORY_REQ|user1|user2
                        if (ProtocolParser.hasMinParts(parts, 3)) {
                            String user1 = ProtocolParser.getPart(parts, 1);
                            String user2 = ProtocolParser.getPart(parts, 2);

                            server.log("Demande d'historique reçue pour " + user1 + " et " + user2);
                            // Logique d'historique à implémenter ici si besoin
                        }
                        break;

                    default:
                        server.log("Commande non reconnue ou non gérée : " + type);
                        break;
                }
            }
        } catch (IOException e) {
            server.log("Erreur avec le client : " + e.getMessage());
        } finally {
            fermerConnexion();
        }
    }

    /**
     * Gère l'authentification d'un client
     */
    private void gererLogin(String username, String password) {
        User user = userDAO.authentifier(username, password);

        if (user != null) {
            this.nomUtilisateurConnecte = user.getUsername();

            // On s'enregistre dans la HashMap du serveur pour pouvoir recevoir des messages !
            server.registerClient(this);

            // On utilise ProtocolParser.build() pour envoyer la réponse proprement
            envoyerMessage(ProtocolParser.build(Protocol.LOGIN_OK, "Bienvenue"));
            server.log(username + " s'est connecté au serveur TCP.");

        } else {
            envoyerMessage(ProtocolParser.build(Protocol.LOGIN_FAIL, "Identifiants incorrects"));
        }
    }

    /**
     * Sauvegarde le message en base de données et le route vers le bon destinataire
     */
    private void routeMessage(String dest, String sender, String content) {

        User senderUser = userDAO.getUserByUsername(sender);
        User destUser = userDAO.getUserByUsername(dest);

        if (senderUser != null && destUser != null) {

            // 1. Création de l'objet et Sauvegarde en BDD
            Message msg = new Message(senderUser.getUsername(), destUser.getUsername(), content);
            int msgId = messageDAO.sauvegarderMessage(msg);

            if (msgId > 0) {
                server.log("[DB] MSG #" + msgId + " enregistré : " + sender + " -> " + dest);

                // 2. Confirmer à l'expéditeur que le serveur a bien reçu (Un tick / STATUS_SENT)
                envoyerMessage(ProtocolParser.build(Protocol.MSG_ACK, String.valueOf(msgId), Protocol.STATUS_SENT));
            }

            // 3. Formater le message à envoyer au destinataire avec le parseur
            String messageAEnvoyer = ProtocolParser.build(Protocol.MSG, sender, dest, content);

            // 4. Envoyer au destinataire via la méthode du Server
            boolean recu = server.envoyerAClient(dest, messageAEnvoyer);

            if (recu) {
                // 5. Mettre à jour la base de données et informer l'expéditeur (Deux ticks / STATUS_DELIVERED)
                messageDAO.marquerTousLus(destUser.getUsername(), senderUser.getUsername());
                envoyerMessage(ProtocolParser.build(Protocol.MSG_STATUS, String.valueOf(msgId), Protocol.STATUS_DELIVERED));
            }

        } else {
            // Utilisateur introuvable
            envoyerMessage(ProtocolParser.build(Protocol.MSG_STATUS, "-1", "FAIL_USER_NOT_FOUND"));
        }
    }

    /**
     * Envoie une ligne de texte brute dans le socket du client
     */
    public void envoyerMessage(String message) {
        if (out != null) {
            out.println(message);
        }
    }

    /**
     * Gère la déconnexion et libère les ressources proprement
     */
    private void fermerConnexion() {
        try {
            if (nomUtilisateurConnecte != null) {
                userDAO.deconnecter(nomUtilisateurConnecte);

                // On se retire de la HashMap du serveur pour ne plus recevoir de messages
                server.removeClient(this);

                server.log(nomUtilisateurConnecte + " s'est déconnecté.");
            }
            if (in != null) in.close();
            if (out != null) out.close();
            if (clientSocket != null && !clientSocket.isClosed()) clientSocket.close();
        } catch (IOException e) {
            server.log("Erreur lors de la fermeture de la connexion : " + e.getMessage());
        }
    }

    public String getNomUtilisateurConnecte() {
        return nomUtilisateurConnecte;
    }
}