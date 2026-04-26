package commun;

/**
 * Contrat de communication partagé par toute l'application.
 * Format des messages : TYPE|param1|param2|...
 */
public final class Protocole {

    // ── Connexion ──────────────────────────────────────────────
    public static final String LOGIN      = "LOGIN";
    public static final String LOGIN_OK   = "LOGIN_OK";
    public static final String LOGIN_FAIL = "LOGIN_FAIL";
    public static final String LOGOUT     = "LOGOUT";

    // ── Utilisateurs ───────────────────────────────────────────
    public static final String USER_LIST   = "USER_LIST";
    public static final String USER_JOINED = "USER_JOINED";
    public static final String USER_LEFT   = "USER_LEFT";

    // ── Messages texte ─────────────────────────────────────────
    public static final String MSG      = "MSG";
    public static final String MSG_RECV = "MSG_RECV";

    // ── Appels audio/vidéo ─────────────────────────────────────
    public static final String CALL_REQUEST  = "CALL_REQUEST";
    public static final String CALL_INCOMING = "CALL_INCOMING";
    public static final String CALL_ACCEPT   = "CALL_ACCEPT";
    public static final String CALL_ACCEPTED = "CALL_ACCEPTED";
    public static final String CALL_REJECT   = "CALL_REJECT";
    public static final String CALL_REJECTED = "CALL_REJECTED";
    public static final String CALL_END      = "CALL_END";
    public static final String CALL_ENDED    = "CALL_ENDED";

    // ── Séparateur ─────────────────────────────────────────────
    public static final String SEP = "|";

    // ── Ports ──────────────────────────────────────────────────
    public static final int PORT_SIGNALING = 5000;  // TCP principal
    public static final int PORT_AUDIO     = 5001;  // TCP audio
    public static final int PORT_VIDEO     = 5002;  // TCP vidéo

    private Protocole() {}
}