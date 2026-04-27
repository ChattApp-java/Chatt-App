package com.chatapp.client.modern;

import javax.swing.SwingUtilities;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Clientui.main(args);
        });
    }
}

