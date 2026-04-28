import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MessageDAO {

    public MessageDAO() {}

    /**
     * Sauvegarde le message et retourne l'ID généré par la BDD.
     */
    public int sauvegarderMessage(Message msg) {
        String contenuSec = msg.getContenu().replace("'", "''");
        String expSec = msg.getExpediteur().replace("'", "''");
        String destSec = msg.getDestinataire().replace("'", "''");

        String sql = "INSERT INTO messages (expediteur, destinataire, contenu) VALUES ('"
                + expSec + "', '" + destSec + "', '" + contenuSec + "')";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.executeUpdate(sql, Statement.RETURN_GENERATED_KEYS);
            ResultSet rs = stmt.getGeneratedKeys();

            if (rs.next()) {
                return rs.getInt(1); // Retourne le msgId
            }
        } catch (SQLException e) {
            System.err.println("[DB] Erreur sauvegarde message : " + e.getMessage());
        }
        return -1;
    }

    /**
     * Récupère l'historique entre deux utilisateurs.
     */
    public List<Message> getHistorique(String user1, String user2) {
        List<Message> historique = new ArrayList<>();
        String u1 = user1.replace("'", "''");
        String u2 = user2.replace("'", "''");

        String sql = "SELECT * FROM messages WHERE " +
                "(expediteur = '" + u1 + "' AND destinataire = '" + u2 + "') OR " +
                "(expediteur = '" + u2 + "' AND destinataire = '" + u1 + "') " +
                "ORDER BY date_envoi ASC";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                historique.add(new Message(
                        rs.getString("expediteur"),
                        rs.getString("destinataire"),
                        rs.getString("contenu"),
                        rs.getTimestamp("date_envoi")
                ));
            }
        } catch (SQLException e) {
            System.err.println("[DB] Erreur historique : " + e.getMessage());
        }
        return historique;
    }

    /**
     * Marque les messages reçus comme lus.
     */
    public void marquerTousLus(String destinataire, String expediteur) {
        String sql = "UPDATE messages SET est_lu = 1 WHERE destinataire = '"
                + destinataire.replace("'", "''") + "' AND expediteur = '"
                + expediteur.replace("'", "''") + "'";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            System.err.println("[DB] Erreur marquer lus : " + e.getMessage());
        }
    }
}