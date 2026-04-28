package org.example.tpchatjavafx.common;

/**
 * Tous les types de messages échangés entre client et serveur.
 */
public enum MessageType {

    // ── Auth ──────────────────────────────────────────────────
    LOGIN,          // client → server : connexion (from=username, content=password)
    REGISTER,       // client → server : inscription (from=username, content=password|email)
    AUTH_SUCCESS,   // server → client : auth réussie (content=username)
    AUTH_FAIL,      // server → client : auth échouée (content=raison)
    LOGOUT,         // client → server : déconnexion

    // ── Système ───────────────────────────────────────────────
    SYSTEM,         // server → client : message informatif
    USER_LIST,      // server → tous  : liste des connectés (content=user1,user2,...)

    // ── Messagerie texte ──────────────────────────────────────
    PRIVATE,        // message privé 1-à-1
    GROUP,          // message de groupe
    JOIN_GROUP,     // rejoindre / créer un groupe

    // ── Médias ────────────────────────────────────────────────
    PRIVATE_AUDIO,  // audio privé
    GROUP_AUDIO,    // audio de groupe
    PRIVATE_IMAGE,  // image privée
    GROUP_IMAGE,    // image de groupe
    PRIVATE_FILE,   // fichier privé
    GROUP_FILE,     // fichier de groupe

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
