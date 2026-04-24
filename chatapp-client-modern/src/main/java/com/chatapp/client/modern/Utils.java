package com.chatapp.client.modern;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class Utils {

    public static JLabel styledLabel(String text, int style, int size, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", style, size));
        label.setForeground(color);
        return label;
    }

    public static String escapeHtml(String text) {
        if (text == null) return "";
        StringBuilder sb = new StringBuilder();
        for (char c : text.toCharArray()) {
            switch (c) {
                case '&': sb.append("&amp;"); break;
                case '<': sb.append("<"); break;
                case '>': sb.append(">"); break;
                case 34: sb.append('&'); sb.append('q'); sb.append('u'); sb.append('o'); sb.append('t'); sb.append(';'); break;
                default: sb.append(c); break;
            }
        }
        return sb.toString();
    }

    public static String getInitials(String name) {
        if (name == null || name.isBlank()) return "?";
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1)
            return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
        return ("" + parts[0].charAt(0) + parts[parts.length - 1].charAt(0)).toUpperCase();
    }

    public static Color getAvatarColor(String name) {
        Color[] palette = UIConstants.AVATAR_COLORS;
        return palette[Math.abs(name.hashCode()) % palette.length];
    }

    public static JPanel makeAvatar(String name, int diameter) {
        String initials  = getInitials(name);
        Color  bgColor   = getAvatarColor(name);
        int    fontSize  = Math.max(diameter / 3, 10);

        JPanel avatar = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(bgColor);
                g2.fillOval(0, 0, diameter, diameter);
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Segoe UI", Font.BOLD, fontSize));
                FontMetrics fm = g2.getFontMetrics();
                int tx = (diameter - fm.stringWidth(initials)) / 2;
                int ty = (diameter - fm.getHeight()) / 2 + fm.getAscent();
                g2.drawString(initials, tx, ty);
                g2.dispose();
            }
        };
        avatar.setOpaque(false);
        avatar.setPreferredSize(new Dimension(diameter, diameter));
        avatar.setMinimumSize(new Dimension(diameter, diameter));
        avatar.setMaximumSize(new Dimension(diameter, diameter));
        return avatar;
    }

    public static JButton pillButton(String text, Color bg, int arc) {
        JButton btn = new JButton(text) {
            private float hover = 0f;
            {
                addMouseListener(new MouseAdapter() {
                    @Override public void mouseEntered(MouseEvent e) { hover = 1f; repaint(); }
                    @Override public void mouseExited(MouseEvent e)  { hover = 0f; repaint(); }
                });
            }
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color base   = bg;
                Color bright = bg.brighter();
                Color c = hover > 0 ? blend(base, bright, 0.25f) : base;
                g2.setColor(c);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setForeground(Color.WHITE);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setOpaque(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    public static JButton dangerButton(String text) {
        return pillButton(text, UIConstants.DANGER, 20);
    }

    public static JButton successButton(String text) {
        return pillButton(text, UIConstants.ONLINE_GREEN, 20);
    }

    public static JButton whatsappButton(String text) {
        return pillButton(text, UIConstants.ACCENT, 24);
    }

    public static Color blend(Color c1, Color c2, float ratio) {
        float inv = 1f - ratio;
        return new Color(
                clamp((int)(c1.getRed()   * inv + c2.getRed()   * ratio)),
                clamp((int)(c1.getGreen() * inv + c2.getGreen() * ratio)),
                clamp((int)(c1.getBlue()  * inv + c2.getBlue()  * ratio))
        );
    }

    public static Color withAlpha(Color c, int alpha) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
    }

    private static int clamp(int v) { return Math.max(0, Math.min(255, v)); }

    public static void paintCheckMarks(Graphics2D g2, int x, int y, int size, boolean blue) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Color c = blue ? UIConstants.CHECK_BLUE : UIConstants.CHECK_GREY;
        g2.setColor(c);
        g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.drawLine(x, y + size / 2, x + size / 3, y + size);
        g2.drawLine(x + size / 3, y + size, x + size, y);
        int ox = 4;
        g2.drawLine(x + ox, y + size / 2, x + ox + size / 3, y + size);
        g2.drawLine(x + ox + size / 3, y + size, x + ox + size, y);
    }

    public static JPanel roundedPanel(Color bg, int arc) {
        JPanel p = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);
                g2.dispose();
            }
        };
        p.setOpaque(false);
        return p;
    }
}

