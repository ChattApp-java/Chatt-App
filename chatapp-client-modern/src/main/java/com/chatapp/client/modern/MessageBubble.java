package com.chatapp.client.modern;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.Base64;
import javax.sound.sampled.*;
import com.chatapp.protocol.Protocol;

/**
 * Bulle de message style WhatsApp avec :
 *  - Avatar (reçu seulement)
 *  - Queue de bulle (triangle)
 *  - Heure + ticks de statut
 *  - Bouton lecture pour messages vocaux
 *  - Menu contextuel (clic droit)
 */
public class MessageBubble extends JPanel {

    private static final int AVATAR_SIZE = 36;
    private static final int ARC         = 14;
    private static final int MAX_WIDTH   = 320;

    private final boolean mine;
    private String status = Protocol.STATUS_SENT; // SENT, DELIVERED, READ

    public MessageBubble(String sender, String text, String time, boolean mine) {
        this(sender, text, time, mine, Protocol.STATUS_SENT, null);
    }

    public MessageBubble(String sender, String text, String time, boolean mine, String status) {
        this(sender, text, time, mine, status, null);
    }

    public MessageBubble(String sender, String text, String time, boolean mine, String status, String audioBase64) {
        this.mine = mine;
        this.status = status;

        setLayout(new BorderLayout(6, 0));
        setOpaque(false);
        setBorder(new EmptyBorder(2, 4, 2, 4));

        // Avatar pour messages reçus
        if (!mine) {
            JPanel avatar = Utils.makeAvatar(sender, AVATAR_SIZE);
            JPanel avatarWrap = new JPanel(new BorderLayout());
            avatarWrap.setOpaque(false);
            avatarWrap.add(avatar, BorderLayout.SOUTH);
            add(avatarWrap, BorderLayout.WEST);
        }

        // Bulle
        JPanel bubble = createBubble(sender, text, time, audioBase64);
        add(bubble, BorderLayout.CENTER);

        // Spacer droite pour messages reçus
        if (!mine) {
            add(Box.createHorizontalStrut(40), BorderLayout.EAST);
        }

        // Menu contextuel
        JPopupMenu menu = new JPopupMenu();
        menu.setBackground(UIConstants.BG_SURFACE);
        menu.setBorder(BorderFactory.createLineBorder(UIConstants.BORDER_COLOR));
        JMenuItem replyItem = new JMenuItem("Repondre");
        JMenuItem copyItem = new JMenuItem("Copier");
        JMenuItem deleteItem = new JMenuItem("Supprimer");
        replyItem.setForeground(UIConstants.TEXT_PRIMARY);
        copyItem.setForeground(UIConstants.TEXT_PRIMARY);
        deleteItem.setForeground(UIConstants.TEXT_PRIMARY);
        replyItem.setBackground(UIConstants.BG_SURFACE);
        copyItem.setBackground(UIConstants.BG_SURFACE);
        deleteItem.setBackground(UIConstants.BG_SURFACE);
        menu.add(replyItem);
        menu.add(copyItem);
        menu.addSeparator();
        menu.add(deleteItem);

        bubble.addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) { if (e.isPopupTrigger()) menu.show(e.getComponent(), e.getX(), e.getY()); }
            @Override public void mouseReleased(MouseEvent e) { if (e.isPopupTrigger()) menu.show(e.getComponent(), e.getX(), e.getY()); }
        });
    }

    private JPanel createBubble(String sender, String text, String time, String audioBase64) {
        JPanel bubble = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                Color bg = mine ? UIConstants.MSG_SENT : UIConstants.MSG_RECV;
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth() - 2, getHeight() - 2, ARC, ARC);

                // Petite queue de bulle (triangle)
                if (!mine) {
                    int[] xs = {0, 0, 8};
                    int[] ys = {10, 22, 16};
                    g2.fillPolygon(xs, ys, 3);
                } else {
                    int[] xs = {getWidth() - 2, getWidth() - 2, getWidth() - 10};
                    int[] ys = {10, 22, 16};
                    g2.fillPolygon(xs, ys, 3);
                }

                g2.dispose();
            }
        };
        bubble.setLayout(new BoxLayout(bubble, BoxLayout.Y_AXIS));
        bubble.setOpaque(false);
        bubble.setBorder(new EmptyBorder(6, 10, 6, 10));

        // Nom de l'expediteur (reçu uniquement)
        if (!mine) {
            JLabel nameLabel = Utils.styledLabel(sender, Font.BOLD, 11, UIConstants.ACCENT);
            nameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            bubble.add(nameLabel);
            bubble.add(Box.createVerticalStrut(2));
        }

        boolean isAudio = audioBase64 != null && !audioBase64.isEmpty();

        if (isAudio) {
            // Ligne avec bouton play pour audio
            JPanel audioRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
            audioRow.setOpaque(false);
            audioRow.setAlignmentX(Component.LEFT_ALIGNMENT);

            JButton btnPlay = new JButton("▶");
            btnPlay.setFont(new Font("Segoe UI", Font.BOLD, 16));
            btnPlay.setForeground(UIConstants.TEXT_PRIMARY);
            btnPlay.setBackground(UIConstants.BG_SURFACE);
            btnPlay.setFocusPainted(false);
            btnPlay.setBorder(BorderFactory.createLineBorder(UIConstants.BORDER_COLOR, 1, true));
            btnPlay.setPreferredSize(new Dimension(36, 36));
            btnPlay.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            btnPlay.addActionListener(e -> jouerAudio(audioBase64));

            JLabel lblAudio = new JLabel("Message vocal");
            lblAudio.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            lblAudio.setForeground(UIConstants.TEXT_PRIMARY);

            audioRow.add(btnPlay);
            audioRow.add(lblAudio);
            bubble.add(audioRow);
        } else {
            // Texte du message normal
            String safe = Utils.escapeHtml(text);
            JLabel msg = new JLabel("<html><body style='width:" + MAX_WIDTH + "px; font-family:Segoe UI; font-size:14px;'>" + safe + "</body></html>");
            msg.setForeground(UIConstants.TEXT_PRIMARY);
            msg.setAlignmentX(Component.LEFT_ALIGNMENT);
            bubble.add(msg);
        }

        // Ligne heure + ticks
        JPanel bottomRow = new JPanel(new FlowLayout(mine ? FlowLayout.RIGHT : FlowLayout.LEFT, 4, 0));
        bottomRow.setOpaque(false);
        bottomRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel timeLabel = Utils.styledLabel(time, Font.PLAIN, 10, UIConstants.TEXT_MUTED);
        bottomRow.add(timeLabel);

        // Ticks WhatsApp pour messages envoyés
        if (mine) {
            boolean isRead = Protocol.STATUS_READ.equals(status);
            bottomRow.add(new JPanel() {
                @Override protected void paintComponent(Graphics g) {
                    Utils.paintCheckMarks((Graphics2D) g, 0, 2, 8, isRead);
                }
                @Override public Dimension getPreferredSize() { return new Dimension(16, 12); }
                @Override public Dimension getMinimumSize() { return new Dimension(16, 12); }
            });
        }

        bubble.add(Box.createVerticalStrut(2));
        bubble.add(bottomRow);

        return bubble;
    }

    private void jouerAudio(String b64) {
        new Thread(() -> {
            try {
                byte[] wav = Base64.getDecoder().decode(b64);
                File temp = File.createTempFile("audio", ".wav");
                temp.deleteOnExit();
                try (FileOutputStream fos = new FileOutputStream(temp)) {
                    fos.write(wav);
                }
                AudioInputStream ais = AudioSystem.getAudioInputStream(temp);
                Clip clip = AudioSystem.getClip();
                clip.open(ais);
                clip.start();
                clip.addLineListener(event -> {
                    if (event.getType() == LineEvent.Type.STOP) {
                        clip.close();
                    }
                });
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(SwingUtilities.getWindowAncestor(this),
                        "Erreur lecture audio: " + ex.getMessage(), "Audio", JOptionPane.ERROR_MESSAGE);
            }
        }).start();
    }

    public void setStatus(String status) {
        this.status = status;
        repaint();
    }
}

