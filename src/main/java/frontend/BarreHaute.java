package frontend;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Barre supérieure avec avatar, nom et boutons d'appel.
 */
public class BarreHaute extends JPanel {

    public final JButton btnAudio;
    public final JButton btnVideo;

    public BarreHaute(String user) {
        setLayout(new BorderLayout(12, 0));
        setBackground(Constantes.BG_PANEL);
        setBorder(new EmptyBorder(12, 18, 12, 18));

        // LEFT: Avatar + Name + Status
        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.X_AXIS));
        leftPanel.setOpaque(false);

        JPanel avatar = Utilitaires.makeAvatar(user, 40);
        leftPanel.add(avatar);
        leftPanel.add(Box.createHorizontalStrut(12));

        JPanel nameCol = new JPanel();
        nameCol.setLayout(new BoxLayout(nameCol, BoxLayout.Y_AXIS));
        nameCol.setOpaque(false);

        JLabel nameLabel = new JLabel(user);
        nameLabel.setFont(new Font("SansSerif", Font.BOLD, 15));
        nameLabel.setForeground(Constantes.TEXT_PRIMARY);
        nameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel statusRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        statusRow.setOpaque(false);

        JPanel dot = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Constantes.ONLINE_GREEN);
                g2.fillOval(0, 3, 8, 8);
                g2.dispose();
            }
        };
        dot.setOpaque(false);
        dot.setPreferredSize(new Dimension(8, 14));

        JLabel statusLabel = Utilitaires.styledLabel("En ligne", Font.PLAIN, 11, Constantes.ONLINE_GREEN);
        statusRow.add(dot);
        statusRow.add(statusLabel);

        nameCol.add(nameLabel);
        nameCol.add(statusRow);
        leftPanel.add(nameCol);

        add(leftPanel, BorderLayout.WEST);

        // RIGHT: Call buttons
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btnPanel.setOpaque(false);

        btnAudio = Utilitaires.pillButton("📞  Audio", Constantes.BG_SURFACE, 10);
        btnAudio.setForeground(Constantes.TEXT_PRIMARY);
        btnAudio.setFont(new Font("SansSerif", Font.PLAIN, 13));
        btnAudio.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Constantes.BORDER_COLOR, 1, true),
                new EmptyBorder(6, 14, 6, 14)
        ));

        btnVideo = Utilitaires.pillButton("📹  Vidéo", Constantes.ACCENT, 10);
        btnVideo.setBorder(new EmptyBorder(7, 15, 7, 15));

        btnPanel.add(btnAudio);
        btnPanel.add(btnVideo);
        add(btnPanel, BorderLayout.EAST);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setColor(Constantes.BORDER_COLOR);
        g2.setStroke(new BasicStroke(1f));
        g2.drawLine(0, getHeight() - 1, getWidth(), getHeight() - 1);
        g2.dispose();
    }
}