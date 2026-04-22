package projet.java;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;

/**
 * Video call window with:
 *  - Two side-by-side video panels (placeholder)
 *  - Camera-off placeholder with avatar initials
 *  - Bottom control bar: Mute / Cam / Hang-up
 */
public class VideoCallUI extends JFrame {

    private JLabel ecranMoi;
    private JLabel ecranAutre;
    private boolean camOn   = true;
    private boolean micOn   = true;

    public VideoCallUI(String me, String other) {
        setTitle("Appel Vidéo");
        setSize(820, 520);
        setResizable(true);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        getContentPane().setBackground(UIConstants.BG_DARK);
        setLayout(new BorderLayout(0, 0));
        setLocationRelativeTo(null);

        // ── Video panels ─────────────────────────────────────
        JPanel videosPanel = new JPanel(new GridLayout(1, 2, 3, 0));
        videosPanel.setBackground(UIConstants.BG_DARK);

        JPanel myPanel    = makeVideoPanel(me,    true);
        JPanel otherPanel = makeVideoPanel(other, false);

        JPanel holderMoi = (JPanel) myPanel.getClientProperty("label");
        ecranMoi = (JLabel) holderMoi.getClientProperty("label");

        JPanel holderAutre = (JPanel) otherPanel.getClientProperty("label");
        ecranAutre = (JLabel) holderAutre.getClientProperty("label");

        videosPanel.add(otherPanel); // other is bigger / primary
        videosPanel.add(myPanel);

        add(videosPanel, BorderLayout.CENTER);

        // ── Controls bar ─────────────────────────────────────
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.CENTER, 16, 0));
        controls.setBackground(UIConstants.BG_PANEL);
        controls.setBorder(new EmptyBorder(14, 0, 14, 0));

        JButton btnMic = Utils.pillButton("🎙️  Micro", UIConstants.BG_SURFACE, 50);
        btnMic.setBorder(new EmptyBorder(10, 18, 10, 18));
        btnMic.addActionListener(e -> {
            micOn = !micOn;
            btnMic.setText(micOn ? "🎙️  Micro" : "🔇  Micro");
        });

        JButton btnCam = Utils.pillButton("📷  Caméra", UIConstants.BG_SURFACE, 50);
        btnCam.setBorder(new EmptyBorder(10, 18, 10, 18));
        btnCam.addActionListener(e -> {
            camOn = !camOn;
            btnCam.setText(camOn ? "📷  Caméra" : "🚫  Caméra");
        });

        JButton btnEnd = Utils.pillButton("📵  Terminer", UIConstants.DANGER, 50);
        btnEnd.setBorder(new EmptyBorder(10, 24, 10, 24));
        btnEnd.addActionListener(e -> dispose());

        controls.add(btnMic);
        controls.add(btnCam);
        controls.add(btnEnd);
        add(controls, BorderLayout.SOUTH);

        setVisible(true);
    }

    private JPanel makeVideoPanel(String name, boolean isMe) {
        // Outer dark panel
        JPanel outer = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                // Subtle gradient
                GradientPaint gp = new GradientPaint(
                        0, 0, new Color(22, 23, 40),
                        0, getHeight(), new Color(10, 10, 20)
                );
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        outer.setOpaque(false);

        // Avatar placeholder (replaced by real camera feed later)
        JPanel centre = new JPanel();
        centre.setLayout(new BoxLayout(centre, BoxLayout.Y_AXIS));
        centre.setOpaque(false);

        JPanel avatar = Utils.makeAvatar(name, 72);
        avatar.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel nameLabel = Utils.styledLabel(
                name + (isMe ? "  (Vous)" : ""), Font.BOLD, 15, UIConstants.TEXT_PRIMARY
        );
        nameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        centre.add(Box.createVerticalGlue());
        centre.add(avatar);
        centre.add(Box.createVerticalStrut(14));
        centre.add(nameLabel);
        centre.add(Box.createVerticalGlue());

        outer.add(centre, BorderLayout.CENTER);

        // Name overlay at top-left
        JLabel overlay = Utils.styledLabel(name, Font.BOLD, 12, UIConstants.TEXT_MUTED);
        overlay.setBorder(new EmptyBorder(8, 10, 0, 0));
        outer.add(overlay, BorderLayout.NORTH);

        // Store label ref in client property for getEcranMoi/Autre
        JPanel labelHolder = new JPanel();
        labelHolder.putClientProperty("label", overlay);
        outer.putClientProperty("label", labelHolder);

        return outer;
    }

    /** For camera integration: returns label in "Vous" panel. */
    public JLabel getEcranMoi()   { return ecranMoi; }
    /** For camera integration: returns label in contact panel. */
    public JLabel getEcranAutre() { return ecranAutre; }
}