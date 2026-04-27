import javafx.application.Platform;
import java.io.*;
import java.net.Socket;
import java.util.Arrays;
import java.util.function.Consumer;

public class ClientReseau {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private Consumer<String> onLoginSuccess;
    private Consumer<String[]> onUserList;
    private Consumer<String> onMsgRecv;

    public void setActions(Consumer<String> success, Consumer<String[]> users, Consumer<String> msg) {
        this.onLoginSuccess = success;
        this.onUserList = users;
        this.onMsgRecv = msg;
    }

    public void connecter(String nom) {
        try {
            socket = new Socket("localhost", Protocol.PORT_SIGNALING);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            out.println(Protocol.LOGIN + Protocol.SEP + nom);
            new Thread(this::ecouter).start();
        } catch (IOException e) {
            System.err.println("Erreur : Serveur introuvable");
        }
    }

    private void ecouter() {
        try {
            String ligne;
            while ((ligne = in.readLine()) != null) {
                String[] parts = ligne.split(java.util.regex.Pattern.quote(Protocol.SEP));
                if (parts[0].equals(Protocol.LOGIN_OK)) {
                    Platform.runLater(() -> onLoginSuccess.accept(parts[1]));
                } else if (parts[0].equals(Protocol.USER_LIST)) {
                    Platform.runLater(() -> onUserList.accept(Arrays.copyOfRange(parts, 1, parts.length)));
                } else if (parts[0].equals(Protocol.MSG_RECV)) {
                    Platform.runLater(() -> onMsgRecv.accept(parts[1] + " : " + parts[2]));
                }
            }
        } catch (IOException e) { e.printStackTrace(); }
    }

    public void envoyer(String dest, String msg) {
        if (out != null) out.println(Protocol.MSG + Protocol.SEP + dest + Protocol.SEP + msg);
    }
}