package org.example.tpchatjavafx.common;

<<<<<<< HEAD
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
=======
//Tous les types de messages Ã©changÃ©s entre client et serveur.
 
public enum MessageType {

    //Auth
    LOGIN,          //  connexion 
    REGISTER,       // inscription 
    AUTH_SUCCESS,   //auth rÃ©ussie 
    AUTH_FAIL,      //auth Ã©chouÃ©e 
    LOGOUT,         // dÃ©connexion

    //SystÃ¨me
    SYSTEM,         // message informatif
    USER_LIST,      // liste des connectÃ©s 
    STATUS_UPDATE,  // statut 
    USER_LIST_REQUEST, //  demande de rafraÃ®chir la liste
    SYNC_HISTORY,   // historique des messages 

    //Messagerie texte
    PRIVATE,        // message privÃ© (pas utiliser pour le moment)

    // MÃ©dias
    PRIVATE_AUDIO,  // audio privÃ©
    PRIVATE_IMAGE,  // image privÃ©e
    PRIVATE_FILE,   // fichier privÃ©

    // Appel vidÃ©o
>>>>>>> a31ef27d82bb99f123d2561c113075e3f77704c8
    VIDEO_CALL_REQUEST,
    VIDEO_CALL_ACCEPT,
    VIDEO_CALL_REJECT,
    VIDEO_CALL_END,
    VIDEO_FRAME,//nombre d image envoiyer par seconde

<<<<<<< HEAD
    // -- Appel vocal -------------------------------------------
=======
    // Appel vocal
>>>>>>> a31ef27d82bb99f123d2561c113075e3f77704c8
    VOICE_CALL_REQUEST,
    VOICE_CALL_ACCEPT,
    VOICE_CALL_REJECT,
    VOICE_CALL_END,
    VOICE_FRAME,//paquetes des donner d audio 

<<<<<<< HEAD
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
=======
    // Contacts
    CONTACT_ADD,    // ajouter un contact 
    CONTACT_LOAD,   // charger ses contacts
    CONTACT_LIST,   // liste des noms 
    ERROR,          //  message d'erreur 
    HISTORY_REQUEST // demande l'historique 
>>>>>>> a31ef27d82bb99f123d2561c113075e3f77704c8
}
