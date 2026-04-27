package projet.java;

import javafx.scene.control.Button;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.geometry.Pos;

public class Utils {

    // ── Avatar helpers ───────────────────────────────────────

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

    /** Round avatar circle with initials, given diameter. */
    public static StackPane makeAvatar(String name, int diameter) {
        String initials = getInitials(name);
        Color  bgColor  = getAvatarColor(name);
        int    fontSize = diameter / 3;

        // Circle background
        Circle circle = new Circle(diameter / 2.0, bgColor);

        // Initials text
        Text text = new Text(initials);
        text.setFont(Font.font("SansSerif", FontWeight.BOLD, fontSize));
        text.setFill(Color.WHITE);

        StackPane avatar = new StackPane(circle, text);
        avatar.setAlignment(Pos.CENTER);
        avatar.setMinSize(diameter, diameter);
        avatar.setMaxSize(diameter, diameter);
        avatar.setPrefSize(diameter, diameter);

        return avatar;
    }

    // ── Styled buttons ───────────────────────────────────────

    /** Pill-shaped button with hover brightness effect. */
    public static Button pillButton(String text, Color bg, int arc) {
        Button btn = new Button(text);
        btn.setFont(Font.font("SansSerif", FontWeight.BOLD, 13));
        btn.setTextFill(Color.WHITE);
        btn.setStyle(
                "-fx-background-color: " + UIConstants.toHex(bg) + ";" +
                        "-fx-background-radius: " + arc + ";" +
                        "-fx-padding: 8 16 8 16;" +
                        "-fx-cursor: hand;"
        );

        // Hover effect
        String base    = UIConstants.toHex(bg);
        String brighter = UIConstants.toHex(bg.brighter());
        btn.setOnMouseEntered(e -> btn.setStyle(
                "-fx-background-color: " + brighter + ";" +
                        "-fx-background-radius: " + arc + ";" +
                        "-fx-padding: 8 16 8 16;" +
                        "-fx-cursor: hand;"
        ));
        btn.setOnMouseExited(e -> btn.setStyle(
                "-fx-background-color: " + base + ";" +
                        "-fx-background-radius: " + arc + ";" +
                        "-fx-padding: 8 16 8 16;" +
                        "-fx-cursor: hand;"
        ));
        return btn;
    }

    /** Danger pill button (red). */
    public static Button dangerButton(String text) {
        return pillButton(text, UIConstants.DANGER, 20);
    }

    /** Success pill button (green). */
    public static Button successButton(String text) {
        return pillButton(text, UIConstants.ONLINE_GREEN, 20);
    }

    // ── Color utilities ──────────────────────────────────────

    public static Color blend(Color c1, Color c2, double ratio) {
        double inv = 1.0 - ratio;
        return Color.color(
                c1.getRed()   * inv + c2.getRed()   * ratio,
                c1.getGreen() * inv + c2.getGreen() * ratio,
                c1.getBlue()  * inv + c2.getBlue()  * ratio
        );
    }

    public static Color withAlpha(Color c, double alpha) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
    }
}