package com.chatapp.client.modern;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import com.chatapp.protocol.Protocol;

/**
 * Zone de chat scrollable style WhatsApp.
 *  - Fond avec pattern subtil
 *  - Separateurs de date (Aujourd'hui, Hier...)
 *  - Messages avec statuts
 */
public class ChatPanel extends JScrollPane {

    private final JPanel content;
    private String lastDateLabel = "";

    public ChatPanel() {
        content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(UIConstants.BG_CHAT);
        content.setBorder(new EmptyBorder(16, 8, 16, 8));

        setViewportView(content);
        setBorder(null);
        getViewport().setBackground(UIConstants.BG_CHAT);

        // Scrollbar stylee
        JScrollBar vBar = getVerticalScrollBar();
        vBar.setPreferredSize(new Dimension(6, 0));
        vBar.setUnitIncrement(16);
        vBar.setUI(new javax.swing.plaf.basic.BasicScrollBarUI() {
            @Override protected void configureScrollBarColors() {
                thumbColor = UIConstants.BORDER_COLOR;
                trackColor = UIConstants.BG_CHAT;
            }
            @Override protected JButton createDecreaseButton(int o) { return zeroButton(); }
            @Override protected JButton createIncreaseButton(int o) { return zeroButton(); }
            private JButton zeroButton() {
                JButton b = new JButton(); b.setPreferredSize(new Dimension(0, 0)); return b;
            }
        });
        setHorizontalScrollBarPolicy(HORIZONTAL_SCROLLBAR_NEVER);
    }

    public void addMessage(String sender, String text, String time, boolean mine) {
        addMessage(sender, text, time, mine, Protocol.STATUS_SENT);
    }

    public void addMessage(String sender, String text, String time, boolean mine, String status) {
        addMessage(sender, text, time, mine, status, null);
    }

    public void addMessage(String sender, String text, String time, boolean mine, String status, String audioData) {
        checkDateSeparator();
        JPanel wrapper = new JPanel(new FlowLayout(mine ? FlowLayout.RIGHT : FlowLayout.LEFT, 0, 0));
        wrapper.setOpaque(false);
        wrapper.add(new MessageBubble(sender, text, time, mine, status, audioData));
        content.add(wrapper);
        content.add(Box.createVerticalStrut(2));
        revalidate(); repaint(); scrollBottom();
    }

    public void addSystemMessage(String text) {
        checkDateSeparator();
        JPanel wrapper = new JPanel(new FlowLayout(FlowLayout.CENTER));
        wrapper.setOpaque(false);

        JPanel pill = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(UIConstants.BG_SURFACE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                g2.dispose();
            }
        };
        pill.setOpaque(false);
        pill.setBorder(new EmptyBorder(4, 14, 4, 14));
        pill.add(Utils.styledLabel(text, Font.ITALIC, 11, UIConstants.TEXT_MUTED));
        wrapper.add(pill);

        content.add(wrapper);
        content.add(Box.createVerticalStrut(6));
        revalidate(); repaint(); scrollBottom();
    }

    public void addDateSeparator(String label) {
        JPanel wrapper = new JPanel(new FlowLayout(FlowLayout.CENTER));
        wrapper.setOpaque(false);
        JLabel lbl = Utils.styledLabel(label, Font.BOLD, 11, UIConstants.TEXT_SECONDARY);
        lbl.setBorder(new EmptyBorder(8, 16, 8, 16));
        wrapper.add(lbl);
        content.add(wrapper);
        content.add(Box.createVerticalStrut(4));
    }

    private void checkDateSeparator() {
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMMM yyyy"));
        if (!today.equals(lastDateLabel)) {
            lastDateLabel = today;
            addDateSeparator(today);
        }
    }

    public void clear() {
        content.removeAll();
        lastDateLabel = "";
        revalidate(); repaint();
    }

    private void scrollBottom() {
        SwingUtilities.invokeLater(() -> {
            JScrollBar bar = getVerticalScrollBar();
            bar.setValue(bar.getMaximum());
        });
    }
}

