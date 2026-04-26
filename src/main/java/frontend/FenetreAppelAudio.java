package frontend;

import client.GestionnaireAppelClient;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;

/**
 * Fenêtre d'appel audio avec avatar pulsant et contrôles.
 */
public class FenetreAppelAudio extends JFrame {

    private int seconds = 0;
    private boolean muted = false;
    private Timer timer;
    private GestionnaireAppelClient gestionnaireAppel;

    public FenetreAppelAudio(String other, String serveurIp) {
        setTitle("Appel Audio");
        setSize(340, 420);
        setResizable(false);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        getContentPane().setBackground(Constantes.BG_DARK);
        setLayout(new BorderLayout());
        setLocationRelativeTo(null);

        // CENTRE
        JPanel centre = new JPanel();
        centre.setLayout(new BoxLayout(centre, BoxLayout.Y_AXIS));
        centre.setOpaque(false);
        centre.setBorder(new EmptyBorder(50, 30, 20, 30));

        AvatarPulsant pulse = new AvatarPulsant(other, 90);
        pulse.setAlignmentX(Component.CENTER_ALIGNMENT);
        centre.add(pulse);
        centre.add(Box.createVerticalStrut(24));

        JLabel nameLabel = Utilitaires.styledLabel(other, Font.BOLD, 22, Constantes.TEXT_PRIMARY);
        nameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        centre.add(nameLabel);
        centre.add(Box.createVerticalStrut(8));

        JLabel timerLabel = Utilitaires.styledLabel("00:00", Font.PLAIN, 14, Constantes.ONLINE_GREEN);
        timerLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        centre.add(timerLabel);

        timer = new Timer(1000, e -> {
            seconds++;
            timerLabel.setText(String.format("%02d:%02d", seconds / 60, seconds % 60));
        });
        timer.start();

        add(centre, BorderLayout.CENTER);

        // BOTTOM: Controls
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 20));
        controls.setOpaque(false);
        controls.setBorder(new EmptyBorder(0, 0, 24, 0));

        JButton btnMute = Utilitaires.pillButton("🎙️  Muet", Constantes.BG_SURFACE, 50);
        btnMute.setBorder(new EmptyBorder(12, 20, 12, 20));
        btnMute.addActionListener(e -> {
            muted = !muted;
            btnMute.setText(muted ? "🔇  Muet" : "🎙️  Muet");
        });

        JButton btnHangUp = Utilitaires.pillButton("📵  Raccrocher", Constantes.DANGER, 50);
        btnHangUp.setBorder(new EmptyBorder(12, 20, 12, 20));
        btnHangUp.addActionListener(e -> raccrocher());

        controls.add(btnMute);
        controls.add(btnHangUp);
        add(controls, BorderLayout.SOUTH);

        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { raccrocher(); }
        });

        try {
            gestionnaireAppel = new GestionnaireAppelClient();
            gestionnaireAppel.demarrerAppelAudio(serveurIp);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Erreur appel audio: " + e.getMessage());
            dispose();
        }

        setVisible(true);
    }

    private void raccrocher() {
        timer.stop();
        if (gestionnaireAppel != null) {
            gestionnaireAppel.terminerAppel();
        }
        dispose();
    }

    private static class AvatarPulsant extends JPanel {
        private float pulse = 0f;
        private final String name;
        private final int size;
        private Timer pulseTimer;

        AvatarPulsant(String name, int size) {
            this.name = name;
            this.size = size;
            int total = size + 30;
            setPreferredSize(new Dimension(total, total));
            setMaximumSize(new Dimension(total, total));
            setOpaque(false);

            pulseTimer = new Timer(40, e -> {
                pulse = (pulse + 0.05f) % ((float) Math.PI * 2);
                repaint();
            });
            pulseTimer.start();
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int cx = getWidth() / 2;
            int cy = getHeight() / 2;

            float alpha = (float)(0.3 + 0.25 * Math.sin(pulse));
            int extra = (int)(6 + 5 * Math.sin(pulse));
            int r = size / 2 + extra;
            g2.setColor(new Color(
                    Constantes.ACCENT.getRed(),
                    Constantes.ACCENT.getGreen(),
                    Constantes.ACCENT.getBlue(),
                    (int)(alpha * 255)
            ));
            g2.fillOval(cx - r, cy - r, r * 2, r * 2);

            Color bg = Utilitaires.getAvatarColor(name);
            String ini = Utilitaires.getInitials(name);
            g2.setColor(bg);
            g2.fillOval(cx - size/2, cy - size/2, size, size);
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("SansSerif", Font.BOLD, size / 3));
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(ini, cx - fm.stringWidth(ini)/2, cy + fm.getAscent()/2 - 1);

            g2.dispose();
        }
    }
}