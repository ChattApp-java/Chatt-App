import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.Socket;
import protocol.Protocol;

public class FenetreConnexion extends JFrame {
    private JTextField champSaisiePseudo;
    private JButton boutonConnexion;

    public FenetreConnexion() {
        this.setTitle("Connexion ChatApp ");
        this.setSize(550, 550);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setLocationRelativeTo(null);
        this.setLayout(new FlowLayout());

        this.add(new JLabel("Entrez votre pseudo :"));
        champSaisiePseudo = new JTextField(15);
        boutonConnexion = new JButton("Se connecter");

        this.add(champSaisiePseudo);
        this.add(boutonConnexion);

        boutonConnexion.addActionListener(e -> {
            String pseudoSaisi = champSaisiePseudo.getText();
            if (!pseudoSaisi.isEmpty()) {
                try {
                    // Connexion au serveur via Socket TCP
                    Socket socket = new Socket("localhost", 5000);
                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                    // Envoi du pseudo selon le format de Lamiae : LOGIN|pseudo
                    out.println(Protocol.LOGIN + Protocol.SEP + pseudoSaisi);

                    String reponse = in.readLine();

                    // Le serveur répond LOGIN_OK|pseudo en cas de succès
                    if (reponse != null && reponse.startsWith(Protocol.LOGIN_OK)) {
                        this.dispose();
                        new FenetreChat(pseudoSaisi, socket, out, in); // Ouverture du chat [cite: 385]
                    } else if (reponse != null && reponse.startsWith(Protocol.LOGIN_FAIL)) {
                        String[] parts = reponse.split("\\" + Protocol.SEP);
                        String raison = (parts.length > 1) ? parts[1] : "Erreur inconnue";
                        JOptionPane.showMessageDialog(this, "Refus : " + raison);
                        socket.close();
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "Serveur injoignable : " + ex.getMessage());
                }
            }
        });
        this.setVisible(true);
    }
}