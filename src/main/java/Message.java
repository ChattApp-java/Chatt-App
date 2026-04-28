import java.io.Serializable;
import java.sql.Timestamp;

public class Message implements Serializable {
    private static final long serialVersionUID = 1L;

    private String expediteur; // Nom d'utilisateur (String)
    private String destinataire; // Nom d'utilisateur (String)
    private String contenu;
    private Timestamp dateEnvoi;

    // Constructeur complet pour le DAO (chargement BDD)
    public Message(String expediteur, String destinataire, String contenu, Timestamp dateEnvoi) {
        this.expediteur = expediteur;
        this.destinataire = destinataire;
        this.contenu = contenu;
        this.dateEnvoi = dateEnvoi;
    }

    // Constructeur simplifié pour l'envoi en temps réel
    public Message(String expediteur, String destinataire, String contenu) {
        this.expediteur = expediteur;
        this.destinataire = destinataire;
        this.contenu = contenu;
        this.dateEnvoi = new Timestamp(System.currentTimeMillis());
    }

    // Getters et Setters
    public String getExpediteur() { return expediteur; }
    public void setExpediteur(String expediteur) { this.expediteur = expediteur; }

    public String getDestinataire() { return destinataire; }
    public void setDestinataire(String destinataire) { this.destinataire = destinataire; }

    public String getContenu() { return contenu; }
    public void setContenu(String contenu) { this.contenu = contenu; }

    public Timestamp getDateEnvoi() { return dateEnvoi; }
    public void setDateEnvoi(Timestamp dateEnvoi) { this.dateEnvoi = dateEnvoi; }
}