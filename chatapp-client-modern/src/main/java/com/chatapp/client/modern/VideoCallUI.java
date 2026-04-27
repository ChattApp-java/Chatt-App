package com.chatapp.client.modern;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamPanel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;

/**
 * Video call window with:
 *  - Active webcam feed (local)
 *  - Placeholder for remote video
 *  - Bottom control bar: Mute / Cam / Hang-up
 *  - onHangUp callback to notify Clientui
 */
public class VideoCallUI extends JFrame {

    private boolean camOn = true;
    private boolean micOn = true;
    private Webcam webcam;
    private WebcamPanel webcamPanel;
    private JLabel remoteLabel;
    private final Runnable onHangUp;

    public VideoCallUI(String me, String other, Runnable onHangUp) {
        this.onHangUp = onHangUp;
        setTitle("Appel Video");
        setSize(900, 520);
        setResizable(true);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        getContentPane().setBackground(UIConstants.BG_DARK);
        setLayout(new BorderLayout(0, 0));
        setLocationRelativeTo(null);

        // Video panels
        JPanel videosPanel = new JPanel(new GridLayout(1, 2, 4, 0));
        videosPanel.setBackground(UIConstants.BG_DARK);

        // Remote panel (placeholder until real feed)
        JPanel remotePanel = createRemotePanel(other);

        // Local webcam panel
        JPanel localPanel = createLocalPanel(me);

        videosPanel.add(remotePanel);
        videosPanel.add(localPanel);
        add(videosPanel, BorderLayout.CENTER);

        // Controls bar
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.CENTER, 16, 0));
        controls.setBackground(UIConstants.BG_PANEL);
        controls.setBorder(new EmptyBorder(14, 0, 14, 0));

        JButton btnMic = Utils.pillButton("🎙️  Micro", UIConstants.BG_SURFACE, 50);
        btnMic.setBorder(new EmptyBorder(10, 18, 10, 18));
        btnMic.addActionListener(e -> {
            micOn = !micOn;
            btnMic.setText(micOn ? "🎙️  Micro" : "🔇  Micro");
        });

        JButton btnCam = Utils.pillButton("📷  Camera", UIConstants.BG_SURFACE, 50);
        btnCam.setBorder(new EmptyBorder(10, 18, 10, 18));
        btnCam.addActionListener(e -> {
            camOn = !camOn;
            btnCam.setText(camOn ? "📷  Camera" : "🚫  Camera");
            if (webcam != null) {
                if (camOn && !webcam.isOpen()) webcam.open();
                else if (!camOn && webcam.isOpen()) webcam.close();
            }
        });

        JButton btnEnd = Utils.pillButton("📵  Terminer", UIConstants.DANGER, 50);
        btnEnd.setBorder(new EmptyBorder(10, 24, 10, 24));
        btnEnd.addActionListener(e -> endCall());

        controls.add(btnMic);
        controls.add(btnCam);
        controls.add(btnEnd);
        add(controls, BorderLayout.SOUTH);

        // Cleanup on close
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                endCall();
            }
        });

        setVisible(true);
    }

    private void endCall() {
        if (webcam != null && webcam.isOpen()) webcam.close();
        if (onHangUp != null) onHangUp.run();
        dispose();
    }

    private JPanel createRemotePanel(String other) {
        JPanel panel = new JPanel(new BorderLayout()) {
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
        panel.setOpaque(false);

        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setOpaque(false);

        JPanel avatar = Utils.makeAvatar(other, 72);
        avatar.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel nameLabel = Utils.styledLabel(other, Font.BOLD, 15, UIConstants.TEXT_PRIMARY);
        nameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        remoteLabel = Utils.styledLabel("En attente de video...", Font.ITALIC, 12, UIConstants.TEXT_MUTED);
        remoteLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        center.add(Box.createVerticalGlue());
        center.add(avatar);
        center.add(Box.createVerticalStrut(14));
        center.add(nameLabel);
        center.add(Box.createVerticalStrut(8));
        center.add(remoteLabel);
        center.add(Box.createVerticalGlue());

        panel.add(center, BorderLayout.CENTER);

        JLabel overlay = Utils.styledLabel(other, Font.BOLD, 12, UIConstants.TEXT_MUTED);
        overlay.setBorder(new EmptyBorder(8, 10, 0, 0));
        panel.add(overlay, BorderLayout.NORTH);

        return panel;
    }

    private JPanel createLocalPanel(String me) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(UIConstants.BG_DARK);

        try {
            webcam = Webcam.getDefault();
            if (webcam != null) {
                webcam.setViewSize(webcam.getViewSizes()[0]);
                webcam.open();

                webcamPanel = new WebcamPanel(webcam);
                webcamPanel.setFPSDisplayed(false);
                webcamPanel.setDisplayDebugInfo(false);
                webcamPanel.setImageSizeDisplayed(false);
                webcamPanel.setMirrored(true);
                webcamPanel.setFillArea(true);
                webcamPanel.setBackground(UIConstants.BG_DARK);

                panel.add(webcamPanel, BorderLayout.CENTER);
            } else {
                panel.add(createFallbackPanel(me), BorderLayout.CENTER);
            }
        } catch (Exception e) {
            panel.add(createFallbackPanel(me), BorderLayout.CENTER);
        }

        JLabel overlay = Utils.styledLabel(me + "  (Vous)", Font.BOLD, 12, UIConstants.TEXT_MUTED);
        overlay.setBorder(new EmptyBorder(8, 10, 0, 0));
        panel.add(overlay, BorderLayout.NORTH);

        return panel;
    }

    private JPanel createFallbackPanel(String me) {
        JPanel fallback = new JPanel() {
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
        fallback.setOpaque(false);
        fallback.setLayout(new BoxLayout(fallback, BoxLayout.Y_AXIS));

        JPanel avatar = Utils.makeAvatar(me, 72);
        avatar.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel nameLabel = Utils.styledLabel(me + "  (Vous)", Font.BOLD, 15, UIConstants.TEXT_PRIMARY);
        nameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel errLabel = Utils.styledLabel("Camera non disponible", Font.ITALIC, 12, UIConstants.DANGER);
        errLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        fallback.add(Box.createVerticalGlue());
        fallback.add(avatar);
        fallback.add(Box.createVerticalStrut(14));
        fallback.add(nameLabel);
        fallback.add(Box.createVerticalStrut(8));
        fallback.add(errLabel);
        fallback.add(Box.createVerticalGlue());

        return fallback;
    }

    public JLabel getRemoteLabel() { return remoteLabel; }
}

