package frontend;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;

/**
 * Barre d'envoi de message en bas de la fenêtre.
 */
public class BarreEnvoi extends JPanel {

    private final JTextField field;

    public BarreEnvoi(Runnable sendAction) {
        setLayout(new BorderLayout(10, 0));
        setBackground(Constantes.BG_PANEL);
        setBorder(new CompoundBorder(
                new MatteBorder(1, 0, 0, 0, Constantes.BORDER_COLOR),
                new EmptyBorder(12, 16, 12, 16)
        ));

        JPanel inputContainer = new JPanel(new BorderLayout(8, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Constantes.BG_INPUT);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 24, 24);
                if (field.hasFocus()) {
                    g2.setColor(Utilitaires.withAlpha(Constantes.ACCENT, 120));
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
        field.setForeground(Constantes.TEXT_PRIMARY);
        field.setCaretColor(Constantes.ACCENT_LIGHT);
        field.setFont(new Font("SansSerif", Font.PLAIN, 14));
        field.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        field.setOpaque(false);

        String placeholder = "Écrire un message…";
        field.setText(placeholder);
        field.setForeground(Constantes.TEXT_MUTED);

        field.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) {
                if (field.getText().equals(placeholder)) {
                    field.setText("");
                    field.setForeground(Constantes.TEXT_PRIMARY);
                }
                inputContainer.repaint();
            }
            @Override public void focusLost(FocusEvent e) {
                if (field.getText().isEmpty()) {
                    field.setText(placeholder);
                    field.setForeground(Constantes.TEXT_MUTED);
                }
                inputContainer.repaint();
            }
        });

        field.addActionListener(e -> sendAction.run());

        inputContainer.add(field, BorderLayout.CENTER);

        JButton send = Utilitaires.pillButton("Envoyer  ›", Constantes.ACCENT, 22);
        send.setFont(new Font("SansSerif", Font.BOLD, 13));
        send.setBorder(new EmptyBorder(10, 20, 10, 20));
        send.addActionListener(e -> sendAction.run());

        add(inputContainer, BorderLayout.CENTER);
        add(send, BorderLayout.EAST);
    }

    public String getMessage() {
        String txt = field.getText();
        return txt.equals("Écrire un message…") ? "" : txt;
    }

    public void clear() {
        field.setText("");
    }
}