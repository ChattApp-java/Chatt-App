package com.chatapp.database;

import com.chatapp.model.Appel;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * AppelDAO.java
 * Accès aux données pour les appels (audio/vidéo), individuels et réunions.
 */
public class AppelDAO {

    public AppelDAO() {}

    // ── Enregistrer un appel ───────────────────────────────────────
    public int enregistrerAppel(Appel appel) {
        String sql = "INSERT INTO appels (appelant_id, recepteur_id, type, statut, date_debut, reunion_id, est_reunion) "
                   + "VALUES (?, ?, ?, ?, NOW(), ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, appel.getAppelantId());
            stmt.setInt(2, appel.getRecepteurId());
            stmt.setString(3, appel.getType().name());
            stmt.setString(4, appel.getStatut().name());
            if (appel.getReunionId() != null) {
                stmt.setInt(5, appel.getReunionId());
            } else {
                stmt.setNull(5, Types.INTEGER);
            }
            stmt.setBoolean(6, appel.isEstReunion());

            stmt.executeUpdate();
            ResultSet keys = stmt.getGeneratedKeys();
            if (keys.next()) {
                int id = keys.getInt(1);
                System.out.println("[DB] Appel enregistré, id=" + id);
                return id;
            }

        } catch (SQLException e) {
            System.err.println("[DB] Erreur enregistrerAppel : " + e.getMessage());
        }
        return -1;
    }

    // ── Mettre à jour le statut d'un appel ────────────────────────
    public void mettreAJourStatut(int idAppel, Appel.StatutAppel statut) {
        String sql = statut == Appel.StatutAppel.TERMINE
                ? "UPDATE appels SET statut = ?, date_fin = NOW() WHERE id = ?"
                : "UPDATE appels SET statut = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, statut.name());
            stmt.setInt(2, idAppel);
            stmt.executeUpdate();
            System.out.println("[DB] Statut appel " + idAppel + " → " + statut);

        } catch (SQLException e) {
            System.err.println("[DB] Erreur mettreAJourStatut : " + e.getMessage());
        }
    }

    // ── Historique des appels entre deux utilisateurs ─────────────
    public List<Appel> getHistoriqueAppels(int user1Id, int user2Id) {
        List<Appel> liste = new ArrayList<>();
        String sql = "SELECT * FROM appels "
                   + "WHERE (appelant_id = ? AND recepteur_id = ?) "
                   + "   OR (appelant_id = ? AND recepteur_id = ?) "
                   + "ORDER BY date_debut DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, user1Id);
            stmt.setInt(2, user2Id);
            stmt.setInt(3, user2Id);
            stmt.setInt(4, user1Id);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) liste.add(extraireAppel(rs));
            System.out.println("[DB] Historique appels : " + liste.size() + " appel(s).");

        } catch (SQLException e) {
            System.err.println("[DB] Erreur getHistoriqueAppels : " + e.getMessage());
        }
        return liste;
    }

    /**
     * Récupère tous les appels liés à une réunion donnée.
     * @param reunionId identifiant de la réunion
     * @return liste des appels de la réunion
     */
    public List<Appel> getAppelsByReunionId(int reunionId) {
        List<Appel> liste = new ArrayList<>();
        String sql = "SELECT * FROM appels WHERE reunion_id = ? ORDER BY date_debut ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, reunionId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) liste.add(extraireAppel(rs));
            System.out.println("[DB] Appels de la réunion " + reunionId + " : " + liste.size());

        } catch (SQLException e) {
            System.err.println("[DB] Erreur getAppelsByReunionId : " + e.getMessage());
        }
        return liste;
    }

    // ── Appels manqués d'un utilisateur ───────────────────────────
    public List<Appel> getAppelsManques(int userId) {
        List<Appel> liste = new ArrayList<>();
        String sql = "SELECT * FROM appels WHERE recepteur_id = ? AND statut = 'MANQUE' ORDER BY date_debut DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) liste.add(extraireAppel(rs));
            System.out.println("[DB] Appels manqués pour user " + userId + " : " + liste.size());

        } catch (SQLException e) {
            System.err.println("[DB] Erreur getAppelsManques : " + e.getMessage());
        }
        return liste;
    }

    // ── Extraction depuis ResultSet ────────────────────────────────
    private Appel extraireAppel(ResultSet rs) throws SQLException {
        Integer reunionId = rs.getObject("reunion_id") != null
                ? rs.getInt("reunion_id") : null;

        return new Appel(
                rs.getInt("id"),
                rs.getInt("appelant_id"),
                rs.getInt("recepteur_id"),
                Appel.TypeAppel.valueOf(rs.getString("type")),
                Appel.StatutAppel.valueOf(rs.getString("statut")),
                rs.getTimestamp("date_debut") != null
                        ? rs.getTimestamp("date_debut").toLocalDateTime() : null,
                rs.getTimestamp("date_fin") != null
                        ? rs.getTimestamp("date_fin").toLocalDateTime() : null,
                reunionId,
                rs.getBoolean("est_reunion")
        );
    }
}
