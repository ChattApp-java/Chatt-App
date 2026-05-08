package org.example.tpchatjavafx.common;

/**
 * Tous les types de messages échangés entre client et serveur.
 */
public enum MessageType {

    // -- Auth --------------------------------------------------
    LOGIN,
    REGISTER,
    AUTH_SUCCESS,
    AUTH_FAIL,
    LOGOUT,

    // -- Système -----------------------------------------------
    SYSTEM,
    USER_LIST,
    STATUS_UPDATE,
    USER_LIST_REQUEST,
    SYNC_HISTORY,

    // -- Messagerie texte --------------------------------------
    PRIVATE,

    // -- Médias --------------------------------------------
    PRIVATE_AUDIO,
    PRIVATE_IMAGE,
    PRIVATE_FILE,

    // -- Appel vidéo -------------------------------------------
    VIDEO_CALL_REQUEST,
    VIDEO_CALL_ACCEPT,
    VIDEO_CALL_REJECT,
    VIDEO_CALL_END,
    VIDEO_FRAME,

    // -- Appel vocal -------------------------------------------
    VOICE_CALL_REQUEST,
    VOICE_CALL_ACCEPT,
    VOICE_CALL_REJECT,
    VOICE_CALL_END,
    VOICE_FRAME,

    // -- Signalisation appels générique ------------------------
    CALL_REQUEST,
    CALL_ANSWER,
    CALL_REJECT,
    CALL_END,
    CALL_INCOMING,
    CALL_INFO,

    // -- Groupes ----------------------------------------------
    GROUP_CREATE,
    GROUP_UPDATE,
    GROUP_DELETE,
    GROUP_JOIN,
    GROUP_LEAVE,
    GROUP_MESSAGE,
    GROUP_MEMBER_ADD,
    GROUP_MEMBER_REMOVE,

    // -- Réunions / Meeting ------------------------------------
    MEETING_INVITE,
    MEETING_STARTED,
    MEETING_ENDED,
    MEETING_PARTICIPANT_JOINED,
    MEETING_PARTICIPANT_LEFT,
    MEETING_INFO,
    MEETING_AUDIO_FRAME,
    MEETING_VIDEO_FRAME,

    // -- Contacts ----------------------------------------------
    CONTACT_ADD,
    CONTACT_LOAD,
    CONTACT_LIST,
    HISTORY_REQUEST,

    ERROR
}
