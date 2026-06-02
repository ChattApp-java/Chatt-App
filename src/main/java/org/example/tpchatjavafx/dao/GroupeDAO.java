package org.example.tpchatjavafx.dao;

import org.example.tpchatjavafx.model.Groupe;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class GroupeDAO {

    public Groupe create(String nom, String description, int createurId) throws SQLException {
        String sql = "INSERT INTO groupe (nom, description, createur_id) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, nom);
            stmt.setString(2, description);
            stmt.setInt(3, createurId);
            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return findById(rs.getInt(1));
                }
            }
        }
        throw new SQLException("Creation du groupe echouee, aucun ID genere.");
    }

    public Groupe findById(int id) throws SQLException {
        String sql = "SELECT * FROM groupe WHERE id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return map(rs);
            }
        }
        return null;
    }

    public List<Groupe> findByUtilisateurId(int utilisateurId) throws SQLException {
        List<Groupe> groupes = new ArrayList<>();
        String sql = "SELECT DISTINCT g.* FROM groupe g " +
                "LEFT JOIN groupe_membre gm ON gm.groupe_id = g.id " +
                "WHERE gm.utilisateur_id = ? " +
                "OR (g.createur_id = ? AND NOT EXISTS (" +
                "    SELECT 1 FROM groupe_membre gm2 WHERE gm2.groupe_id = g.id" +
                ")) " +
                "ORDER BY g.date_creation DESC";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, utilisateurId);
            stmt.setInt(2, utilisateurId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) groupes.add(map(rs));
            }
        }
        return groupes;
    }

    public boolean update(int id, String nom, String description) throws SQLException {
        String sql = "UPDATE groupe SET nom = ?, description = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, nom);
            stmt.setString(2, description);
            stmt.setInt(3, id);
            return stmt.executeUpdate() > 0;
        }
    }

    public boolean delete(int id) throws SQLException {
        String sql = "DELETE FROM groupe WHERE id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        }
    }

    public List<Groupe> searchByNom(String query) throws SQLException {
        List<Groupe> groupes = new ArrayList<>();
        String sql = "SELECT * FROM groupe WHERE nom LIKE ? ORDER BY nom ASC";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, "%" + (query == null ? "" : query) + "%");
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) groupes.add(map(rs));
            }
        }
        return groupes;
    }

    private Groupe map(ResultSet rs) throws SQLException {
        Groupe groupe = new Groupe();
        groupe.setId(rs.getInt("id"));
        groupe.setNom(rs.getString("nom"));
        groupe.setDescription(rs.getString("description"));
        groupe.setCreateurId(rs.getInt("createur_id"));
        Timestamp created = rs.getTimestamp("date_creation");
        if (created != null) groupe.setDateCreation(created.toLocalDateTime());
        return groupe;
    }
}
