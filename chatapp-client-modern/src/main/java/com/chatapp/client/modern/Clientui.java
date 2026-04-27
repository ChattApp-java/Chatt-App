package com.chatapp.client.modern;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Base64;
import com.chatapp.protocol.Protocol;
import com.chatapp.audio.GestionnaireAudioUDP;
import com.chatapp.audio.CapteurAudio;
import javax.sound.sampled.*;

public class Clientui {

    private String monPseudo;
    private String currentUser = null;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    private boolean enAppel = false;
    private GestionnaireAudioUDP gestionnaireAudio;
    private String interlocuteurAppel;

    private JFrame frame;
    private ChatPanel chatPanel;
    private BottomBar bottomBar;
    private TopBar topBar;
    private DefaultListModel<String> modeleUtilisateurs;
    private JList<String> listeUtilisateurs;
    private JPanel centerPanel;

    private final Map<String, ArrayList<MsgEntry>> history = new HashMap<>();
    private boolean isTyping = false;

    static class MsgEntry {
        String sender, text, time, status;
        boolean mine;
        String audioData;
        MsgEntry(String s, String t, String tm, boolean m, String st) {
            sender = s; text = t; time = tm; mine = m; status = st;
        }
        MsgEntry(String s, String t, String tm, boolean m, String st, String audio) {
            sender = s; text = t; time = tm; mine = m; status = st; audioData = audio;
        }
    }

    public Clientui(String pseudo, Socket socket, PrintWriter out, BufferedReader in) {
        this.monPseudo = pseudo;
        this.socket = socket;
        this.out = out;
        this.in = in;

        frame = new JFrame("ChatApp - " + monPseudo);
        frame.setSize(950, 650);
        frame.setMinimumSize(new Dimension(700, 500));
        frame.setLayout(new BorderLayout());
        frame.getContentPane().setBackground(UIConstants.BG_DARK);

        modeleUtilisateurs = new DefaultListModel<>();
        listeUtilisateurs = new JList<>(modeleUtilisateurs);
        listeUtilisateurs.setBackground(UIConstants.BG_PANEL);
        listeUtilisateurs.setForeground(UIConstants.TEXT_PRIMARY);
        listeUtilisateurs.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        listeUtilisateurs.setSelectionBackground(UIConstants.BG_SURFACE);
        listeUtilisateurs.setSelectionForeground(UIConstants.TEXT_PRIMARY);
        listeUtilisateurs.setFixedCellHeight(48);
        listeUtilisateurs.setBorder(new EmptyBorder(8, 8, 8, 8));

        listeUtilisateurs.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                label.setBorder(new EmptyBorder(4, 4, 4, 4));
                label.setIconTextGap(10);
                if (!isSelected) label.setBackground(UIConstants.BG_PANEL);
                return label;
            }
        });

        JScrollPane scrollUsers = new JScrollPane(listeUtilisateurs);
        scrollUsers.setPreferredSize(new Dimension(220, 0));
        scrollUsers.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, UIConstants.DIVIDER));
        frame.add(scrollUsers, BorderLayout.WEST);

        centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBackground(UIConstants.BG_DARK);

        topBar = new TopBar("Selectionnez un contact");
        centerPanel.add(topBar, BorderLayout.NORTH);

        chatPanel = new ChatPanel();
        centerPanel.add(chatPanel, BorderLayout.CENTER);

        EmojiPicker emojiPicker = new EmojiPicker(emoji -> bottomBar.appendText(emoji));

        bottomBar = new BottomBar(this::sendMessage, this::toggleAudioRecording, this::envoyerFichier, (JComponent c) -> {
            emojiPicker.show(c, 0, -emojiPicker.getPreferredSize().height);
        });
        centerPanel.add(bottomBar, BorderLayout.SOUTH);

        frame.add(centerPanel, BorderLayout.CENTER);

        chatPanel.addSystemMessage("Bienvenue " + monPseudo + " 👋");

        listeUtilisateurs.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) selectUser(listeUtilisateurs.getSelectedValue());
        });

        topBar.btnAudio.addActionListener(ae -> lancerAppel("AUDIO"));
        topBar.btnVideo.addActionListener(ae -> lancerAppel("VIDEO"));

        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { seDeconnecter(); }
        });
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        new Thread(new EcouteurServeur()).start();
    }

    private void selectUser(String user) {
        currentUser = user;
        if (currentUser == null) return;

        topBar = new TopBar(currentUser);
        topBar.btnAudio.addActionListener(ae -> lancerAppel("AUDIO"));
        topBar.btnVideo.addActionListener(ae -> lancerAppel("VIDEO"));
        centerPanel.add(topBar, BorderLayout.NORTH);
        centerPanel.revalidate(); centerPanel.repaint();

        chatPanel.clear();
        ArrayList<MsgEntry> msgs = history.getOrDefault(currentUser, new ArrayList<>());
        for (MsgEntry m : msgs) {
            chatPanel.addMessage(m.sender, m.text, m.time, m.mine, m.status, m.audioData);
        }
        if (msgs.isEmpty()) {
            chatPanel.addSystemMessage("Debut de la conversation avec " + currentUser);
        }

        out.println(Protocol.MSG_READ + Protocol.SEP + currentUser + Protocol.SEP + monPseudo);
    }

    private void sendMessage() {
        if (currentUser == null) {
            JOptionPane.showMessageDialog(frame, "Selectionnez un destinataire.");
            return;
        }
        String msg = bottomBar.getMessage().trim();
        if (msg.isEmpty()) return;
        String time = new SimpleDateFormat("HH:mm").format(new Date());
        out.println(Protocol.MSG + Protocol.SEP + currentUser + Protocol.SEP + msg);

        MsgEntry entry = new MsgEntry(monPseudo, msg, time, true, Protocol.STATUS_SENT);
        history.computeIfAbsent(currentUser, k -> new ArrayList<>()).add(entry);
        chatPanel.addMessage(monPseudo, msg, time, true, Protocol.STATUS_SENT);
        bottomBar.clear();

        if (isTyping) {
            isTyping = false;
            out.println(Protocol.TYPING_STOP + Protocol.SEP + currentUser);
        }
    }

    private boolean isRecording = false;
    private CapteurAudio capteurAudio;
    private ByteArrayOutputStream audioBuffer;

    private void toggleAudioRecording() {
        if (!isRecording) {
            try {
                capteurAudio = new CapteurAudio();
                capteurAudio.demarrer();
                audioBuffer = new ByteArrayOutputStream();
                isRecording = true;
                chatPanel.addSystemMessage("🎤 Enregistrement vocal demarre... (cliquez a nouveau pour arreter)");

                new Thread(() -> {
                    while (isRecording && capteurAudio.estActif()) {
                        byte[] chunk = capteurAudio.lireChunk();
                        try { audioBuffer.write(chunk); } catch (IOException ignored) {}
                    }
                }).start();
            } catch (Exception e) {
                JOptionPane.showMessageDialog(frame, "Erreur microphone: " + e.getMessage());
            }
        } else {
            isRecording = false;
            if (capteurAudio != null) capteurAudio.arreter();

            if (currentUser == null) {
                JOptionPane.showMessageDialog(frame, "Selectionnez un destinataire.");
                return;
            }

            try {
                byte[] audioData = audioBuffer.toByteArray();
                if (audioData.length == 0) {
                    chatPanel.addSystemMessage("Enregistrement vide annule.");
                    return;
                }

                byte[] wavData = creerWav(audioData);
                String b64 = Base64.getEncoder().encodeToString(wavData);

                out.println(Protocol.AUDIO_MSG + Protocol.SEP + currentUser + Protocol.SEP + b64);
                String time = new SimpleDateFormat("HH:mm").format(new Date());
                MsgEntry entry = new MsgEntry(monPseudo, "🎤 Message vocal", time, true, Protocol.STATUS_SENT, b64);
                history.computeIfAbsent(currentUser, k -> new ArrayList<>()).add(entry);
                chatPanel.addMessage(monPseudo, "🎤 Message vocal", time, true, Protocol.STATUS_SENT, b64);
                chatPanel.addSystemMessage("Message vocal envoye a " + currentUser);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(frame, "Erreur envoi vocal: " + e.getMessage());
            }
        }
    }

    private byte[] creerWav(byte[] pcmData) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int sampleRate = (int) CapteurAudio.SAMPLE_RATE;
        short channels = (short) CapteurAudio.CHANNELS;
        short bitsPerSample = (short) CapteurAudio.SAMPLE_SIZE_BITS;
        int byteRate = sampleRate * channels * bitsPerSample / 8;

        out.write("RIFF".getBytes());
        writeInt(out, 36 + pcmData.length);
        out.write("WAVE".getBytes());
        out.write("fmt ".getBytes());
        writeInt(out, 16);
        writeShort(out, (short) 1);
        writeShort(out, channels);
        writeInt(out, sampleRate);
        writeInt(out, byteRate);
        writeShort(out, (short) (channels * bitsPerSample / 8));
        writeShort(out, bitsPerSample);
        out.write("data".getBytes());
        writeInt(out, pcmData.length);
        out.write(pcmData);
        return out.toByteArray();
    }

    private void writeInt(ByteArrayOutputStream out, int val) throws IOException {
        out.write(val & 0xFF); out.write((val >> 8) & 0xFF);
        out.write((val >> 16) & 0xFF); out.write((val >> 24) & 0xFF);
    }
    private void writeShort(ByteArrayOutputStream out, short val) throws IOException {
        out.write(val & 0xFF); out.write((val >> 8) & 0xFF);
    }

    private void lancerAppel(String type) {
        if (currentUser == null) {
            JOptionPane.showMessageDialog(frame, "Selectionnez un utilisateur.");
            return;
        }
        if (enAppel) {
            JOptionPane.showMessageDialog(frame, "Deja en appel.");
            return;
        }
        enAppel = true;
        interlocuteurAppel = currentUser;
        out.println(Protocol.CALL_REQUEST + Protocol.SEP + monPseudo + Protocol.SEP + currentUser + Protocol.SEP + type);
        chatPanel.addSystemMessage("Appel " + type.toLowerCase() + " lance vers " + currentUser + "...");
    }

    private void envoyerFichier() {
        if (currentUser == null) {
            JOptionPane.showMessageDialog(frame, "Veuillez selectionner un contact avant d'envoyer un fichier.");
            return;
        }
        JFileChooser chooser = new JFileChooser();
        int res = chooser.showOpenDialog(frame);
        if (res != JFileChooser.APPROVE_OPTION) return;
        File file = chooser.getSelectedFile();
        try {
            byte[] bytes = java.nio.file.Files.readAllBytes(file.toPath());
            String b64 = Base64.getEncoder().encodeToString(bytes);
            out.println(Protocol.FILE + Protocol.SEP + currentUser + Protocol.SEP + file.getName() + Protocol.SEP + b64);
            String time = new SimpleDateFormat("HH:mm").format(new Date());
            chatPanel.addMessage(monPseudo, "[Fichier: " + file.getName() + "]", time, true);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(frame, "Erreur lecture fichier: " + ex.getMessage());
        }
    }

    private void repondreAppel(String caller, String callType) {
        SwingUtilities.invokeLater(() -> {
            int choix = JOptionPane.showConfirmDialog(frame,
                    caller + " vous appelle en " + callType + ". Accepter ?",
                    "Appel entrant", JOptionPane.YES_NO_OPTION);
            if (choix == JOptionPane.YES_OPTION) {
                enAppel = true;
                interlocuteurAppel = caller;
                out.println(Protocol.CALL_ACCEPT + Protocol.SEP + caller + Protocol.SEP + monPseudo);
                chatPanel.addSystemMessage("Appel " + callType.toLowerCase() + " accepte avec " + caller);
                if ("AUDIO".equalsIgnoreCase(callType)) {
                    demarrerAudio(interlocuteurAppel);
                    new AudioCallUI(caller, this::terminerAppel);
                } else {
                    new VideoCallUI(monPseudo, caller, this::terminerAppel);
                }
            } else {
                out.println(Protocol.CALL_REJECT + Protocol.SEP + caller + Protocol.SEP + monPseudo);
                enAppel = false;
                interlocuteurAppel = null;
                chatPanel.addSystemMessage("Appel refuse avec " + caller);
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
            chatPanel.addSystemMessage("[AUDIO] Transmission UDP demarree (" + session + ")");
        } catch (Exception e) {
            chatPanel.addSystemMessage("[AUDIO ERREUR] " + e.getMessage());
        }
    }

    private void arreterAudio() {
        if (gestionnaireAudio != null) {
            gestionnaireAudio.arreter();
            gestionnaireAudio = null;
            chatPanel.addSystemMessage("[AUDIO] Transmission UDP arretee.");
        }
    }

    public void terminerAppel() {
        enAppel = false;
        interlocuteurAppel = null;
        arreterAudio();
        chatPanel.addSystemMessage("Appel termine.");
    }

    private void seDeconnecter() {
        try {
            if (enAppel && interlocuteurAppel != null) {
                out.println(Protocol.CALL_END + Protocol.SEP + monPseudo + Protocol.SEP + interlocuteurAppel);
            }
            arreterAudio();
            out.println(Protocol.LOGOUT + Protocol.SEP + monPseudo);
            socket.close();
        } catch (IOException ignored) {}
        System.exit(0);
    }

    private class EcouteurServeur implements Runnable {
        public void run() {
            try {
                String line;
                while ((line = in.readLine()) != null) {
                    String[] parts = line.split("\\" + Protocol.SEP, -1);
                    String type = parts[0];
                    String time = new SimpleDateFormat("HH:mm").format(new Date());

                    switch (type) {
                        case Protocol.USER_LIST:
                            SwingUtilities.invokeLater(() -> {
                                modeleUtilisateurs.clear();
                                if (parts.length > 1) {
                                    for (String u : parts[1].split(",")) {
                                        if (!u.equals(monPseudo) && !u.isEmpty()) modeleUtilisateurs.addElement(u);
                                    }
                                }
                            });
                            break;
                        case Protocol.MSG_RECV:
                            if (parts.length >= 3) {
                                String sender = parts[1];
                                String content = parts[2];
                                out.println(Protocol.MSG_ACK + Protocol.SEP + sender + Protocol.SEP + monPseudo);
                                SwingUtilities.invokeLater(() -> {
                                    MsgEntry entry = new MsgEntry(sender, content, time, false, Protocol.STATUS_READ);
                                    history.computeIfAbsent(sender, k -> new ArrayList<>()).add(entry);
                                    if (sender.equals(currentUser)) {
                                        chatPanel.addMessage(sender, content, time, false);
                                    } else {
                                        chatPanel.addSystemMessage("Message de " + sender + ": " + content.substring(0, Math.min(20, content.length())) + "...");
                                    }
                                });
                            }
                            break;
                        case Protocol.MSG_ACK:
                            if (parts.length >= 2) {
                                SwingUtilities.invokeLater(() -> {
                                    ArrayList<MsgEntry> msgs = history.get(parts[1]);
                                    if (msgs != null && !msgs.isEmpty()) {
                                        MsgEntry last = msgs.get(msgs.size() - 1);
                                        if (last.mine) last.status = Protocol.STATUS_DELIVERED;
                                    }
                                    if (parts[1].equals(currentUser)) selectUser(currentUser);
                                });
                            }
                            break;
                        case Protocol.MSG_READ:
                            if (parts.length >= 2) {
                                SwingUtilities.invokeLater(() -> {
                                    ArrayList<MsgEntry> msgs = history.get(parts[1]);
                                    if (msgs != null) {
                                        for (MsgEntry m : msgs) if (m.mine) m.status = Protocol.STATUS_READ;
                                    }
                                    if (parts[1].equals(currentUser)) selectUser(currentUser);
                                });
                            }
                            break;
                        case Protocol.TYPING_START:
                            if (parts.length >= 2 && parts[1].equals(currentUser)) {
                                SwingUtilities.invokeLater(() -> topBar.setTyping());
                            }
                            break;
                        case Protocol.TYPING_STOP:
                            if (parts.length >= 2 && parts[1].equals(currentUser)) {
                                SwingUtilities.invokeLater(() -> topBar.setOnline());
                            }
                            break;
                        case Protocol.USER_JOINED:
                            if (!parts[1].equals(monPseudo)) {
                                SwingUtilities.invokeLater(() -> modeleUtilisateurs.addElement(parts[1]));
                            }
                            chatPanel.addSystemMessage("--- " + parts[1] + " a rejoint le chat ---");
                            break;
                        case Protocol.USER_LEFT:
                            SwingUtilities.invokeLater(() -> modeleUtilisateurs.removeElement(parts[1]));
                            chatPanel.addSystemMessage("--- " + parts[1] + " a quitte le chat ---");
                            break;
                        case Protocol.CALL_INCOMING:
                            if (parts.length >= 3) repondreAppel(parts[1], parts[2]);
                            break;
                        case Protocol.CALL_ACCEPTED:
                            if (parts.length >= 4) {
                                enAppel = true;
                                chatPanel.addSystemMessage("Appel accepte par " + parts[1]);
                                demarrerAudio(parts[1]);
                            }
                            break;
                        case Protocol.CALL_REJECTED:
                            if (parts.length >= 2) {
                                enAppel = false;
                                interlocuteurAppel = null;
                                chatPanel.addSystemMessage("Appel refuse par " + parts[1]);
                            }
                            break;
                        case Protocol.CALL_ENDED:
                            if (parts.length >= 2) terminerAppel();
                            break;
                        case Protocol.AUDIO_RECV:
                            if (parts.length >= 3) {
                                String senderAudio = parts[1];
                                String b64Audio = parts[2];
                                SwingUtilities.invokeLater(() -> {
                                    MsgEntry entry = new MsgEntry(senderAudio, "🎤 Message vocal", time, false, Protocol.STATUS_READ, b64Audio);
                                    history.computeIfAbsent(senderAudio, k -> new ArrayList<>()).add(entry);
                                    if (senderAudio.equals(currentUser)) {
                                        chatPanel.addMessage(senderAudio, "🎤 Message vocal recu", time, false, Protocol.STATUS_READ, b64Audio);
                                    }
                                    chatPanel.addSystemMessage("🎤 Message vocal de " + senderAudio + " " + (senderAudio.equals(currentUser) ? "" : " (nouveau)"));
                                });
                            }
                            break;
                        case Protocol.FILE_RECV:
                            if (parts.length >= 4) {
                                String envoyeur = parts[1];
                                String nomFichier = parts[2];
                                String b64 = parts[3];
                                chatPanel.addSystemMessage(envoyeur + " a envoye: " + nomFichier);
                                int rep = JOptionPane.showConfirmDialog(frame,
                                        "Recevoir " + nomFichier + " de " + envoyeur + " ?",
                                        "Fichier recu", JOptionPane.YES_NO_OPTION);
                                if (rep == JOptionPane.YES_OPTION) {
                                    JFileChooser save = new JFileChooser();
                                    save.setSelectedFile(new File(nomFichier));
                                    if (save.showSaveDialog(frame) == JFileChooser.APPROVE_OPTION) {
                                        try (FileOutputStream fos = new FileOutputStream(save.getSelectedFile())) {
                                            fos.write(Base64.getDecoder().decode(b64));
                                            chatPanel.addSystemMessage("[Fichier sauvegarde: " + save.getSelectedFile().getAbsolutePath() + "]");
                                        } catch (IOException ex) {
                                            JOptionPane.showMessageDialog(frame, "Erreur sauvegarde: " + ex.getMessage());
                                        }
                                    }
                                }
                            }
                            break;
                    }
                }
            } catch (IOException e) {
                SwingUtilities.invokeLater(() -> chatPanel.addSystemMessage("Connexion perdue."));
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new LoginScreen((user, socket, out, in) -> new Clientui(user, socket, out, in)));
    }
}
