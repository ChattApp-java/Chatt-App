package com.chatapp.client.modern;

import java.awt.Color;

/**
 * Palette de couleurs officielle WhatsApp Dark Mode.
 */
public class UIConstants {

    // ── Backgrounds WhatsApp Dark ────────────────────────────
    public static final Color BG_DARK      = new Color(11,  20,  26);   // #0B141A - fond principal
    public static final Color BG_PANEL     = new Color(17,  27,  33);   // #111B21 - panneau latéral
    public static final Color BG_SURFACE   = new Color(32,  44,  51);   // #202C33 - surface élévée
    public static final Color BG_INPUT     = new Color(42,  57,  66);   // #2A3942 - champ saisie
    public static final Color BG_CHAT      = new Color(11,  20,  26);   // fond conversation

    // ── Accent WhatsApp Teal/Green ───────────────────────────
    public static final Color ACCENT       = new Color(0, 168, 132);    // #00A884 - vert WhatsApp
    public static final Color ACCENT_LIGHT = new Color(0, 198, 156);    // #00C69C - hover
    public static final Color ACCENT_DARK  = new Color(0, 138, 110);    // #008A6E - press

    // ── Messages ─────────────────────────────────────────────
    public static final Color MSG_SENT     = new Color(0,  92,  75);    // #005C4B - bulle envoyée
    public static final Color MSG_RECV     = new Color(32, 44,  51);    // #202C33 - bulle reçue
    public static final Color MSG_SELECTED = new Color(42, 57,  66);    // sélection

    // ── Text ─────────────────────────────────────────────────
    public static final Color TEXT_PRIMARY   = new Color(233, 237, 239); // #E9EDED
    public static final Color TEXT_SECONDARY = new Color(134, 150, 160); // #8696A0
    public static final Color TEXT_MUTED     = new Color(134, 150, 160); // gris
    public static final Color TEXT_LINK      = new Color(83,  189, 235); // liens

    // ── Status ───────────────────────────────────────────────
    public static final Color ONLINE_GREEN = new Color(0, 168, 132);     // en ligne
    public static final Color DANGER       = new Color(239, 105, 96);   // rouge
    public static final Color WARNING      = new Color(255, 189, 46);   // jaune
    public static final Color CHECK_GREY   = new Color(134, 150, 160);  // tick gris
    public static final Color CHECK_BLUE   = new Color(83,  189, 235);  // tick bleu (lu)

    // ── Structural ───────────────────────────────────────────
    public static final Color BORDER_COLOR = new Color(42,  57,  66);   // bordures
    public static final Color DIVIDER      = new Color(30,  42,  49);   // séparateurs

    // ── Avatar palette ───────────────────────────────────────
    public static final Color[] AVATAR_COLORS = {
            new Color(0,   168, 132),  // teal
            new Color(236, 72,  153),  // pink
            new Color(245, 158, 11),   // amber
            new Color(20,  184, 166),  // teal clair
            new Color(59,  130, 246),  // blue
            new Color(168, 85,  247),  // purple
    };

    // ── Pattern WhatsApp chat background (subtle) ────────────
    public static final Color PATTERN_COLOR = new Color(17, 27, 33, 40);
}

