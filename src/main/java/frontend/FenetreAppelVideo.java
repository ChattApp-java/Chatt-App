package frontend;

import client.GestionnaireAppelClient;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;

/**
 * Fenêtre d'appel vidéo avec deux panneaux vidéo et contrôles.
 */
public class FenetreAppelVideo extends JFrame {

    private JLabel ecranMoi;
    private JLabel ecranAutre;
    private boolean camOn = true;
    private boolean micOn = true;
    private GestionnaireAppelClient gestionnaireAppel;

    public FenetreAppelVideo(String me, String other, String serveurIp) {
        setTitle("Appel Vidéo");
        setSize(820, 520);
        setResizable(true);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        getContentPane().setBackground(Constantes.BG_DARK);
        setLayout(new BorderLayout(0, 0));
        setLocationRelativeTo(null);

        // Video panels
        JPanel videosPanel = new JPanel(new GridLayout(1, 2, 3, 0));
        videosPanel.setBackground(Constantes.BG_DARK);

        JPanel myPanel = makeVideoPanel(me, true);
        JPanel otherPanel = makeVideoPanel(other, false);

        ecranMoi = new JLabel();
        ecranMoi.setHorizontalAlignment(SwingConstants.CENTER);
        ecranAutre = new JLabel();
        ecranAutre.setHorizontalAlignment(SwingConstants.CENTER);

        JPanel holderMoi = (JPanel) myPanel.getClientProperty("holder");
        if (holderMoi != null) {
            holderMoi.removeAll();
            holderMoi.setLayout(new BorderLayout());
            holderMoi.add(ecranMoi, BorderLayout.CENTER);
        }

        JPanel holderAutre = (JPanel) otherPanel.getClientProperty("holder");
        if (holderAutre != null) {
            holderAutre.removeAll();
            holderAutre.setLayout(new BorderLayout());
            holderAutre.add(ecranAutre, BorderLayout.CENTER);
        }

        videosPanel.add(otherPanel);
        videosPanel.add(myPanel);

        add(videosPanel, BorderLayout.CENTER);

        // Controls bar
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.CENTER, 16, 0));
        controls.setBackground(Constantes.BG_PANEL);
        controls.setBorder(new EmptyBorder(14, 0, 14, 0));

        JButton btnMic = Utilitaires.pillButton("🎙️  Micro", Constantes.BG_SURFACE, 50);
        btnMic.setBorder(new EmptyBorder(10, 18, 10, 18));
        btnMic.addActionListener(e -> {
            micOn = !micOn;
            btnMic.setText(micOn ? "🎙️  Micro" : "🔇  Micro");
        });

        JButton btnCam = Utilitaires.pillButton("📷  Caméra", Constantes.BG_SURFACE, 50);
        btnCam.setBorder(new EmptyBorder(10, 18, 10, 18));
        btnCam.addActionListener(e -> {
            camOn = !camOn;
            btnCam.setText(camOn ? "📷  Caméra" : "🚫  Caméra");
        });

        JButton btnEnd = Utilitaires.pillButton("📵  Terminer", Constantes.DANGER, 50);
        btnEnd.setBorder(new EmptyBorder(10, 24, 10, 24));
        btnEnd.addActionListener(e -> terminerAppel());

        controls.add(btnMic);
        controls.add(btnCam);
        controls.add(btnEnd);
        add(controls, BorderLayout.SOUTH);

        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { terminerAppel(); }
        });

        try {
            gestionnaireAppel = new GestionnaireAppelClient();
            gestionnaireAppel.demarrerAppelVideo(serveurIp, ecranMoi);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Erreur appel vidéo: " + e.getMessage());
            dispose();
        }

        setVisible(true);
    }

    private JPanel makeVideoPanel(String name, boolean isMe) {
        JPanel outer = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
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

        JPanel centre = new JPanel();
        centre.setLayout(new BoxLayout(centre, BoxLayout.Y_AXIS));
        centre.setOpaque(false);

        JPanel avatar = Utilitaires.makeAvatar(name, 72);
        avatar.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel nameLabel = Utilitaires.styledLabel(
                name + (isMe ? "  (Vous)" : ""), Font.BOLD, 15, Constantes.TEXT_PRIMARY
        );
        nameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        centre.add(Box.createVerticalGlue());
        centre.add(avatar);
        centre.add(Box.createVerticalStrut(14));
        centre.add(nameLabel);
        centre.add(Box.createVerticalGlue());

        outer.add(centre, BorderLayout.CENTER);

        JLabel overlay = Utilitaires.styledLabel(name, Font.BOLD, 12, Constantes.TEXT_MUTED);
        overlay.setBorder(new EmptyBorder(8, 10, 0, 0));
        outer.add(overlay, BorderLayout.NORTH);

        JPanel holder = new JPanel(new BorderLayout());
        holder.setOpaque(false);
        outer.putClientProperty("holder", holder);
        outer.add(holder, BorderLayout.CENTER);

        return outer;
    }

    private void terminerAppel() {
        if (gestionnaireAppel != null) {
            gestionnaireAppel.terminerAppel();
        }
        dispose();
    }

    public JLabel getEcranMoi() { return ecranMoi; }
    public JLabel getEcranAutre() { return ecranAutre; }
}