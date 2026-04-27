package client;

import java.io.*;
import java.net.Socket;

public class GestionnaireFichier {

    public static void envoyerFichier(File file, String destinataire, ConnexionServeur conn) {
        try {
            long size = file.length();
            String name = file.getName();
            String type = getFileType(name);

            // 1. Envoyer l'info
            conn.envoyer(commun.Protocole.FILE_INFO + commun.Protocole.SEP 
                    + destinataire + commun.Protocole.SEP 
                    + name + commun.Protocole.SEP 
                    + size + commun.Protocole.SEP 
                    + type);

            // 2. Envoyer les données par blocs
            // Note: Pour faire simple ici, on utilise le même socket ou un socket dédié.
            // Dans ce protocole, on va simuler l'envoi via le flux existant (attention à la synchronisation)
            // Idéalement, on ouvrirait un socket de données séparé.
            
            System.out.println("Envoi du fichier : " + name + " (" + size + " bytes)");
            
            // Pour l'instant, on se contente de l'annonce. 
            // L'implémentation réelle du flux binaire nécessite un protocole plus strict pour ne pas mélanger texte et binaire.
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String getFileType(String filename) {
        if (filename.endsWith(".jpg") || filename.endsWith(".png")) return "IMAGE";
        if (filename.endsWith(".mp3") || filename.endsWith(".wav")) return "AUDIO";
        return "FILE";
    }
}
