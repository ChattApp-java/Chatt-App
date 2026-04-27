package serveur;

import commun.Protocole;

import java.io.*;
import java.net.Socket;

/**
 * Gère la communication avec un client connecté.
 */
public class GestionnaireClient implements Runnable {

    private final Socket socket;
    private final ServeurPrincipal serveur;
    private final base_de_donnees.DAOUtilisateur daoUser;
    private String username;

    private BufferedReader in;
    private PrintWriter out;

    public GestionnaireClient(Socket socket, ServeurPrincipal serveur) {
        this.socket = socket;
        this.serveur = serveur;
        this.daoUser = new base_de_donnees.DAOUtilisateur();
    }

    @Override
    public void run() {
        try {
            in  = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);

            serveur.log("Buffer prêt pour : " + socket.getInetAddress().getHostAddress());

            String line;
            while ((line = in.readLine()) != null) {
                traiterMessage(line.trim());
            }

        } catch (IOException e) {
            serveur.log("Connexion perdue : " + (username != null ? username : socket.getInetAddress().getHostAddress()));
        } finally {
            deconnecter();
        }
    }

    private void traiterMessage(String raw) {
        if (raw == null || raw.isEmpty()) return;

        String[] parts = raw.split("\\" + Protocole.SEP, -1);
        String type = parts[0];

        serveur.log("REÇU de [" + (username != null ? username : "?") + "] : " + raw);

        switch (type) {

            case Protocole.LOGIN: {
                if (parts.length < 2) break;
                String name = parts[1].trim();

                if (name.isEmpty() || serveur.isUsernameTaken(name)) {
                    envoyerMessage(Protocole.LOGIN_FAIL + Protocole.SEP + "Nom d'utilisateur indisponible");
                    serveur.log("LOGIN refusé pour : " + name);
                    return;
                }

                this.username = name;
                serveur.enregistrerClient(this);
                daoUser.logAction(username, "CONNEXION");

                envoyerMessage(Protocole.LOGIN_OK + Protocole.SEP + username);
                envoyerMessage(Protocole.USER_LIST + Protocole.SEP + serveur.getConnectedUsernames());
                serveur.broadcast(Protocole.USER_JOINED + Protocole.SEP + username, this);

                serveur.log("LOGIN OK : " + username + " | Connectés : " + serveur.getClientCount());
                break;
            }

            case Protocole.LOGOUT: {
                serveur.log("LOGOUT demandé par : " + username);
                deconnecter();
                break;
            }

            case Protocole.MSG: {
                if (parts.length < 3) break;
                String dest = parts[1];
                String content = parts[2];

                GestionnaireClient target = serveur.getClient(dest);
                if (target != null) {
                    target.envoyerMessage(Protocole.MSG_RECV + Protocole.SEP + username + Protocole.SEP + content);
                    serveur.log("MSG routé : " + username + " → " + dest);
                } else {
                    serveur.log("MSG non livré : destinataire [" + dest + "] introuvable");
                }
                break;
            }

            case Protocole.CALL_REQUEST: {
                if (parts.length < 4) break;
                String caller = parts[1];
                String callee = parts[2];
                String callType = parts[3];

                GestionnaireClient target = serveur.getClient(callee);
                if (target != null) {
                    target.envoyerMessage(Protocole.CALL_INCOMING + Protocole.SEP + caller + Protocole.SEP + callType);
                    serveur.log("CALL_REQUEST : " + caller + " → " + callee + " [" + callType + "]");
                } else {
                    envoyerMessage(Protocole.CALL_REJECTED + Protocole.SEP + callee);
                    serveur.log("CALL_REQUEST échoué : " + callee + " hors ligne");
                }
                break;
            }

            case Protocole.CALL_ACCEPT: {
                if (parts.length < 3) break;
                String caller = parts[1];
                String acceptor = parts[2];

                GestionnaireClient callerHandler = serveur.getClient(caller);
                if (callerHandler != null) {
                    callerHandler.envoyerMessage(
                            Protocole.CALL_ACCEPTED + Protocole.SEP
                                    + acceptor + Protocole.SEP
                                    + Protocole.PORT_AUDIO + Protocole.SEP
                                    + Protocole.PORT_VIDEO);
                    serveur.log("CALL accepté : " + acceptor + " accepte l'appel de " + caller);
                }
                break;
            }

            case Protocole.CALL_REJECT: {
                if (parts.length < 3) break;
                String caller = parts[1];
                String refuser = parts[2];

                GestionnaireClient callerHandler = serveur.getClient(caller);
                if (callerHandler != null) {
                    callerHandler.envoyerMessage(Protocole.CALL_REJECTED + Protocole.SEP + refuser);
                    serveur.log("CALL refusé : " + refuser + " refuse l'appel de " + caller);
                }
                break;
            }

            case Protocole.CALL_END: {
                if (parts.length < 3) break;
                String other = parts[2];

                GestionnaireClient otherHandler = serveur.getClient(other);
                if (otherHandler != null) {
                    otherHandler.envoyerMessage(Protocole.CALL_ENDED + Protocole.SEP + username);
                    serveur.log("CALL terminé entre " + username + " et " + other);
                }
                break;
            }

            case Protocole.FILE_INFO: {
                if (parts.length < 5) break;
                String dest = parts[1];
                GestionnaireClient target = serveur.getClient(dest);
                if (target != null) {
                    target.envoyerMessage(raw); // Relayer l'info complète
                    serveur.log("FILE_INFO routé de " + username + " vers " + dest);
                }
                break;
            }

            default:
                serveur.log("Message inconnu reçu de [" + username + "] : " + type);
        }
    }

    public synchronized void envoyerMessage(String message) {
        if (out != null) {
            out.println(message);
            serveur.log("ENVOYÉ à [" + (username != null ? username : "?") + "] : " + message);
        }
    }

    private void deconnecter() {
        if (username != null) {
            daoUser.logAction(username, "DÉCONNEXION");
            serveur.supprimerClient(this);
            serveur.broadcast(Protocole.USER_LEFT + Protocole.SEP + username, this);
            serveur.log("Déconnexion de : " + username);
            username = null;
        }
        try {
            if (!socket.isClosed()) socket.close();
        } catch (IOException ignored) {}
    }

    public String getUsername() {
        return username;
    }
}