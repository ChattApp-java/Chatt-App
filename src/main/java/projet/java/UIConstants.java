package projet.java;
import java.awt.*;

public class UIConstants {

    // ── Backgrounds ──────────────────────────────────────────
    public static final Color BG_DARK    = new Color(9,  10, 20);
    public static final Color BG_PANEL   = new Color(15, 16, 30);
    public static final Color BG_SURFACE = new Color(22, 23, 42);
    public static final Color BG_INPUT   = new Color(30, 31, 56);

    // ── Accent ───────────────────────────────────────────────
    public static final Color ACCENT       = new Color(108, 99, 255);
    public static final Color ACCENT_LIGHT = new Color(148, 141, 255);
    public static final Color ACCENT_GLOW  = new Color(108, 99, 255, 70);

    // ── Messages ─────────────────────────────────────────────
    public static final Color MSG_SENT = new Color(79, 58, 218);
    public static final Color MSG_RECV = new Color(28, 29, 52);

    // ── Text ─────────────────────────────────────────────────
    public static final Color TEXT_PRIMARY = new Color(228, 230, 255);
    public static final Color TEXT_MUTED   = new Color(100, 102, 132);
    public static final Color TEXT_ACCENT  = new Color(148, 141, 255);

    // ── Status ───────────────────────────────────────────────
    public static final Color ONLINE_GREEN = new Color(52, 211, 153);
    public static final Color DANGER       = new Color(248, 113, 113);
    public static final Color WARNING      = new Color(251, 191, 36);

    // ── Structural ───────────────────────────────────────────
    public static final Color BORDER_COLOR = new Color(36, 38, 66);

    // ── Avatar palette ───────────────────────────────────────
    public static final Color[] AVATAR_COLORS = {
            new Color(108, 99,  255),  // violet
            new Color(236, 72,  153),  // rose
            new Color(245, 158,  11),  // amber
            new Color( 20, 184, 166),  // teal
            new Color( 59, 130, 246),  // blue
            new Color(168,  85, 247),  // purple
    };
}