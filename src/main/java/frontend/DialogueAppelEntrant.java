package frontend;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Dialogue de notification d'appel entrant.
 */
public class DialogueAppelEntrant extends JDialog {

    private boolean accepte = false;

    public DialogueAppelEntrant(JFrame parent, String contact, String typeAppel) {
        super(parent, "Appel entrant", true);
        setUndecorated(true);
        setSize(340, 210);
        setLocationRelativeTo(parent);

        JPanel root = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Constantes.BG_SURFACE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                g2.setColor(Constantes.BORDER_COLOR);
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 20, 20);
                g2.dispose();
            }
        };
        root.setOpaque(false);
        root.setBorder(new EmptyBorder(24, 24, 20, 24));
        setContentPane(root);
        getContentPane().setBackground(new Color(0,0,0,0));

        // TOP: Icon + call type label
        JPanel topRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        topRow.setOpaque(false);

        String typeIcon = typeAppel.equalsIgnoreCase("Vidéo") ? "📹" : "📞";
        JLabel iconLabel = new JLabel(typeIcon);
        iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 22));

        JPanel typeCol = new JPanel();
        typeCol.setLayout(new BoxLayout(typeCol, BoxLayout.Y_AXIS));
        typeCol.setOpaque(false);

        JLabel incoming = Utilitaires.styledLabel("Appel " + typeAppel, Font.PLAIN, 11, Constantes.TEXT_MUTED);
        JLabel contactLabel = new JLabel(contact);
        contactLabel.setFont(new Font("SansSerif", Font.BOLD, 17));
        contactLabel.setForeground(Constantes.TEXT_PRIMARY);

        typeCol.add(incoming);
        typeCol.add(contactLabel);

        topRow.add(iconLabel);
        topRow.add(typeCol);
        root.add(topRow, BorderLayout.NORTH);

        // CENTER: Sub-label
        JPanel center = new JPanel(new FlowLayout(FlowLayout.LEFT));
        center.setOpaque(false);
        JLabel sub = Utilitaires.styledLabel("souhaite vous parler…", Font.ITALIC, 13, Constantes.TEXT_MUTED);
        center.add(sub);
        root.add(center, BorderLayout.CENTER);

        // BOTTOM: Buttons
        JPanel btnPanel = new JPanel(new GridLayout(1, 2, 12, 0));
        btnPanel.setOpaque(false);
        btnPanel.setBorder(new EmptyBorder(4, 0, 0, 0));

        JButton btnRefuser = Utilitaires.dangerButton("✖  Refuser");
        JButton btnAccepter = Utilitaires.successButton("✔  Accepter");

        btnRefuser.setBorder(new EmptyBorder(10, 0, 10, 0));
        btnAccepter.setBorder(new EmptyBorder(10, 0, 10, 0));

        btnRefuser.addActionListener(e -> { accepte = false; dispose(); });
        btnAccepter.addActionListener(e -> { accepte = true; dispose(); });

        btnPanel.add(btnRefuser);
        btnPanel.add(btnAccepter);
        root.add(btnPanel, BorderLayout.SOUTH);

        setBackground(new Color(0, 0, 0, 0));
    }

    public boolean isAccepte() { return accepte; }
}