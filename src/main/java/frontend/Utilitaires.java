package frontend;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Utilitaires UI réutilisables.
 */
public class Utilitaires {

    public static JLabel styledLabel(String text, int style, int size, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("SansSerif", style, size));
        label.setForeground(color);
        return label;
    }

    public static String escapeHtml(String text) {
        return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    public static String getInitials(String name) {
        if (name == null || name.isBlank()) return "?";
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1)
            return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
        return ("" + parts[0].charAt(0) + parts[parts.length - 1].charAt(0)).toUpperCase();
    }

    public static Color getAvatarColor(String name) {
        return Constantes.AVATAR_COLORS[Math.abs(name.hashCode()) % Constantes.AVATAR_COLORS.length];
    }

    public static JPanel makeAvatar(String name, int diameter) {
        String initials = getInitials(name);
        Color bgColor = getAvatarColor(name);
        int fontSize = diameter / 3;

        JPanel avatar = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(bgColor);
                g2.fillOval(0, 0, diameter, diameter);
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("SansSerif", Font.BOLD, fontSize));
                FontMetrics fm = g2.getFontMetrics();
                int tx = (diameter - fm.stringWidth(initials)) / 2;
                int ty = (diameter - fm.getHeight()) / 2 + fm.getAscent();
                g2.drawString(initials, tx, ty);
                g2.dispose();
            }
        };
        avatar.setOpaque(false);
        avatar.setPreferredSize(new Dimension(diameter, diameter));
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
                Color base = bg;
                Color bright = bg.brighter();
                Color c = hover > 0 ? blend(base, bright, 0.25f) : base;
                g2.setColor(c);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setForeground(Color.WHITE);
        btn.setFont(new Font("SansSerif", Font.BOLD, 13));
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setOpaque(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    public static JButton dangerButton(String text) {
        return pillButton(text, Constantes.DANGER, 20);
    }

    public static JButton successButton(String text) {
        return pillButton(text, Constantes.ONLINE_GREEN, 20);
    }

    public static Color blend(Color c1, Color c2, float ratio) {
        float inv = 1f - ratio;
        return new Color(
                (int)(c1.getRed()   * inv + c2.getRed()   * ratio),
                (int)(c1.getGreen() * inv + c2.getGreen() * ratio),
                (int)(c1.getBlue()  * inv + c2.getBlue()  * ratio)
        );
    }

    public static Color withAlpha(Color c, int alpha) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
    }
}