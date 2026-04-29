package org.example.tpchatjavafx.common;

//Tous les types de messages échangés entre client et serveur.
 
public enum MessageType {

    //Auth
    LOGIN,          //  connexion 
    REGISTER,       // inscription 
    AUTH_SUCCESS,   //auth réussie 
    AUTH_FAIL,      //auth échouée 
    LOGOUT,         // déconnexion

    //Système
    SYSTEM,         // message informatif
    USER_LIST,      // liste des connectés 
    STATUS_UPDATE,  // statut 
    USER_LIST_REQUEST, //  demande de rafraîchir la liste
    SYNC_HISTORY,   // historique des messages 

    //Messagerie texte
    PRIVATE,        // message privé (pas utiliser pour le moment)

    // Médias
    PRIVATE_AUDIO,  // audio privé
    PRIVATE_IMAGE,  // image privée
    PRIVATE_FILE,   // fichier privé

    // Appel vidéo
    VIDEO_CALL_REQUEST,
    VIDEO_CALL_ACCEPT,
    VIDEO_CALL_REJECT,
    VIDEO_CALL_END,
    VIDEO_FRAME,//nombre d image envoiyer par seconde

    // Appel vocal
    VOICE_CALL_REQUEST,
    VOICE_CALL_ACCEPT,
    VOICE_CALL_REJECT,
    VOICE_CALL_END,
    VOICE_FRAME,//paquetes des donner d audio 

    // Contacts
    CONTACT_ADD,    // ajouter un contact 
    CONTACT_LOAD,   // charger ses contacts
    CONTACT_LIST,   // liste des noms 
    ERROR,          //  message d'erreur 
    HISTORY_REQUEST // demande l'historique 
}
