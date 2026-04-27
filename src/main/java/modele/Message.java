package modele;

import java.time.LocalDateTime;

/**
 * Représente un message dans la base de données.
 */
public class Message {

    private int id_message;
    private int id_sender;
    private int id_receiver;
    private String contenu;
    private LocalDateTime date_envoi;
    private String statut;
    private String typeMessage; // TEXT, IMAGE, FILE, AUDIO
    private String filePath;

    public Message(int id_message, int id_sender, int id_receiver,
                   String contenu, LocalDateTime date_envoi, String statut) {
        this(id_message, id_sender, id_receiver, contenu, date_envoi, statut, "TEXT", null);
    }

    public Message(int id_message, int id_sender, int id_receiver,
                   String contenu, LocalDateTime date_envoi, String statut,
                   String typeMessage, String filePath) {
        this.id_message = id_message;
        this.id_sender = id_sender;
        this.id_receiver = id_receiver;
        this.contenu = contenu;
        this.date_envoi = date_envoi;
        this.statut = statut;
        this.typeMessage = typeMessage;
        this.filePath = filePath;
    }

    public Message(int id_sender, int id_receiver, String contenu) {
        this.id_sender = id_sender;
        this.id_receiver = id_receiver;
        this.contenu = contenu;
        this.statut = "non_lu";
        this.typeMessage = "TEXT";
    }

    public int getId_message() { return id_message; }
    public int getId_sender() { return id_sender; }
    public int getId_receiver() { return id_receiver; }
    public String getContenu() { return contenu; }
    public LocalDateTime getDate_envoi() { return date_envoi; }
    public String getStatut() { return statut; }
    public String getTypeMessage() { return typeMessage; }
    public String getFilePath() { return filePath; }

    public void setId_message(int id_message) { this.id_message = id_message; }
    public void setStatut(String statut) { this.statut = statut; }
    public void setDate_envoi(LocalDateTime d) { this.date_envoi = d; }
    public void setTypeMessage(String type) { this.typeMessage = type; }
    public void setFilePath(String path) { this.filePath = path; }

    @Override
    public String toString() {
        return "Message{id=" + id_message
                + ", de=" + id_sender
                + ", vers=" + id_receiver
                + ", type=" + typeMessage
                + ", contenu='" + contenu + "'"
                + "}";
    }
}