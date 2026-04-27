package client;

import commun.Protocole;

import java.io.*;
import java.net.Socket;

/**
 * Gère la connexion réseau au serveur principal.
 */
public class ConnexionServeur {

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private String username;

    public String connecterEtRecupererStatut(String pseudo, String serveurIp) throws Exception {
        this.socket = new Socket(serveurIp, Protocole.PORT_SIGNALING);
        this.out = new PrintWriter(socket.getOutputStream(), true);
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        out.println(Protocole.LOGIN + Protocole.SEP + pseudo);

        String reponse = in.readLine();
        if (reponse != null) {
            if (reponse.startsWith(Protocole.LOGIN_OK)) {
                this.username = pseudo;
                return "OK";
            } else if (reponse.startsWith(Protocole.LOGIN_FAIL)) {
                String[] parts = reponse.split("\\" + Protocole.SEP);
                return (parts.length > 1) ? parts[1] : "Échec du login";
            }
        }
        socket.close();
        return "Serveur ne répond pas correctement";
    }

    public boolean connecter(String pseudo, String serveurIp) throws Exception {
        this.socket = new Socket(serveurIp, Protocole.PORT_SIGNALING);
        this.out = new PrintWriter(socket.getOutputStream(), true);
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        out.println(Protocole.LOGIN + Protocole.SEP + pseudo);

        String reponse = in.readLine();
        if (reponse != null && reponse.startsWith(Protocole.LOGIN_OK)) {
            this.username = pseudo;
            return true;
        }
        socket.close();
        return false;
    }

    public void envoyer(String message) {
        if (out != null) {
            out.println(message);
        }
    }

    public String lire() throws IOException {
        return in.readLine();
    }

    public void deconnecter() {
        try {
            if (out != null) out.println(Protocole.LOGOUT + Protocole.SEP + username);
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException ignored) {}
    }

    public boolean estConnecte() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }

    public String getUsername() { return username; }
    public PrintWriter getOut() { return out; }
    public BufferedReader getIn() { return in; }
    public Socket getSocket() { return socket; }
}