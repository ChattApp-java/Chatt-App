import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.Socket;
import protocol.Protocol;

public class FenetreChat extends JFrame {
    private JTextArea zoneConversation;
    private JTextField champMessage;
    private JButton boutonEnvoyer;
    private DefaultListModel<String> modeleUtilisateurs;
    private JList<String> listeUtilisateurs;

    private String monPseudo;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    public FenetreChat(String pseudo, Socket socket, PrintWriter out, BufferedReader in) {
        this.monPseudo = pseudo;
        this.socket = socket;
        this.out = out;
        this.in = in;

        this.setTitle("ChatApp ENSA - " + monPseudo);
        this.setSize(600, 400);
        this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        this.setLocationRelativeTo(null);
        this.setLayout(new BorderLayout());

        // Zone de texte pour la conversation [cite: 385]
        zoneConversation = new JTextArea();
        zoneConversation.setEditable(false);
        this.add(new JScrollPane(zoneConversation), BorderLayout.CENTER);

        // Liste des utilisateurs connectés [cite: 381, 382]
        modeleUtilisateurs = new DefaultListModel<>();
        listeUtilisateurs = new JList<>(modeleUtilisateurs);
        JScrollPane scrollUsers = new JScrollPane(listeUtilisateurs);
        scrollUsers.setPreferredSize(new Dimension(150, 0));
        scrollUsers.setBorder(BorderFactory.createTitledBorder("Connectés"));
        this.add(scrollUsers, BorderLayout.EAST);

        // Zone d'envoi [cite: 383]
        JPanel panneauBas = new JPanel(new BorderLayout());
        champMessage = new JTextField();
        boutonEnvoyer = new JButton("Envoyer");
        panneauBas.add(champMessage, BorderLayout.CENTER);
        panneauBas.add(boutonEnvoyer, BorderLayout.EAST);
        this.add(panneauBas, BorderLayout.SOUTH);

        boutonEnvoyer.addActionListener(e -> envoyerMessage());
        champMessage.addActionListener(e -> envoyerMessage());

        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                seDeconnecter(); // Gérer déconnexion [cite: 387]
            }
        });

        this.setVisible(true);
        new Thread(new EcouteurServeur()).start(); // Thread de réception temps réel [cite: 384]
    }

    private void envoyerMessage() {
        String texte = champMessage.getText();
        String destinataire = listeUtilisateurs.getSelectedValue();

        if (destinataire != null && !texte.isEmpty()) {
            // Format de Lamiae : MSG|destinataire|contenu [cite: 383]
            out.println(Protocol.MSG + Protocol.SEP + destinataire + Protocol.SEP + texte);
            zoneConversation.append("Moi -> " + destinataire + " : " + texte + "\n");
            champMessage.setText("");
        } else if (destinataire == null) {
            JOptionPane.showMessageDialog(this, "Sélectionnez un destinataire.");
        }
    }

    private void seDeconnecter() {
        try {
            out.println(Protocol.LOGOUT + Protocol.SEP + monPseudo); // [cite: 387]
            socket.close();
            System.exit(0);
        } catch (IOException ex) { ex.printStackTrace(); }
    }

    private class EcouteurServeur implements Runnable {
        public void run() {
            try {
                String line;
                while ((line = in.readLine()) != null) {
                    String[] parts = line.split("\\" + Protocol.SEP);
                    String type = parts[0];

                    switch (type) {
                        case Protocol.USER_LIST: // Mise à jour liste complète [cite: 381]
                            SwingUtilities.invokeLater(() -> {
                                modeleUtilisateurs.clear();
                                if (parts.length > 1) {
                                    for (String u : parts[1].split(",")) modeleUtilisateurs.addElement(u);
                                }
                            });
                            break;
                        case Protocol.MSG_RECV: // Réception message [cite: 384]
                            zoneConversation.append(parts[1] + " : " + parts[2] + "\n");
                            break;
                        case Protocol.USER_JOINED: // Nouvel arrivant [cite: 382]
                            modeleUtilisateurs.addElement(parts[1]);
                            zoneConversation.append("--- " + parts[1] + " a rejoint le chat ---\n");
                            break;
                        case Protocol.USER_LEFT: // Un départ [cite: 387]
                            modeleUtilisateurs.removeElement(parts[1]);
                            zoneConversation.append("--- " + parts[1] + " a quitté le chat ---\n");
                            break;
                    }
                }
            } catch (IOException e) { zoneConversation.append("Connexion perdue.\n"); }
        }
    }
}