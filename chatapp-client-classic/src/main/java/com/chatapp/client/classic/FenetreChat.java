package com.chatapp.client.classic;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.Socket;
import java.util.Base64;
import com.chatapp.protocol.Protocol;
import com.chatapp.audio.GestionnaireAudioUDP;


public class FenetreChat extends JFrame {
    private JTextArea zoneConversation;
    private JTextField champMessage;
    private JButton boutonEnvoyer;
    private JButton boutonAppelAudio;
    private JButton boutonAppelVideo;
    private JButton boutonFichier;
    private DefaultListModel<String> modeleUtilisateurs;
    private JList<String> listeUtilisateurs;

    private String monPseudo;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    private boolean enAppel = false;
    private GestionnaireAudioUDP gestionnaireAudio;
    private String interlocuteurAppel;

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

        // Zone de texte pour la conversation
        zoneConversation = new JTextArea();
        zoneConversation.setEditable(false);
        this.add(new JScrollPane(zoneConversation), BorderLayout.CENTER);

        // Liste des utilisateurs connectés
        modeleUtilisateurs = new DefaultListModel<>();
        listeUtilisateurs = new JList<>(modeleUtilisateurs);
        JScrollPane scrollUsers = new JScrollPane(listeUtilisateurs);
        scrollUsers.setPreferredSize(new Dimension(150, 0));
        scrollUsers.setBorder(BorderFactory.createTitledBorder("Connectés"));
        this.add(scrollUsers, BorderLayout.EAST);

        // Zone d'envoi
        JPanel panneauBas = new JPanel(new BorderLayout());
        JPanel panneauBoutons = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        
        boutonAppelAudio = new JButton("\ud83d\udcde Audio");
        boutonAppelAudio.setEnabled(false);
        boutonAppelAudio.addActionListener(e -> lancerAppel("AUDIO"));
        panneauBoutons.add(boutonAppelAudio);
        
        boutonAppelVideo = new JButton("\ud83d\udcf9 Vidéo");
        boutonAppelVideo.setEnabled(false);
        boutonAppelVideo.addActionListener(e -> lancerAppel("VIDEO"));
        panneauBoutons.add(boutonAppelVideo);
        
        boutonFichier = new JButton("\ud83d\udcc4 Fichier");
        boutonFichier.setEnabled(false);
        boutonFichier.addActionListener(e -> envoyerFichier());
        panneauBoutons.add(boutonFichier);
        
        panneauBas.add(panneauBoutons, BorderLayout.WEST);
        
        champMessage = new JTextField();
        boutonEnvoyer = new JButton("Envoyer");
        panneauBas.add(champMessage, BorderLayout.CENTER);
        panneauBas.add(boutonEnvoyer, BorderLayout.EAST);
        this.add(panneauBas, BorderLayout.SOUTH);

        boutonEnvoyer.addActionListener(e -> envoyerMessage());
        champMessage.addActionListener(e -> envoyerMessage());
        
        listeUtilisateurs.addListSelectionListener(e -> {
            boolean sel = listeUtilisateurs.getSelectedIndex() != -1 && !enAppel;
            boutonAppelAudio.setEnabled(sel);
            boutonAppelVideo.setEnabled(sel);
            boutonFichier.setEnabled(sel && listeUtilisateurs.getSelectedIndex() != -1);
        });

        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                seDeconnecter();
            }
        });

        this.setVisible(true);
        new Thread(new EcouteurServeur()).start(); // Thread de réception temps réel
    }

    private void envoyerMessage() {
        String texte = champMessage.getText();
        String destinataire = listeUtilisateurs.getSelectedValue();

        if (destinataire != null && !texte.isEmpty()) {
            // Format de Lamiae : MSG|destinataire|contenu
            out.println(Protocol.MSG + Protocol.SEP + destinataire + Protocol.SEP + texte);
            zoneConversation.append("Moi -> " + destinataire + " : " + texte + "\n");
            champMessage.setText("");
        } else if (destinataire == null) {
            JOptionPane.showMessageDialog(this, "Sélectionnez un destinataire.");
        }
    }

    private void lancerAppel(String type) {
        String destinataire = listeUtilisateurs.getSelectedValue();
        if (destinataire == null) {
            JOptionPane.showMessageDialog(this, "Sélectionnez un utilisateur.");
            return;
        }
        if (enAppel) {
            JOptionPane.showMessageDialog(this, "Déjà en appel.");
            return;
        }
        enAppel = true;
        interlocuteurAppel = destinataire;
        out.println(Protocol.CALL_REQUEST + Protocol.SEP + monPseudo + Protocol.SEP + destinataire + Protocol.SEP + type);
        zoneConversation.append("Appel " + type.toLowerCase() + " lancé vers " + destinataire + "...\n");
        boutonAppelAudio.setEnabled(false);
        boutonAppelVideo.setEnabled(false);
        boutonFichier.setEnabled(false);
    }

    private void envoyerFichier() {
        String destinataire = listeUtilisateurs.getSelectedValue();
        if (destinataire == null) return;
        
        JFileChooser chooser = new JFileChooser();
        int res = chooser.showOpenDialog(this);
        if (res != JFileChooser.APPROVE_OPTION) return;
        
        File file = chooser.getSelectedFile();
        try {
            byte[] bytes = java.nio.file.Files.readAllBytes(file.toPath());
            String b64 = Base64.getEncoder().encodeToString(bytes);
            out.println(Protocol.FILE + Protocol.SEP + destinataire + Protocol.SEP + file.getName() + Protocol.SEP + b64);
            zoneConversation.append("Moi -> " + destinataire + " : [Fichier envoyé: " + file.getName() + "]\n");
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Erreur lecture fichier: " + ex.getMessage());
        }
    }

    private void repondreAppel(String caller, String callType) {
        SwingUtilities.invokeLater(() -> {
            int choix = JOptionPane.showConfirmDialog(this,
                    caller + " vous appelle en " + callType + ". Accepter ?",
                    "Appel entrant",
                    JOptionPane.YES_NO_OPTION);
            if (choix == JOptionPane.YES_OPTION) {
                enAppel = true;
                interlocuteurAppel = caller;
                out.println(Protocol.CALL_ACCEPT + Protocol.SEP + caller + Protocol.SEP + monPseudo);
                zoneConversation.append("Appel " + callType.toLowerCase() + " accepté avec " + caller + "\n");
                boutonAppelAudio.setEnabled(false);
                boutonAppelVideo.setEnabled(false);
                boutonFichier.setEnabled(false);
                if ("AUDIO".equalsIgnoreCase(callType)) {
                    demarrerAudio(interlocuteurAppel);
                } else {
                    zoneConversation.append("[VIDEO] TODO: Démarrer transmission vidéo UDP\n");
                }
            } else {
                out.println(Protocol.CALL_REJECT + Protocol.SEP + caller + Protocol.SEP + monPseudo);
                enAppel = false;
                interlocuteurAppel = null;
                zoneConversation.append("Appel refusé avec " + caller + "\n");
            }
        });
    }

    private String makeSessionId(String u1, String u2) {
        return u1.compareTo(u2) < 0 ? u1 + ":" + u2 : u2 + ":" + u1;
    }

    private void demarrerAudio(String autre) {
        try {
            String session = makeSessionId(monPseudo, autre);
            if (gestionnaireAudio != null) gestionnaireAudio.arreter();
            gestionnaireAudio = new GestionnaireAudioUDP();
            gestionnaireAudio.demarrer(socket.getInetAddress().getHostAddress(), session);
            zoneConversation.append("[AUDIO] Transmission UDP démarrée (" + session + ")\n");
        } catch (Exception e) {
            zoneConversation.append("[AUDIO ERREUR] " + e.getMessage() + "\n");
        }
    }

    private void arreterAudio() {
        if (gestionnaireAudio != null) {
            gestionnaireAudio.arreter();
            gestionnaireAudio = null;
            zoneConversation.append("[AUDIO] Transmission UDP arrêtée.\n");
        }
    }

    private void terminerAppel() {
        enAppel = false;
        interlocuteurAppel = null;
        boolean sel = listeUtilisateurs.getSelectedIndex() != -1;
        boutonAppelAudio.setEnabled(sel);
        boutonAppelVideo.setEnabled(sel);
        boutonFichier.setEnabled(sel);
        zoneConversation.append("Appel terminé.\n");
        arreterAudio();
    }

    private void seDeconnecter() {
        try {
            if (enAppel) {
                if (interlocuteurAppel != null) {
                    out.println(Protocol.CALL_END + Protocol.SEP + monPseudo + Protocol.SEP + interlocuteurAppel);
                }
                arreterAudio();
            }
            out.println(Protocol.LOGOUT + Protocol.SEP + monPseudo);
            socket.close();
            System.exit(0);
        } catch (IOException ex) { ex.printStackTrace(); }
    }

    private class EcouteurServeur implements Runnable {
        public void run() {
            try {
                String line;
                while ((line = in.readLine()) != null) {
                    String[] parts = line.split("\\" + Protocol.SEP, -1);
                    String type = parts[0];

                    switch (type) {
                        case Protocol.USER_LIST:
                            SwingUtilities.invokeLater(() -> {
                                modeleUtilisateurs.clear();
                                if (parts.length > 1) {
                                    for (String u : parts[1].split(",")) modeleUtilisateurs.addElement(u);
                                }
                            });
                            break;
                        case Protocol.MSG_RECV:
                            zoneConversation.append(parts[1] + " : " + parts[2] + "\n");
                            break;
                        case Protocol.USER_JOINED:
                            modeleUtilisateurs.addElement(parts[1]);
                            zoneConversation.append("--- " + parts[1] + " a rejoint le chat ---\n");
                            break;
                        case Protocol.USER_LEFT:
                            modeleUtilisateurs.removeElement(parts[1]);
                            zoneConversation.append("--- " + parts[1] + " a quitté le chat ---\n");
                            break;

                        case Protocol.CALL_INCOMING:
                            if (parts.length >= 3) {
                                String caller = parts[1];
                                String callType = parts[2];
                                zoneConversation.append("Appel entrant de " + caller + " (" + callType + ")\n");
                                repondreAppel(caller, callType);
                            }
                            break;

                        case Protocol.CALL_ACCEPTED:
                            if (parts.length >= 4) {
                                String acceptor = parts[1];
                                int audioPort = Integer.parseInt(parts[2]);
                                int videoPort = Integer.parseInt(parts[3]);
                                zoneConversation.append("Appel accepté par " + acceptor + " (ports UDP: " + audioPort + "/" + videoPort + ")\n");
                                enAppel = true;
                                boutonAppelAudio.setEnabled(false);
                                boutonAppelVideo.setEnabled(false);
                                boutonFichier.setEnabled(false);
                                demarrerAudio(acceptor);
                            }
                            break;

                        case Protocol.CALL_REJECTED:
                            if (parts.length >= 2) {
                                String refuser = parts[1];
                                zoneConversation.append("Appel refusé par " + refuser + "\n");
                                enAppel = false;
                                interlocuteurAppel = null;
                                boolean sel = listeUtilisateurs.getSelectedIndex() != -1;
                                boutonAppelAudio.setEnabled(sel);
                                boutonAppelVideo.setEnabled(sel);
                                boutonFichier.setEnabled(sel);
                            }
                            break;

                        case Protocol.CALL_ENDED:
                            if (parts.length >= 2) {
                                String terminePar = parts[1];
                                zoneConversation.append("Appel terminé par " + terminePar + "\n");
                                terminerAppel();
                            }
                            break;

                        case Protocol.FILE_RECV:
                            if (parts.length >= 4) {
                                String envoyeur = parts[1];
                                String nomFichier = parts[2];
                                String b64 = parts[3];
                                zoneConversation.append(envoyeur + " a envoyé un fichier: " + nomFichier + "\n");
                                int rep = JOptionPane.showConfirmDialog(FenetreChat.this,
                                        "Recevoir le fichier " + nomFichier + " de " + envoyeur + " ?",
                                        "Fichier reçu", JOptionPane.YES_NO_OPTION);
                                if (rep == JOptionPane.YES_OPTION) {
                                    JFileChooser save = new JFileChooser();
                                    save.setSelectedFile(new File(nomFichier));
                                    if (save.showSaveDialog(FenetreChat.this) == JFileChooser.APPROVE_OPTION) {
                                        try (FileOutputStream fos = new FileOutputStream(save.getSelectedFile())) {
                                            fos.write(Base64.getDecoder().decode(b64));
                                            zoneConversation.append("[Fichier sauvegardé: " + save.getSelectedFile().getAbsolutePath() + "]\n");
                                        } catch (IOException ex) {
                                            JOptionPane.showMessageDialog(FenetreChat.this, "Erreur sauvegarde: " + ex.getMessage());
                                        }
                                    }
                                }
                            }
                            break;
                    }
                }
            } catch (IOException e) { zoneConversation.append("Connexion perdue.\n"); }
        }
    }
}

