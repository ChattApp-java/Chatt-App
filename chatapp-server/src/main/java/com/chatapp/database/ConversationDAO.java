package com.chatapp.database;

import com.chatapp.model.Conversation;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * ConversationDAO.java
 * Accès aux données pour les conversations (individuelles et de groupe).
 */
public class ConversationDAO {

    public ConversationDAO() {}

    // ── Créer une conversation ────────────────────────────────────
    public int creerConversation(Conversation conv) {
        String sql = "INSERT INTO conversations (type, groupe_id, utilisateur1_id, utilisateur2_id, date_creation) "
                   + "VALUES (?, ?, ?, ?, NOW())";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, conv.getType().name());
            if (conv.getGroupeId() != null) {
                stmt.setInt(2, conv.getGroupeId());
            } else {
                stmt.setNull(2, Types.INTEGER);
            }
            stmt.setInt(3, conv.getUtilisateur1Id());
            stmt.setInt(4, conv.getUtilisateur2Id());

            stmt.executeUpdate();
            ResultSet keys = stmt.getGeneratedKeys();
            if (keys.next()) {
                int id = keys.getInt(1);
                System.out.println("[DB] Conversation créée, id=" + id);
                return id;
            }

        } catch (SQLException e) {
            System.err.println("[DB] Erreur creerConversation : " + e.getMessage());
        }
        return -1;
    }

    // ── Trouver une conversation individuelle entre deux utilisateurs ─
    public Conversation getConversationIndividuelle(int user1Id, int user2Id) {
        String sql = "SELECT * FROM conversations "
                   + "WHERE type = 'INDIVIDUEL' "
                   + "  AND ((utilisateur1_id = ? AND utilisateur2_id = ?) "
                   + "    OR (utilisateur1_id = ? AND utilisateur2_id = ?))";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, user1Id);
            stmt.setInt(2, user2Id);
            stmt.setInt(3, user2Id);
            stmt.setInt(4, user1Id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return extraireConversation(rs);

        } catch (SQLException e) {
            System.err.println("[DB] Erreur getConversationIndividuelle : " + e.getMessage());
        }
        return null;
    }

    // ── Récupérer toutes les conversations d'un utilisateur ───────
    public List<Conversation> getConversationsByUserId(int userId) {
        List<Conversation> liste = new ArrayList<>();
        String sql = "SELECT * FROM conversations "
                   + "WHERE utilisateur1_id = ? OR utilisateur2_id = ? "
                   + "ORDER BY dernier_message DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            stmt.setInt(2, userId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) liste.add(extraireConversation(rs));
            System.out.println("[DB] Conversations pour user " + userId + " : " + liste.size());

        } catch (SQLException e) {
            System.err.println("[DB] Erreur getConversationsByUserId : " + e.getMessage());
        }
        return liste;
    }

    /**
     * Récupère toutes les conversations liées à un groupe donné.
     * @param groupeId identifiant du groupe
     * @return liste des conversations du groupe
     */
    public List<Conversation> getConversationsByGroupeId(int groupeId) {
        List<Conversation> liste = new ArrayList<>();
        String sql = "SELECT * FROM conversations WHERE groupe_id = ? ORDER BY dernier_message DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, groupeId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) liste.add(extraireConversation(rs));
            System.out.println("[DB] Conversations du groupe " + groupeId + " : " + liste.size());

        } catch (SQLException e) {
            System.err.println("[DB] Erreur getConversationsByGroupeId : " + e.getMessage());
        }
        return liste;
    }

    // ── Mettre à jour la date du dernier message ──────────────────
    public void mettreAJourDernierMessage(int conversationId) {
        String sql = "UPDATE conversations SET dernier_message = NOW() WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, conversationId);
            stmt.executeUpdate();

        } catch (SQLException e) {
            System.err.println("[DB] Erreur mettreAJourDernierMessage : " + e.getMessage());
        }
    }

    // ── Supprimer une conversation ────────────────────────────────
    public boolean supprimerConversation(int id) {
        String sql = "DELETE FROM conversations WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("[DB] Erreur supprimerConversation : " + e.getMessage());
            return false;
        }
    }

    // ── Extraction depuis ResultSet ────────────────────────────────
    private Conversation extraireConversation(ResultSet rs) throws SQLException {
        Integer groupeId = rs.getObject("groupe_id") != null
                ? rs.getInt("groupe_id") : null;

        return new Conversation(
                rs.getInt("id"),
                Conversation.Type.valueOf(rs.getString("type")),
                groupeId,
                rs.getInt("utilisateur1_id"),
                rs.getInt("utilisateur2_id"),
                rs.getTimestamp("date_creation") != null
                        ? rs.getTimestamp("date_creation").toLocalDateTime() : null,
                rs.getTimestamp("dernier_message") != null
                        ? rs.getTimestamp("dernier_message").toLocalDateTime() : null
        );
    }
}
