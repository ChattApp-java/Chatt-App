package org.example.tpchatjavafx.dao;

import org.example.tpchatjavafx.model.GroupeMembre;
import org.example.tpchatjavafx.model.Utilisateur;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class GroupeMembreDAO {

    public boolean addMember(int groupeId, int utilisateurId, String role) throws SQLException {
        String sql = "INSERT INTO groupe_membre (groupe_id, utilisateur_id, role) VALUES (?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE role = VALUES(role)";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, groupeId);
            stmt.setInt(2, utilisateurId);
            stmt.setString(3, normalizeRole(role));
            return stmt.executeUpdate() > 0;
        }
    }

    public boolean removeMember(int groupeId, int utilisateurId) throws SQLException {
        String sql = "DELETE FROM groupe_membre WHERE groupe_id = ? AND utilisateur_id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, groupeId);
            stmt.setInt(2, utilisateurId);
            return stmt.executeUpdate() > 0;
        }
    }

    public boolean removeMember(int groupeId, int requesterId, int utilisateurId, String systemMessage) throws SQLException {
        String role = getRole(groupeId, requesterId);
        if (requesterId != utilisateurId && !GroupeMembre.ROLE_ADMIN.equals(role)) {
            throw new SecurityException("Seul un admin peut retirer un membre.");
        }

        String deleteSql = "DELETE FROM groupe_membre WHERE groupe_id = ? AND utilisateur_id = ?";
        String messageSql = "INSERT INTO message (contenu, type, expediteur_id, groupe_id) VALUES (?, 'SYSTEM', ?, ?)";
        try (Connection conn = DatabaseConnection.getInstance().getConnection()) {
            boolean previousAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try (PreparedStatement deleteStmt = conn.prepareStatement(deleteSql);
                 PreparedStatement messageStmt = conn.prepareStatement(messageSql)) {
                deleteStmt.setInt(1, groupeId);
                deleteStmt.setInt(2, utilisateurId);
                boolean deleted = deleteStmt.executeUpdate() > 0;

                if (deleted && systemMessage != null && !systemMessage.isBlank()) {
                    messageStmt.setString(1, systemMessage);
                    messageStmt.setInt(2, requesterId);
                    messageStmt.setInt(3, groupeId);
                    messageStmt.executeUpdate();
                }

                conn.commit();
                conn.setAutoCommit(previousAutoCommit);
                return deleted;
            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                conn.setAutoCommit(previousAutoCommit);
                throw e;
            }
        }
    }

    public List<Utilisateur> getMembers(int groupeId) throws SQLException {
        List<Utilisateur> membres = new ArrayList<>();
        String sql = "SELECT u.* FROM utilisateur u JOIN groupe_membre gm ON gm.utilisateur_id = u.id " +
                "WHERE gm.groupe_id = ? ORDER BY u.username ASC";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, groupeId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) membres.add(mapUtilisateur(rs));
            }
        }
        return membres;
    }

    public List<Integer> getMemberIds(int groupeId) throws SQLException {
        List<Integer> ids = new ArrayList<>();
        String sql = "SELECT utilisateur_id FROM groupe_membre WHERE groupe_id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, groupeId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) ids.add(rs.getInt("utilisateur_id"));
            }
        }
        return ids;
    }

    public boolean isMember(int groupeId, int utilisateurId) throws SQLException {
        String sql = "SELECT 1 FROM groupe_membre WHERE groupe_id = ? AND utilisateur_id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, groupeId);
            stmt.setInt(2, utilisateurId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    public String getRole(int groupeId, int utilisateurId) throws SQLException {
        String sql = "SELECT role FROM groupe_membre WHERE groupe_id = ? AND utilisateur_id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, groupeId);
            stmt.setInt(2, utilisateurId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getString("role") : null;
            }
        }
    }

    public boolean updateRole(int groupeId, int utilisateurId, String role) throws SQLException {
        String sql = "UPDATE groupe_membre SET role = ? WHERE groupe_id = ? AND utilisateur_id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, normalizeRole(role));
            stmt.setInt(2, groupeId);
            stmt.setInt(3, utilisateurId);
            return stmt.executeUpdate() > 0;
        }
    }

    public List<Integer> getGroupIdsForUser(int utilisateurId) throws SQLException {
        List<Integer> ids = new ArrayList<>();
        String sql = "SELECT groupe_id FROM groupe_membre WHERE utilisateur_id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, utilisateurId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) ids.add(rs.getInt("groupe_id"));
            }
        }
        return ids;
    }

    private String normalizeRole(String role) {
        return GroupeMembre.ROLE_ADMIN.equalsIgnoreCase(String.valueOf(role))
                ? GroupeMembre.ROLE_ADMIN
                : GroupeMembre.ROLE_MEMBRE;
    }

    private Utilisateur mapUtilisateur(ResultSet rs) throws SQLException {
        Utilisateur u = new Utilisateur();
        u.setId(rs.getInt("id"));
        u.setUsername(rs.getString("username"));
        u.setEmail(rs.getString("email"));
        u.setPassword(rs.getString("password"));
        u.setStatut(rs.getString("statut"));
        try {
            Timestamp ts = rs.getTimestamp("derniereConnexion");
            if (ts != null) u.setDerniereConnexion(ts.toLocalDateTime());
        } catch (SQLException ignored) {
            Timestamp ts = rs.getTimestamp("derniere_connexion");
            if (ts != null) u.setDerniereConnexion(ts.toLocalDateTime());
        }
        return u;
    }
}
