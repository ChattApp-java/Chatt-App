package com.chatapp.protocol;

/**
 * Protocol.java
 * Contrat de communication partagé par toute l'application.
 * Format des messages : TYPE|param1|param2|...
 */
public class Protocol {

    // ── Connexion ──────────────────────────────────────────────
    public static final String LOGIN         = "LOGIN";
    public static final String LOGIN_OK      = "LOGIN_OK";
    public static final String LOGIN_FAIL    = "LOGIN_FAIL";
    public static final String LOGOUT        = "LOGOUT";
    public static final String REGISTER      = "REGISTER";
    public static final String REGISTER_OK   = "REGISTER_OK";
    public static final String REGISTER_FAIL = "REGISTER_FAIL";

    // ── Utilisateurs ───────────────────────────────────────────
    public static final String USER_LIST     = "USER_LIST";
    public static final String USER_JOINED   = "USER_JOINED";
    public static final String USER_LEFT     = "USER_LEFT";
    public static final String USER_STATUS   = "USER_STATUS";     // status en ligne/hors ligne

    // ── Messages texte ─────────────────────────────────────────
    public static final String MSG           = "MSG";
    public static final String MSG_RECV      = "MSG_RECV";
    public static final String MSG_STATUS    = "MSG_STATUS";      // statut d'un message
    public static final String MSG_ACK       = "MSG_ACK";         // accusé de réception
    public static final String MSG_READ      = "MSG_READ";        // message lu

    // ── Indicateurs de frappe ──────────────────────────────────
    public static final String TYPING_START  = "TYPING_START";
    public static final String TYPING_STOP   = "TYPING_STOP";

    // ── Appels audio/vidéo ─────────────────────────────────────
    public static final String CALL_REQUEST  = "CALL_REQUEST";
    public static final String CALL_INCOMING = "CALL_INCOMING";
    public static final String CALL_ACCEPT   = "CALL_ACCEPT";
    public static final String CALL_ACCEPTED = "CALL_ACCEPTED";
    public static final String CALL_REJECT   = "CALL_REJECT";
    public static final String CALL_REJECTED = "CALL_REJECTED";
    public static final String CALL_END      = "CALL_END";
    public static final String CALL_ENDED    = "CALL_ENDED";

    // ── Messages audio ─────────────────────────────────────────
    public static final String AUDIO_MSG     = "AUDIO_MSG";       // message vocal
    public static final String AUDIO_RECV    = "AUDIO_RECV";      // vocal reçu

    // ── Fichiers ───────────────────────────────────────────────
    public static final String FILE          = "FILE";
    public static final String FILE_RECV     = "FILE_RECV";

    // ── Historique ─────────────────────────────────────────────
    public static final String HISTORY_REQ   = "HISTORY_REQ";
    public static final String HISTORY_RESP  = "HISTORY_RESP";

    // ── Séparateur ─────────────────────────────────────────────
    public static final String SEP           = "|";

    // ── Ports ──────────────────────────────────────────────────
    public static final int PORT_SIGNALING   = 5000;
    public static final int PORT_AUDIO       = 5001;
    public static final int PORT_VIDEO       = 5002;

    // ── Statuts message ────────────────────────────────────────
    public static final String STATUS_SENT      = "SENT";       // envoyé
    public static final String STATUS_DELIVERED = "DELIVERED";  // livré (2 ticks gris)
    public static final String STATUS_READ      = "READ";       // lu (2 ticks bleus)

    private Protocol() {}
}

