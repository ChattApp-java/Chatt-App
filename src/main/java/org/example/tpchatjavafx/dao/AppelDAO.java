package org.example.tpchatjavafx.dao;

import org.example.tpchatjavafx.model.Appel;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AppelDAO {

    public Appel create(Appel appel) throws SQLException {
        String sql = "INSERT INTO appel (type_appel, date_heure, duree, statut, expediteur_id, destinataire_id, " +
                     "reunion_id, est_reunion) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, appel.getTypeAppel());
            pstmt.setTimestamp(2, appel.getDateHeure() != null
                    ? new Timestamp(appel.getDateHeure().getTime())
                    : new Timestamp(System.currentTimeMillis()));
            pstmt.setInt(3, appel.getDuree());
            pstmt.setString(4, appel.getStatut());
            pstmt.setInt(5, appel.getExpediteurId());
            pstmt.setInt(6, appel.getDestinataireId());
            if (appel.getReunionId() != null) pstmt.setInt(7, appel.getReunionId());
            else pstmt.setNull(7, Types.INTEGER);
            pstmt.setBoolean(8, appel.isEstReunion());

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating appel failed, no rows affected.");
            }

            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    appel.setId(generatedKeys.getInt(1));
                }
            }
            return appel;
        }
    }

    public Appel findById(int id) throws SQLException {
        String sql = "SELECT * FROM appel WHERE id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return mapAppel(rs);
            }
        }
        return null;
    }

    public List<Appel> getAppelsByUserId(int userId) throws SQLException {
        List<Appel> list = new ArrayList<>();
        String sql = "SELECT * FROM appel WHERE expediteur_id = ? OR destinataire_id = ? ORDER BY date_heure DESC";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) list.add(mapAppel(rs));
            }
        }
        return list;
    }

    /**
     * P2 : Retourne les appels liés à une réunion spécifique.
     */
    public List<Appel> getAppelsByReunionId(int reunionId) throws SQLException {
        List<Appel> list = new ArrayList<>();
        String sql = "SELECT * FROM appel WHERE reunion_id = ? ORDER BY date_heure DESC";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, reunionId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) list.add(mapAppel(rs));
            }
        }
        return list;
    }

    private Appel mapAppel(ResultSet rs) throws SQLException {
        Appel a = new Appel();
        a.setId(rs.getInt("id"));
        a.setTypeAppel(rs.getString("type_appel"));
        Timestamp ts = rs.getTimestamp("date_heure");
        if (ts != null) a.setDateHeure(new java.util.Date(ts.getTime()));
        a.setDuree(rs.getInt("duree"));
        a.setStatut(rs.getString("statut"));
        a.setExpediteurId(rs.getInt("expediteur_id"));
        a.setDestinataireId(rs.getInt("destinataire_id"));
        int rid = rs.getInt("reunion_id");
        if (!rs.wasNull()) a.setReunionId(rid);
        a.setEstReunion(rs.getBoolean("est_reunion"));
        return a;
    }
}
