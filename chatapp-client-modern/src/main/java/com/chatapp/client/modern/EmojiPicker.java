package com.chatapp.client.modern;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;

/**
 * Popup selecteur d'emojis style WhatsApp.
 */
public class EmojiPicker extends JPopupMenu {

    private final Callback callback;

    public interface Callback {
        void onEmojiSelected(String emoji);
    }

    public EmojiPicker(Callback cb) {
        this.callback = cb;
        setBackground(UIConstants.BG_SURFACE);
        setBorder(BorderFactory.createLineBorder(UIConstants.BORDER_COLOR));

        JPanel grid = new JPanel(new GridLayout(6, 8, 4, 4));
        grid.setBackground(UIConstants.BG_SURFACE);
        grid.setBorder(new EmptyBorder(8, 8, 8, 8));

        String[] emojis = {
            "😀","😂","🥰","😍","😘","😎","🤔","😭",
            "😡","😱","🤗","😐","😴","🤒","🤠","😈",
            "👍","👎","👏","🙏","🤝","💪","🤞","✌",
            "❤","💔","💖","💯","🔥","✨","🎉","🎁",
            "🐱","🐶","🦊","🐼","🐨","🦁","🐯","🐷",
            "🌹","🌞","🌈","⭐","⚡","☀","🌙","☁"
        };

        for (String e : emojis) {
            JButton btn = new JButton(e);
            btn.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 20));
            btn.setFocusPainted(false);
            btn.setBackground(UIConstants.BG_SURFACE);
            btn.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
            btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btn.addActionListener(ae -> {
                callback.onEmojiSelected(e);
                setVisible(false);
            });
            grid.add(btn);
        }

        add(grid);
    }
}

