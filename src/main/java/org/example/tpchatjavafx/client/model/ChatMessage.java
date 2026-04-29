package org.example.tpchatjavafx.client.model;

import org.example.tpchatjavafx.common.MessageType;
import java.util.Base64;

public class ChatMessage {

    // Type de message (texte, audio, image, fichier, appel, etc.)
    private MessageType type;
    // ID du message
    private int messageId;
    // Expéditeur du message (username ou string userId)
    private String from;
    // Destinataire privé (username)
    private String to;
    // ID de la conversation
    private String conversationId;
    // Contenu textuel du message
    private String content;
    // Données binaires du message (audio, image, fichier)
    private byte[] binaryData;
    // Horodatage du message (HH:mm)
    private String timestamp;

    // Constructeur principal
    public ChatMessage(MessageType type, String from, String to, String conversationId, String content) {
        this.type = type;
        this.from = from;
        this.to = to;
        this.conversationId = conversationId;
        this.content = content;
        this.messageId = -1;
    }

    // ---------------- GETTERS & SETTERS ----------------
    public MessageType getType() { return type; }
    public String getFrom() { return from; }
    public String getTo()   { return to; }
    public String getConversationId() { return conversationId; }
    public String getContent() { return content; }
    
    public int getMessageId() { return messageId; }
    public void setMessageId(int messageId) { this.messageId = messageId; }

    // ---------------- BINARY DATA ----------------
    public byte[] getBinaryData() { return binaryData; }
    public void setBinaryData(byte[] data) { this.binaryData = data; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    // ---------------- SERIALIZATION ----------------
    // Sérialise le message en une seule ligne texte pour l'envoyer via le réseau
    // Format : type|messageId|from|to|conversationId|content|base64(binary)
    public String serialize() {
        // Convertit les données binaires en Base64 pour pouvoir les envoyer en texte
        String base64 = (binaryData == null) ? "" : Base64.getEncoder().encodeToString(binaryData);

        return type + "|" +     // type du message
                messageId + "|" + 
                safe(from) + "|" +   // expéditeur
                safe(to) + "|" +     // destinataire privé
                safe(conversationId) + "|" + // ID de la conversation
                safe(content) + "|" + // contenu textuel
                base64 + "|" +        // données binaires encodées
                safe(timestamp);      // horodatage
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
            if (parts.length < 6) return null;

            MessageType type = MessageType.valueOf(parts[0]);
            int messageId    = Integer.parseInt(parts[1]);
            String from      = parts[2].replace("␟", "|");
            String to        = parts[3].replace("␟", "|");
            String convId    = parts[4].replace("␟", "|");
            String content   = parts[5].replace("␟", "|");

            // Crée un objet ChatMessage avec les données textuelles
            ChatMessage msg = new ChatMessage(type, from, to, convId, content);
            msg.setMessageId(messageId);

            // Décodage Base64 si binaire
            if (parts.length >= 7 && !parts[6].isEmpty()) {
                msg.setBinaryData(Base64.getDecoder().decode(parts[6]));
            }
            
            // Horodatage (8ème champ si présent)
            if (parts.length >= 8) {
                msg.setTimestamp(parts[7].replace("␟", "|"));
            }

            return msg;
        } catch (Exception e) {
            System.err.println("[ChatMessage] Erreur désérialisation: " + e.getMessage());
            return null;
        }
    }
}
