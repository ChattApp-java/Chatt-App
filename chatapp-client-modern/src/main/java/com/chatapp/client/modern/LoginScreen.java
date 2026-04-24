package com.chatapp.client.modern;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.Socket;
import com.chatapp.protocol.Protocol;

/**
 * Ecran de connexion/inscription style WhatsApp.
 */
public class LoginScreen extends JFrame {

    private final CardLayout cardLayout;
    private final JPanel cardPanel;
    private JTextField txtUser;
    private JPasswordField txtPass;
    private JTextField txtEmail;
    private final Callback callback;

    public interface Callback {
        void onLogin(String username, Socket socket, PrintWriter out, BufferedReader in);
    }

    public LoginScreen(Callback cb) {
        this.callback = cb;
        setTitle("ChatApp");
        setSize(380, 520);
        setResizable(false);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        getContentPane().setBackground(UIConstants.BG_DARK);
        setLocationRelativeTo(null);

        cardLayout = new CardLayout();
        cardPanel = new JPanel(cardLayout);
        cardPanel.setOpaque(false);

        cardPanel.add(createLoginPanel(), "LOGIN");
        cardPanel.add(createRegisterPanel(), "REGISTER");

        add(cardPanel, BorderLayout.CENTER);
        setVisible(true);
    }

    private JPanel createLoginPanel() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(40, 30, 30, 30));

        // Logo
        JLabel logo = new JLabel("💬", SwingConstants.CENTER);
        logo.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 64));
        logo.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel title = new JLabel("ChatApp", SwingConstants.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 28));
        title.setForeground(UIConstants.TEXT_PRIMARY);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitle = new JLabel("Connectez-vous pour discuter", SwingConstants.CENTER);
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subtitle.setForeground(UIConstants.TEXT_SECONDARY);
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        p.add(logo);
        p.add(Box.createVerticalStrut(12));
        p.add(title);
        p.add(Box.createVerticalStrut(4));
        p.add(subtitle);
        p.add(Box.createVerticalStrut(32));

        txtUser = createField("Nom d'utilisateur");
        txtPass = createPassField("Mot de passe");

        p.add(txtUser);
        p.add(Box.createVerticalStrut(12));
        p.add(txtPass);
        p.add(Box.createVerticalStrut(24));

        JButton btnLogin = Utils.whatsappButton("Se connecter");
        btnLogin.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        btnLogin.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnLogin.setFont(new Font("Segoe UI", Font.BOLD, 15));
        btnLogin.addActionListener(e -> doLogin());

        p.add(btnLogin);
        p.add(Box.createVerticalStrut(16));

        JLabel link = new JLabel("Pas de compte ? S'inscrire");
        link.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        link.setForeground(UIConstants.ACCENT);
        link.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        link.setAlignmentX(Component.CENTER_ALIGNMENT);
        link.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { cardLayout.show(cardPanel, "REGISTER"); }
        });
        p.add(link);

        return p;
    }

    private JPanel createRegisterPanel() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(40, 30, 30, 30));

        JLabel title = new JLabel("Inscription", SwingConstants.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setForeground(UIConstants.TEXT_PRIMARY);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        p.add(title);
        p.add(Box.createVerticalStrut(32));

        JTextField regUser = createField("Nom d'utilisateur");
        JPasswordField regPass = createPassField("Mot de passe");
        txtEmail = createField("Email");

        p.add(regUser);
        p.add(Box.createVerticalStrut(12));
        p.add(regPass);
        p.add(Box.createVerticalStrut(12));
        p.add(txtEmail);
        p.add(Box.createVerticalStrut(24));

        JButton btnReg = Utils.whatsappButton("S'inscrire");
        btnReg.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        btnReg.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnReg.setFont(new Font("Segoe UI", Font.BOLD, 15));
        btnReg.addActionListener(e -> {
            JOptionPane.showMessageDialog(this, "Inscription simulee - utilisez la connexion directe.");
            cardLayout.show(cardPanel, "LOGIN");
        });

        p.add(btnReg);
        p.add(Box.createVerticalStrut(16));

        JLabel link = new JLabel("Deja un compte ? Se connecter");
        link.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        link.setForeground(UIConstants.ACCENT);
        link.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        link.setAlignmentX(Component.CENTER_ALIGNMENT);
        link.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { cardLayout.show(cardPanel, "LOGIN"); }
        });
        p.add(link);

        return p;
    }

    private JTextField createField(String placeholder) {
        JTextField f = new JTextField(placeholder);
        f.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        f.setForeground(UIConstants.TEXT_MUTED);
        f.setBackground(UIConstants.BG_INPUT);
        f.setCaretColor(UIConstants.ACCENT);
        f.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIConstants.BORDER_COLOR, 1, true),
                new EmptyBorder(10, 14, 10, 14)
        ));
        f.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        f.setAlignmentX(Component.CENTER_ALIGNMENT);
        f.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) {
                if (f.getText().equals(placeholder)) { f.setText(""); f.setForeground(UIConstants.TEXT_PRIMARY); }
                f.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(UIConstants.ACCENT, 1, true),
                        new EmptyBorder(10, 14, 10, 14)
                ));
            }
            @Override public void focusLost(FocusEvent e) {
                if (f.getText().isEmpty()) { f.setText(placeholder); f.setForeground(UIConstants.TEXT_MUTED); }
                f.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(UIConstants.BORDER_COLOR, 1, true),
                        new EmptyBorder(10, 14, 10, 14)
                ));
            }
        });
        return f;
    }

    private JPasswordField createPassField(String placeholder) {
        JPasswordField f = new JPasswordField(placeholder);
        f.setEchoChar((char) 0);
        f.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        f.setForeground(UIConstants.TEXT_MUTED);
        f.setBackground(UIConstants.BG_INPUT);
        f.setCaretColor(UIConstants.ACCENT);
        f.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIConstants.BORDER_COLOR, 1, true),
                new EmptyBorder(10, 14, 10, 14)
        ));
        f.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        f.setAlignmentX(Component.CENTER_ALIGNMENT);
        f.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) {
                if (String.valueOf(f.getPassword()).equals(placeholder)) { f.setText(""); f.setEchoChar('●'); f.setForeground(UIConstants.TEXT_PRIMARY); }
                f.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(UIConstants.ACCENT, 1, true),
                        new EmptyBorder(10, 14, 10, 14)
                ));
            }
            @Override public void focusLost(FocusEvent e) {
                if (f.getPassword().length == 0) { f.setText(placeholder); f.setEchoChar((char) 0); f.setForeground(UIConstants.TEXT_MUTED); }
                f.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(UIConstants.BORDER_COLOR, 1, true),
                        new EmptyBorder(10, 14, 10, 14)
                ));
            }
        });
        return f;
    }

    private void doLogin() {
        String user = txtUser.getText().trim();
        if (user.isEmpty() || user.equals("Nom d'utilisateur")) {
            JOptionPane.showMessageDialog(this, "Entrez un nom d'utilisateur.");
            return;
        }
        try {
            Socket socket = new Socket("localhost", Protocol.PORT_SIGNALING);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out.println(Protocol.LOGIN + Protocol.SEP + user);
            String rep = in.readLine();
            if (rep != null && rep.startsWith(Protocol.LOGIN_OK)) {
                dispose();
                callback.onLogin(user, socket, out, in);
            } else {
                JOptionPane.showMessageDialog(this, "Connexion refusee.");
                socket.close();
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Serveur injoignable : " + ex.getMessage());
        }
    }
}

