package projet.java;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;

/**
 * Bottom input bar:
 *  - Rounded input container (dark surface)
 *  - Placeholder text hint
 *  - Send button (accent colour, pill shape)
 *  - Top separator line
 */
public class BottomBar extends JPanel {

    private final JTextField field;

    public BottomBar(Runnable sendAction) {
        setLayout(new BorderLayout(10, 0));
        setBackground(UIConstants.BG_PANEL);
        setBorder(new CompoundBorder(
                new MatteBorder(1, 0, 0, 0, UIConstants.BORDER_COLOR),
                new EmptyBorder(12, 16, 12, 16)
        ));

        // ── Input container (rounded) ────────────────────────
        JPanel inputContainer = new JPanel(new BorderLayout(8, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(UIConstants.BG_INPUT);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 24, 24);
                // Focus ring
                if (field.hasFocus()) {
                    g2.setColor(Utils.withAlpha(UIConstants.ACCENT, 120));
                    g2.setStroke(new BasicStroke(2f));
                    g2.drawRoundRect(1, 1, getWidth() - 2, getHeight() - 2, 24, 24);
                }
                g2.dispose();
            }
        };
        inputContainer.setOpaque(false);
        inputContainer.setBorder(new EmptyBorder(0, 16, 0, 12));

        field = new JTextField();
        field.setBackground(new Color(0, 0, 0, 0));
        field.setForeground(UIConstants.TEXT_PRIMARY);
        field.setCaretColor(UIConstants.ACCENT_LIGHT);
        field.setFont(new Font("SansSerif", Font.PLAIN, 14));
        field.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        field.setOpaque(false);

        // Placeholder simulation
        String placeholder = "Écrire un message…";
        field.setText(placeholder);
        field.setForeground(UIConstants.TEXT_MUTED);

        field.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) {
                if (field.getText().equals(placeholder)) {
                    field.setText("");
                    field.setForeground(UIConstants.TEXT_PRIMARY);
                }
                inputContainer.repaint();
            }
            @Override public void focusLost(FocusEvent e) {
                if (field.getText().isEmpty()) {
                    field.setText(placeholder);
                    field.setForeground(UIConstants.TEXT_MUTED);
                }
                inputContainer.repaint();
            }
        });

        field.addActionListener(e -> sendAction.run());

        inputContainer.add(field, BorderLayout.CENTER);

        // ── Send button ──────────────────────────────────────
        JButton send = Utils.pillButton("Envoyer  ›", UIConstants.ACCENT, 22);
        send.setFont(new Font("SansSerif", Font.BOLD, 13));
        send.setBorder(new EmptyBorder(10, 20, 10, 20));
        send.addActionListener(e -> sendAction.run());

        add(inputContainer, BorderLayout.CENTER);
        add(send, BorderLayout.EAST);
    }

    public String getMessage() {
        String txt = field.getText();
        // Don't return placeholder
        return txt.equals("Écrire un message…") ? "" : txt;
    }

    public void clear() {
        field.setText("");
    }
}