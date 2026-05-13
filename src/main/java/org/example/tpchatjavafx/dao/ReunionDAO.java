package org.example.tpchatjavafx.dao;

import org.example.tpchatjavafx.model.Reunion;
import org.example.tpchatjavafx.model.Utilisateur;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ReunionDAO {

    public Reunion create(int groupeId, int initiateurId, String type) throws SQLException {
        String sql = "INSERT INTO reunion (groupe_id, initiateur_id, type) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, groupeId);
            stmt.setInt(2, initiateurId);
            stmt.setString(3, normalizeType(type));
            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) return findById(rs.getInt(1));
            }
        }
        throw new SQLException("Creation de la reunion echouee, aucun ID genere.");
    }

    public Reunion findById(int id) throws SQLException {
        String sql = "SELECT * FROM reunion WHERE id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return mapReunion(rs);
            }
        }
        return null;
    }

    public List<Reunion> getHistoryByGroupeId(int groupeId) throws SQLException {
        List<Reunion> reunions = new ArrayList<>();
        String sql = "SELECT * FROM reunion WHERE groupe_id = ? ORDER BY date_debut DESC";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, groupeId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) reunions.add(mapReunion(rs));
            }
        }
        return reunions;
    }

    public Reunion findActiveByGroupeId(int groupeId) throws SQLException {
        String sql = "SELECT * FROM reunion WHERE groupe_id = ? AND statut = 'EN_COURS' ORDER BY date_debut DESC LIMIT 1";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, groupeId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return mapReunion(rs);
            }
        }
        return null;
    }

    public boolean endMeeting(int reunionId) throws SQLException {
        String sql = "UPDATE reunion SET statut = 'TERMINEE', date_fin = CURRENT_TIMESTAMP WHERE id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, reunionId);
            return stmt.executeUpdate() > 0;
        }
    }

    public boolean addParticipant(int reunionId, int utilisateurId) throws SQLException {
        String closePrevious = "UPDATE reunion_participant SET date_leave = CURRENT_TIMESTAMP " +
                "WHERE reunion_id = ? AND utilisateur_id = ? AND date_leave IS NULL";
        String insert = "INSERT INTO reunion_participant (reunion_id, utilisateur_id) VALUES (?, ?)";
        try (Connection conn = DatabaseConnection.getInstance().getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement closeStmt = conn.prepareStatement(closePrevious);
                 PreparedStatement insertStmt = conn.prepareStatement(insert)) {
                closeStmt.setInt(1, reunionId);
                closeStmt.setInt(2, utilisateurId);
                closeStmt.executeUpdate();
                insertStmt.setInt(1, reunionId);
                insertStmt.setInt(2, utilisateurId);
                boolean ok = insertStmt.executeUpdate() > 0;
                conn.commit();
                return ok;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    public boolean removeParticipant(int reunionId, int utilisateurId) throws SQLException {
        String sql = "UPDATE reunion_participant SET date_leave = CURRENT_TIMESTAMP " +
                "WHERE reunion_id = ? AND utilisateur_id = ? AND date_leave IS NULL";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, reunionId);
            stmt.setInt(2, utilisateurId);
            return stmt.executeUpdate() > 0;
        }
    }

    public List<Utilisateur> getCurrentParticipants(int reunionId) throws SQLException {
        List<Utilisateur> participants = new ArrayList<>();
        String sql = "SELECT u.* FROM utilisateur u JOIN reunion_participant rp ON rp.utilisateur_id = u.id " +
                "WHERE rp.reunion_id = ? AND rp.date_leave IS NULL ORDER BY u.username ASC";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, reunionId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) participants.add(mapUtilisateur(rs));
            }
        }
        return participants;
    }

    private String normalizeType(String type) {
        return Reunion.TYPE_VIDEO.equalsIgnoreCase(String.valueOf(type)) ? Reunion.TYPE_VIDEO : Reunion.TYPE_AUDIO;
    }

    private Reunion mapReunion(ResultSet rs) throws SQLException {
        Reunion r = new Reunion();
        r.setId(rs.getInt("id"));
        r.setGroupeId(rs.getInt("groupe_id"));
        r.setInitiateurId(rs.getInt("initiateur_id"));
        r.setType(rs.getString("type"));
        r.setStatut(rs.getString("statut"));
        Timestamp debut = rs.getTimestamp("date_debut");
        Timestamp fin = rs.getTimestamp("date_fin");
        if (debut != null) r.setDateDebut(debut.toLocalDateTime());
        if (fin != null) r.setDateFin(fin.toLocalDateTime());
        return r;
    }

    private Utilisateur mapUtilisateur(ResultSet rs) throws SQLException {
        Utilisateur u = new Utilisateur();
        u.setId(rs.getInt("id"));
        u.setUsername(rs.getString("username"));
        u.setEmail(rs.getString("email"));
        u.setPassword(rs.getString("password"));
        u.setStatut(rs.getString("statut"));
        return u;
    }
}
