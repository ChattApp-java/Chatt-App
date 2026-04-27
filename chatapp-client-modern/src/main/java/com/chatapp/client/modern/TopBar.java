package com.chatapp.client.modern;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Barre superieure WhatsApp avec:
 *  - Avatar + nom + statut dynamique
 *  - Boutons appel audio/video
 *  - Separator
 */
public class TopBar extends JPanel {

    public final JButton btnAudio;
    public final JButton btnVideo;
    private final JLabel statusLabel;
    private final JPanel dot;

    public TopBar(String user) {
        setLayout(new BorderLayout(12, 0));
        setBackground(UIConstants.BG_PANEL);
        setBorder(new EmptyBorder(10, 16, 10, 16));

        // Gauche: avatar + nom + statut
        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.X_AXIS));
        leftPanel.setOpaque(false);

        JPanel avatar = Utils.makeAvatar(user, 40);
        leftPanel.add(avatar);
        leftPanel.add(Box.createHorizontalStrut(12));

        JPanel nameCol = new JPanel();
        nameCol.setLayout(new BoxLayout(nameCol, BoxLayout.Y_AXIS));
        nameCol.setOpaque(false);

        JLabel nameLabel = new JLabel(user);
        nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 15));
        nameLabel.setForeground(UIConstants.TEXT_PRIMARY);
        nameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel statusRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        statusRow.setOpaque(false);

        dot = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(UIConstants.ONLINE_GREEN);
                g2.fillOval(0, 3, 8, 8);
                g2.dispose();
            }
        };
        dot.setOpaque(false);
        dot.setPreferredSize(new Dimension(8, 14));

        statusLabel = Utils.styledLabel("En ligne", Font.PLAIN, 11, UIConstants.ONLINE_GREEN);

        statusRow.add(dot);
        statusRow.add(statusLabel);

        nameCol.add(nameLabel);
        nameCol.add(statusRow);
        leftPanel.add(nameCol);

        add(leftPanel, BorderLayout.WEST);

        // Droite: boutons appel
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btnPanel.setOpaque(false);

        btnAudio = Utils.pillButton("Appel", UIConstants.BG_SURFACE, 10);
        btnAudio.setForeground(UIConstants.TEXT_PRIMARY);
        btnAudio.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnAudio.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIConstants.BORDER_COLOR, 1, true),
                new EmptyBorder(6, 14, 6, 14)
        ));

        btnVideo = Utils.pillButton("Video", UIConstants.ACCENT, 10);
        btnVideo.setBorder(new EmptyBorder(7, 15, 7, 15));

        btnPanel.add(btnAudio);
        btnPanel.add(btnVideo);
        add(btnPanel, BorderLayout.EAST);
    }

    public void setStatus(String text, Color color) {
        statusLabel.setText(text);
        statusLabel.setForeground(color);
        dot.setVisible(!text.equals("Hors ligne"));
        revalidate(); repaint();
    }

    public void setTyping() {
        setStatus("en train d'ecrire...", UIConstants.ACCENT);
    }

    public void setOnline() {
        setStatus("En ligne", UIConstants.ONLINE_GREEN);
    }

    public void setOffline() {
        setStatus("Hors ligne", UIConstants.TEXT_MUTED);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setColor(UIConstants.DIVIDER);
        g2.setStroke(new BasicStroke(1f));
        g2.drawLine(0, getHeight() - 1, getWidth(), getHeight() - 1);
        g2.dispose();
    }
}

