package org.example.tpchatjavafx.common;

/**
 * Tous les types de messages échangés entre client et serveur.
 */
public enum MessageType {

    // ── Auth ──────────────────────────────────────────────────
    LOGIN,          // client → server : connexion (from=username, content=password)
    REGISTER,       // client → server : inscription (from=username, content=password|email)
    AUTH_SUCCESS,   // server → client : auth réussie (content=username, to=userId)
    AUTH_FAIL,      // server → client : auth échouée (content=raison)
    LOGOUT,         // client → server : déconnexion

    // ── Système ───────────────────────────────────────────────
    SYSTEM,         // server → client : message informatif
    USER_LIST,      // server → tous  : liste des connectés (content=user1,user2,...)
    STATUS_UPDATE,  // server → tous  : statut (from=username, content=EN_LIGNE/NON_CONNECTE)
    USER_LIST_REQUEST, // client → server : demande de rafraîchir la liste
    SYNC_HISTORY,   // server → client : historique des messages (content=JSON history)

    // ── Messagerie texte ──────────────────────────────────────
    PRIVATE,        // message privé 1-à-1

    // ── Médias ────────────────────────────────────────────────
    PRIVATE_AUDIO,  // audio privé
    PRIVATE_IMAGE,  // image privée
    PRIVATE_FILE,   // fichier privé

    // ── Appel vidéo ───────────────────────────────────────────
    VIDEO_CALL_REQUEST,
    VIDEO_CALL_ACCEPT,
    VIDEO_CALL_REJECT,
    VIDEO_CALL_END,
    VIDEO_FRAME,

    // ── Appel vocal ───────────────────────────────────────────
    VOICE_CALL_REQUEST,
    VOICE_CALL_ACCEPT,
    VOICE_CALL_REJECT,
    VOICE_CALL_END,
    VOICE_FRAME
}
