package frontend;

import client.ConnexionServeur;
import client.EcouteurMessages;
import client.GestionnaireAppelClient;
import commun.Protocole;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Fenêtre principale de chat unifiée.
 */
public class FenetreChat extends JFrame {

    private final String username;
    private final ConnexionServeur connexion;
    private final PanneauChat panneauChat;
    private final BarreEnvoi barreEnvoi;
    private final DefaultListModel<String> modeleUtilisateurs;
    private final JList<String> listeUtilisateurs;
    private final GestionnaireAppelClient gestionnaireAppel;

    private String utilisateurSelectionne = null;

    public FenetreChat(String pseudo, ConnexionServeur connexion) {
        this.username = pseudo;
        this.connexion = connexion;
        this.gestionnaireAppel = new GestionnaireAppelClient();

        setTitle("ChatApp - " + pseudo);
        setSize(900, 650);
        setMinimumSize(new Dimension(600, 400));
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // TopBar
        BarreHaute barreHaute = new BarreHaute(pseudo);
        barreHaute.btnAudio.addActionListener(e -> initierAppel("AUDIO"));
        barreHaute.btnVideo.addActionListener(e -> initierAppel("VIDEO"));
        add(barreHaute, BorderLayout.NORTH);

        // Liste utilisateurs
        modeleUtilisateurs = new DefaultListModel<>();
        listeUtilisateurs = new JList<>(modeleUtilisateurs);
        listeUtilisateurs.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        listeUtilisateurs.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                utilisateurSelectionne = listeUtilisateurs.getSelectedValue();
            }
        });

        JPanel panneauUtilisateurs = new JPanel(new BorderLayout());
        panneauUtilisateurs.setPreferredSize(new Dimension(180, 0));
        panneauUtilisateurs.setBackground(Constantes.BG_PANEL);
        panneauUtilisateurs.setBorder(new EmptyBorder(10, 10, 10, 10));

        JLabel lblUtilisateurs = new JLabel("Connectés");
        lblUtilisateurs.setForeground(Constantes.TEXT_PRIMARY);
        lblUtilisateurs.setFont(new Font("SansSerif", Font.BOLD, 14));
        panneauUtilisateurs.add(lblUtilisateurs, BorderLayout.NORTH);
        panneauUtilisateurs.add(new JScrollPane(listeUtilisateurs), BorderLayout.CENTER);
        add(panneauUtilisateurs, BorderLayout.WEST);

        // Zone chat
        panneauChat = new PanneauChat();
        add(panneauChat, BorderLayout.CENTER);

        // Barre d'envoi
        barreEnvoi = new BarreEnvoi(this::envoyerMessage);
        add(barreEnvoi, BorderLayout.SOUTH);

        // Déconnexion
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { seDeconnecter(); }
        });

        // Message de bienvenue
        panneauChat.addSystemMessage("Bienvenue " + pseudo + " 👋");

        // Démarrer l'écouteur
        new Thread(new EcouteurMessages(connexion, this)).start();

        setVisible(true);
    }

    private void envoyerMessage() {
        String msg = barreEnvoi.getMessage().trim();
        if (msg.isEmpty()) return;

        if (utilisateurSelectionne == null) {
            JOptionPane.showMessageDialog(this, "Sélectionnez un destinataire.");
            return;
        }

        connexion.envoyer(Protocole.MSG + Protocole.SEP + utilisateurSelectionne + Protocole.SEP + msg);
        String time = new SimpleDateFormat("HH:mm").format(new Date());
        panneauChat.addMessage(username, msg, time, true);
        barreEnvoi.clear();
    }

    private void initierAppel(String type) {
        if (utilisateurSelectionne == null) {
            JOptionPane.showMessageDialog(this, "Sélectionnez un contact pour appeler.");
            return;
        }

        connexion.envoyer(Protocole.CALL_REQUEST + Protocole.SEP + username + Protocole.SEP
                + utilisateurSelectionne + Protocole.SEP + type);

        if (type.equals("AUDIO")) {
            new FenetreAppelAudio(utilisateurSelectionne, "localhost");
        } else {
            new FenetreAppelVideo(username, utilisateurSelectionne, "localhost");
        }
    }

    private void seDeconnecter() {
        connexion.deconnecter();
        System.exit(0);
    }

    // Méthodes appelées par EcouteurMessages
    public void mettreAJourListe(String[] utilisateurs) {
        modeleUtilisateurs.clear();
        for (String u : utilisateurs) {
            if (!u.equals(username)) modeleUtilisateurs.addElement(u);
        }
    }

    public void ajouterMessage(String sender, String text, boolean mine) {
        String time = new SimpleDateFormat("HH:mm").format(new Date());
        panneauChat.addMessage(sender, text, time, mine);
    }

    public void ajouterMessageSysteme(String text) {
        panneauChat.addSystemMessage(text);
    }

    public void ajouterUtilisateur(String user) {
        if (!user.equals(username) && !modeleUtilisateurs.contains(user)) {
            modeleUtilisateurs.addElement(user);
        }
    }

    public void retirerUtilisateur(String user) {
        modeleUtilisateurs.removeElement(user);
    }

    public void gererAppelEntrant(String caller, String typeAppel) {
        DialogueAppelEntrant popup = new DialogueAppelEntrant(this, caller, typeAppel);
        popup.setVisible(true);

        if (popup.isAccepte()) {
            connexion.envoyer(Protocole.CALL_ACCEPT + Protocole.SEP + caller + Protocole.SEP + username);
            if (typeAppel.equalsIgnoreCase("VIDEO")) {
                new FenetreAppelVideo(username, caller, "localhost");
            } else {
                new FenetreAppelAudio(caller, "localhost");
            }
        } else {
            connexion.envoyer(Protocole.CALL_REJECT + Protocole.SEP + caller + Protocole.SEP + username);
        }
    }

    public void appelAccepte(String[] parts) {
        JOptionPane.showMessageDialog(this, "Appel accepté ! Ports: " + parts[2] + ", " + parts[3]);
    }

    public void appelRefuse(String refuser) {
        JOptionPane.showMessageDialog(this, "Appel refusé par " + refuser);
    }

    public void appelTermine() {
        if (gestionnaireAppel.estEnAppel()) {
            gestionnaireAppel.terminerAppel();
        }
        JOptionPane.showMessageDialog(this, "Appel terminé.");
    }
}