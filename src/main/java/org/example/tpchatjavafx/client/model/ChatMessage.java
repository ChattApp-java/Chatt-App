package org.example.tpchatjavafx.client.model;

import org.example.tpchatjavafx.common.MessageType;
import java.util.Base64;

public class ChatMessage {

    // Type de message (texte, audio, image, fichier, appel, etc.)
    private MessageType type;
    // Expéditeur du message
    private String from;
    // Destinataire privé (null si message de groupe)
    private String to;
    // ID du groupe (null si message privé)
    private String groupId;
    // Contenu textuel du message
    private String content;
    // Contenu binaire du message (audio, image, fichier)
    private byte[] binaryData;

    // Constructeur principal
    public ChatMessage(MessageType type, String from, String to, String groupId, String content) {
        this.type = type;
        this.from = from;
        this.to = to;
        this.groupId = groupId;
        this.content = content;
    }

    // ---------------- GETTERS ----------------
    public MessageType getType() { return type; }
    public String getFrom() { return from; }
    public String getTo()   { return to; }
    public String getGroupId() { return groupId; }
    public String getContent() { return content; }

    // ---------------- BINARY DATA ----------------
    public byte[] getBinaryData() { return binaryData; }
    public void setBinaryData(byte[] data) { this.binaryData = data; }

    // ---------------- SERIALIZATION ----------------
    // Sérialise le message en une seule ligne texte pour l'envoyer via le réseau
    // Format : type|from|to|groupId|content|base64(binary)
    public String serialize() {
        // Convertit les données binaires en Base64 pour pouvoir les envoyer en texte
        String base64 = (binaryData == null) ? "" : Base64.getEncoder().encodeToString(binaryData);

        return type + "|" +     // type du message
                safe(from) + "|" +   // expéditeur
                safe(to) + "|" +     // destinataire privé
                safe(groupId) + "|" + // ID du groupe
                safe(content) + "|" + // contenu textuel
                base64;               // données binaires encodées
    }

    // Remplace les caractères '|' par un caractère spécial pour éviter les conflits
    private String safe(String s) {
        return (s == null ? "" : s.replace("|", "␟"));
    }

    // ---------------- DESERIALIZATION ----------------
    // Transforme une ligne texte reçue en objet ChatMessage
    public static ChatMessage deserialize(String line) {
        try {
            // Sépare la ligne en parties, y compris les champs vides
            String[] parts = line.split("\\|", -1);
            if (parts.length < 5) return null;

            MessageType type = MessageType.valueOf(parts[0]);
            String from    = parts[1].replace("␟", "|");
            String to      = parts[2].replace("␟", "|");
            String groupId = parts[3].replace("␟", "|");
            String content = parts[4].replace("␟", "|");

            // Crée un objet ChatMessage avec les données textuelles
            ChatMessage msg = new ChatMessage(type, from, to, groupId, content);

            // Si des données binaires sont présentes, on les décode depuis Base64
            if (parts.length >= 6 && !parts[5].isEmpty()) {
                msg.setBinaryData(Base64.getDecoder().decode(parts[5]));
            }

            return msg;
        } catch (Exception e) {
            System.err.println("[ChatMessage] Erreur désérialisation: " + e.getMessage());
            return null;
        }
    }
}
