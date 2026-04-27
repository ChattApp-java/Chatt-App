import javafx.scene.paint.Color;

public class UIConstants {

    // ── Backgrounds ──────────────────────────────────────────
    public static final Color BG_DARK    = Color.rgb(9,  10,  20);
    public static final Color BG_PANEL   = Color.rgb(15, 16,  30);
    public static final Color BG_SURFACE = Color.rgb(22, 23,  42);
    public static final Color BG_INPUT   = Color.rgb(30, 31,  56);

    // ── Accent ───────────────────────────────────────────────
    public static final Color ACCENT       = Color.rgb(108, 99,  255);
    public static final Color ACCENT_LIGHT = Color.rgb(148, 141, 255);
    public static final Color ACCENT_GLOW  = Color.rgb(108, 99,  255, 0.27);

    // ── Messages ─────────────────────────────────────────────
    public static final Color MSG_SENT = Color.rgb(79,  58,  218);
    public static final Color MSG_RECV = Color.rgb(28,  29,  52);

    // ── Text ─────────────────────────────────────────────────
    public static final Color TEXT_PRIMARY = Color.rgb(228, 230, 255);
    public static final Color TEXT_MUTED   = Color.rgb(100, 102, 132);
    public static final Color TEXT_ACCENT  = Color.rgb(148, 141, 255);

    // ── Status ───────────────────────────────────────────────
    public static final Color ONLINE_GREEN = Color.rgb(52,  211, 153);
    public static final Color DANGER       = Color.rgb(248, 113, 113);
    public static final Color SUCCESS      = Color.rgb(34,  197, 94);
    public static final Color WARNING      = Color.rgb(251, 191, 36);

    // ── Structural ───────────────────────────────────────────
    public static final Color BORDER_COLOR = Color.rgb(36, 38, 66);

    // ── Avatar palette ───────────────────────────────────────
    public static final Color[] AVATAR_COLORS = {
            Color.rgb(108, 99,  255),  // violet
            Color.rgb(236, 72,  153),  // rose
            Color.rgb(245, 158,  11),  // amber
            Color.rgb( 20, 184, 166),  // teal
            Color.rgb( 59, 130, 246),  // blue
            Color.rgb(168,  85, 247),  // purple
    };

    // ── CSS hex helper (for inline -fx- styles) ───────────────
    public static String toHex(Color c) {
        return String.format("#%02x%02x%02x",
                (int)(c.getRed()   * 255),
                (int)(c.getGreen() * 255),
                (int)(c.getBlue()  * 255));
    }
}