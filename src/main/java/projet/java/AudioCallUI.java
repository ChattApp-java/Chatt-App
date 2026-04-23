package projet.java;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;

/**
 * Audio call window with:
 *  - Pulsing avatar ring
 *  - Contact name & live call timer
 *  - Mute + Hang-up buttons
 */
public class AudioCallUI extends JFrame {

    private int  seconds  = 0;
    private boolean muted = false;
    private Timer timer;

    public AudioCallUI(String other) {
        setTitle("Appel Audio");
        setSize(340, 420);
        setResizable(false);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        getContentPane().setBackground(UIConstants.BG_DARK);
        setLayout(new BorderLayout());
        setLocationRelativeTo(null);

        // ── CENTRE ──────────────────────────────────────────
        JPanel centre = new JPanel();
        centre.setLayout(new BoxLayout(centre, BoxLayout.Y_AXIS));
        centre.setOpaque(false);
        centre.setBorder(new EmptyBorder(50, 30, 20, 30));

        // Pulsing ring + avatar
        PulsingAvatar pulse = new PulsingAvatar(other, 90);
        pulse.setAlignmentX(Component.CENTER_ALIGNMENT);
        centre.add(pulse);
        centre.add(Box.createVerticalStrut(24));

        // Name
        JLabel nameLabel = Utils.styledLabel(other, Font.BOLD, 22, UIConstants.TEXT_PRIMARY);
        nameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        centre.add(nameLabel);
        centre.add(Box.createVerticalStrut(8));

        // Timer
        JLabel timerLabel = Utils.styledLabel("00:00", Font.PLAIN, 14, UIConstants.ONLINE_GREEN);
        timerLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        centre.add(timerLabel);

        // Start live timer
        timer = new Timer(1000, e -> {
            seconds++;
            timerLabel.setText(String.format("%02d:%02d", seconds / 60, seconds % 60));
        });
        timer.start();

        add(centre, BorderLayout.CENTER);

        // ── BOTTOM: Controls ─────────────────────────────────
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 20));
        controls.setOpaque(false);
        controls.setBorder(new EmptyBorder(0, 0, 24, 0));

        JButton btnMute = Utils.pillButton("🎙️  Muet", UIConstants.BG_SURFACE, 50);
        btnMute.setBorder(new EmptyBorder(12, 20, 12, 20));
        btnMute.addActionListener(e -> {
            muted = !muted;
            btnMute.setText(muted ? "🔇  Muet" : "🎙️  Muet");
        });

        JButton btnHangUp = Utils.pillButton("📵  Raccrocher", UIConstants.DANGER, 50);
        btnHangUp.setBorder(new EmptyBorder(12, 20, 12, 20));
        btnHangUp.addActionListener(e -> {
            timer.stop();
            dispose();
        });

        controls.add(btnMute);
        controls.add(btnHangUp);
        add(controls, BorderLayout.SOUTH);

        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { timer.stop(); }
        });

        setVisible(true);
    }

    // ── Inner: pulsing avatar ring ────────────────────────────
    private static class PulsingAvatar extends JPanel {
        private float pulse = 0f;
        private final String name;
        private final int    size;
        private Timer pulseTimer;

        PulsingAvatar(String name, int size) {
            this.name = name;
            this.size = size;
            int total = size + 30; // ring margin
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

            // Outer pulse ring
            float alpha = (float)(0.3 + 0.25 * Math.sin(pulse));
            int   extra = (int)(6 + 5 * Math.sin(pulse));
            int   r     = size / 2 + extra;
            g2.setColor(new Color(
                    UIConstants.ACCENT.getRed(),
                    UIConstants.ACCENT.getGreen(),
                    UIConstants.ACCENT.getBlue(),
                    (int)(alpha * 255)
            ));
            g2.fillOval(cx - r, cy - r, r * 2, r * 2);

            // Avatar circle
            Color bg  = Utils.getAvatarColor(name);
            String ini = Utils.getInitials(name);
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