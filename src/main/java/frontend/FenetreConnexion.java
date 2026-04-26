package frontend;

import client.ConnexionServeur;
import commun.Protocole;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * Fenêtre de connexion initiale.
 */
public class FenetreConnexion extends JFrame {

    private JTextField champSaisiePseudo;
    private JButton boutonConnexion;

    public FenetreConnexion() {
        setTitle("Connexion ChatApp");
        setSize(400, 200);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        JPanel center = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 20));
        center.add(new JLabel("Pseudo :"));
        champSaisiePseudo = new JTextField(15);
        center.add(champSaisiePseudo);
        add(center, BorderLayout.CENTER);

        boutonConnexion = new JButton("Se connecter");
        boutonConnexion.addActionListener(e -> connecter());

        JPanel south = new JPanel();
        south.add(boutonConnexion);
        add(south, BorderLayout.SOUTH);

        champSaisiePseudo.addActionListener(e -> connecter());

        setVisible(true);
    }

    private void connecter() {
        String pseudo = champSaisiePseudo.getText().trim();
        if (pseudo.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Veuillez entrer un pseudo.");
            return;
        }

        try {
            ConnexionServeur connexion = new ConnexionServeur();
            if (connexion.connecter(pseudo, "localhost")) {
                dispose();
                new FenetreChat(pseudo, connexion);
            } else {
                JOptionPane.showMessageDialog(this, "Connexion refusée. Pseudo peut-être déjà pris.");
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Serveur injoignable : " + ex.getMessage());
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(FenetreConnexion::new);
    }
}