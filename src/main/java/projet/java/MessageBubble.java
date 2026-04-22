package projet.java;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * A chat bubble with:
 *  - avatar circle on the left  (received)
 *  - no avatar, right-aligned   (sent)
 *  - sender name + time as subtitle
 *  - rounded bubble background
 */
public class MessageBubble extends JPanel {

    private static final int  AVATAR_SIZE = 34;
    private static final int  ARC         = 20;
    private static final int  MAX_WIDTH   = 260;

    private final boolean mine;

    public MessageBubble(String sender, String text, String time, boolean mine) {
        this.mine = mine;

        setLayout(new BorderLayout(8, 0));
        setOpaque(false);
        setBorder(new EmptyBorder(4, 6, 4, 6));

        // ── Avatar (only for received) ──────────────────────
        if (!mine) {
            JPanel avatar = Utils.makeAvatar(sender, AVATAR_SIZE);
            JPanel avatarWrap = new JPanel(new BorderLayout());
            avatarWrap.setOpaque(false);
            avatarWrap.add(avatar, BorderLayout.SOUTH); // align to bottom of bubble
            add(avatarWrap, BorderLayout.WEST);
        }

        // ── Bubble content ──────────────────────────────────
        JPanel bubble = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                // Shadow (subtle)
                g2.setColor(new Color(0, 0, 0, 40));
                g2.fillRoundRect(2, 3, getWidth() - 2, getHeight() - 2, ARC, ARC);
                // Bubble body
                g2.setColor(mine ? UIConstants.MSG_SENT : UIConstants.MSG_RECV);
                g2.fillRoundRect(0, 0, getWidth() - 2, getHeight() - 2, ARC, ARC);
                // Subtle border on received
                if (!mine) {
                    g2.setColor(UIConstants.BORDER_COLOR);
                    g2.setStroke(new BasicStroke(1f));
                    g2.drawRoundRect(0, 0, getWidth() - 3, getHeight() - 3, ARC, ARC);
                }
                g2.dispose();
            }
        };
        bubble.setLayout(new BoxLayout(bubble, BoxLayout.Y_AXIS));
        bubble.setOpaque(false);
        bubble.setBorder(new EmptyBorder(10, 14, 10, 14));

        // Sender name (received only) or "Vous" (sent)
        if (!mine) {
            JLabel nameLabel = Utils.styledLabel(sender, Font.BOLD, 11, UIConstants.TEXT_ACCENT);
            nameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            bubble.add(nameLabel);
            bubble.add(Box.createVerticalStrut(4));
        }

        // Message text
        String safe = Utils.escapeHtml(text);
        JLabel msg = new JLabel(
                "<html><body style='width:" + MAX_WIDTH + "px; font-family:SansSerif; font-size:14px;'>"
                        + safe + "</body></html>"
        );
        msg.setForeground(UIConstants.TEXT_PRIMARY);
        msg.setAlignmentX(Component.LEFT_ALIGNMENT);
        bubble.add(msg);

        // Time stamp
        bubble.add(Box.createVerticalStrut(5));
        JLabel timeLabel = Utils.styledLabel(time, Font.PLAIN, 10, UIConstants.TEXT_MUTED);
        timeLabel.setAlignmentX(mine ? Component.RIGHT_ALIGNMENT : Component.LEFT_ALIGNMENT);
        bubble.add(timeLabel);

        add(bubble, BorderLayout.CENTER);

        // Spacer on right for received messages (keeps bubbles from stretching)
        if (!mine) {
            add(Box.createHorizontalStrut(40), BorderLayout.EAST);
        }
    }
}