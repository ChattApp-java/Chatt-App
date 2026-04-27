package com.chatapp.client.modern;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.util.function.Consumer;

/**
 * Barre de saisie WhatsApp avec:
 *  - Bouton emoji
 *  - Bouton piece jointe
 *  - Champ texte arrondi
 *  - Bouton micro / envoyer
 */
public class BottomBar extends JPanel {

    private final JTextField field;
    private final JButton btnSend;
    private final JButton btnMic;
    private boolean isRecording = false;

    private final Runnable attachAction;

    public BottomBar(Runnable sendAction, Runnable micAction, Runnable attachAction, Consumer<JComponent> emojiAction) {
        this.attachAction = attachAction;
        setLayout(new BorderLayout(8, 0));
        setBackground(UIConstants.BG_PANEL);
        setBorder(new CompoundBorder(
                new MatteBorder(1, 0, 0, 0, UIConstants.DIVIDER),
                new EmptyBorder(10, 12, 10, 12)
        ));

        // Bouton emoji
        JButton btnEmoji = new JButton("☺");
        btnEmoji.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 20));
        btnEmoji.setFocusPainted(false);
        btnEmoji.setBackground(UIConstants.BG_PANEL);
        btnEmoji.setForeground(UIConstants.TEXT_SECONDARY);
        btnEmoji.setBorder(new EmptyBorder(4, 4, 4, 4));
        btnEmoji.setContentAreaFilled(false);
        btnEmoji.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnEmoji.addActionListener(e -> emojiAction.accept(btnEmoji));

        // Bouton piece jointe
        JButton btnAttach = new JButton("📎");
        btnAttach.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 18));
        btnAttach.setFocusPainted(false);
        btnAttach.setBackground(UIConstants.BG_PANEL);
        btnAttach.setForeground(UIConstants.TEXT_SECONDARY);
        btnAttach.setBorder(new EmptyBorder(4, 4, 4, 4));
        btnAttach.setContentAreaFilled(false);
        btnAttach.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnAttach.addActionListener(e -> { if (attachAction != null) attachAction.run(); });

        JPanel leftBtns = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        leftBtns.setOpaque(false);
        leftBtns.add(btnEmoji);
        leftBtns.add(btnAttach);

        // Container input arrondi
        JPanel inputContainer = new JPanel(new BorderLayout(8, 0)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(UIConstants.BG_INPUT);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                if (field.hasFocus()) {
                    g2.setColor(Utils.withAlpha(UIConstants.ACCENT, 100));
                    g2.setStroke(new BasicStroke(1.5f));
                    g2.drawRoundRect(1, 1, getWidth() - 2, getHeight() - 2, 20, 20);
                }
                g2.dispose();
            }
        };
        inputContainer.setOpaque(false);
        inputContainer.setBorder(new EmptyBorder(0, 14, 0, 10));

        field = new JTextField();
        field.setBackground(new Color(0, 0, 0, 0));
        field.setForeground(UIConstants.TEXT_PRIMARY);
        field.setCaretColor(UIConstants.ACCENT_LIGHT);
        field.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        field.setBorder(BorderFactory.createEmptyBorder(9, 0, 9, 0));
        field.setOpaque(false);

        String placeholder = "Message";
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
        field.addKeyListener(new KeyAdapter() {
            @Override public void keyTyped(KeyEvent e) { updateSendButton(); }
            @Override public void keyReleased(KeyEvent e) { updateSendButton(); }
        });

        inputContainer.add(field, BorderLayout.CENTER);

        // Bouton envoyer / micro
        btnSend = Utils.pillButton("▶", UIConstants.ACCENT, 50);
        btnSend.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnSend.setBorder(new EmptyBorder(10, 14, 10, 14));
        btnSend.addActionListener(e -> sendAction.run());
        btnSend.setVisible(false);

        btnMic = Utils.pillButton("🎤", UIConstants.ACCENT, 50);
        btnMic.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 16));
        btnMic.setBorder(new EmptyBorder(10, 14, 10, 14));
        btnMic.addActionListener(e -> micAction.run());

        JPanel rightBtn = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        rightBtn.setOpaque(false);
        rightBtn.add(btnSend);
        rightBtn.add(btnMic);

        add(leftBtns, BorderLayout.WEST);
        add(inputContainer, BorderLayout.CENTER);
        add(rightBtn, BorderLayout.EAST);
    }

    private void updateSendButton() {
        String txt = field.getText();
        boolean hasText = !txt.isEmpty() && !txt.equals("Message");
        btnSend.setVisible(hasText);
        btnMic.setVisible(!hasText);
    }

    public String getMessage() {
        String txt = field.getText();
        return txt.equals("Message") ? "" : txt;
    }

    public void clear() {
        field.setText("");
        updateSendButton();
    }

    public void appendText(String text) {
        if (field.getText().equals("Message")) {
            field.setText("");
            field.setForeground(UIConstants.TEXT_PRIMARY);
        }
        field.setText(field.getText() + text);
        updateSendButton();
        field.requestFocusInWindow();
    }
}

