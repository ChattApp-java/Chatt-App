package frontend;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Zone de chat scrollable avec bulles de messages.
 */
public class PanneauChat extends JScrollPane {

    private final JPanel content;

    public PanneauChat() {
        content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(Constantes.BG_DARK);
        content.setBorder(new EmptyBorder(16, 8, 16, 8));

        setViewportView(content);
        setBorder(null);
        getViewport().setBackground(Constantes.BG_DARK);

        JScrollBar vBar = getVerticalScrollBar();
        vBar.setPreferredSize(new Dimension(5, 0));
        vBar.setUnitIncrement(16);
        vBar.setUI(new javax.swing.plaf.basic.BasicScrollBarUI() {
            @Override protected void configureScrollBarColors() {
                thumbColor = Constantes.BORDER_COLOR;
                trackColor = Constantes.BG_DARK;
            }
            @Override protected JButton createDecreaseButton(int o) { return zeroButton(); }
            @Override protected JButton createIncreaseButton(int o) { return zeroButton(); }
            private JButton zeroButton() {
                JButton b = new JButton();
                b.setPreferredSize(new Dimension(0, 0));
                return b;
            }
        });
        setHorizontalScrollBarPolicy(HORIZONTAL_SCROLLBAR_NEVER);
    }

    public void addMessage(String sender, String text, String time, boolean mine) {
        JPanel wrapper = new JPanel(new FlowLayout(mine ? FlowLayout.RIGHT : FlowLayout.LEFT, 0, 0));
        wrapper.setOpaque(false);
        wrapper.add(new MessageBubble(sender, text, time, mine));

        content.add(wrapper);
        content.add(Box.createVerticalStrut(4));
        revalidate();
        repaint();
        scrollBottom();
    }

    public void addSystemMessage(String text) {
        JPanel wrapper = new JPanel(new FlowLayout(FlowLayout.CENTER));
        wrapper.setOpaque(false);

        JPanel pill = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Constantes.BG_SURFACE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                g2.dispose();
            }
        };
        pill.setOpaque(false);
        pill.setBorder(new EmptyBorder(4, 14, 4, 14));

        JLabel label = Utilitaires.styledLabel(text, Font.ITALIC, 11, Constantes.TEXT_MUTED);
        pill.add(label);
        wrapper.add(pill);

        content.add(wrapper);
        content.add(Box.createVerticalStrut(8));
        revalidate();
        repaint();
    }

    private void scrollBottom() {
        SwingUtilities.invokeLater(() -> {
            JScrollBar bar = getVerticalScrollBar();
            bar.setValue(bar.getMaximum());
        });
    }
}