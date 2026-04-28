package org.example.tpchatjavafx.dao;

import org.example.tpchatjavafx.model.Utilisateur;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UtilisateurDAO {

    public Utilisateur create(Utilisateur utilisateur) throws SQLException {
        String sql = "INSERT INTO utilisateur (username, email, password) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, utilisateur.getUsername());
            stmt.setString(2, utilisateur.getEmail());
            stmt.setString(3, utilisateur.getPassword());
            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    utilisateur.setId(rs.getInt(1));
                }
            }
        }
        return utilisateur;
    }

    public Utilisateur findById(int id) throws SQLException {
        String sql = "SELECT * FROM utilisateur WHERE id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToUtilisateur(rs);
                }
            }
        }
        return null;
    }

    public Utilisateur findByUsername(String username) throws SQLException {
        String sql = "SELECT * FROM utilisateur WHERE username = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToUtilisateur(rs);
                }
            }
        }
        return null;
    }

    public List<Utilisateur> listAll() throws SQLException {
        List<Utilisateur> list = new ArrayList<>();
        String sql = "SELECT * FROM utilisateur";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                list.add(mapResultSetToUtilisateur(rs));
            }
        }
        return list;
    }
    
    public List<Utilisateur> searchByUsername(String query) throws SQLException {
        List<Utilisateur> list = new ArrayList<>();
        String sql = "SELECT * FROM utilisateur WHERE username LIKE ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, "%" + query + "%");
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToUtilisateur(rs));
                }
            }
        }
        return list;
    }

    private Utilisateur mapResultSetToUtilisateur(ResultSet rs) throws SQLException {
        Utilisateur u = new Utilisateur();
        u.setId(rs.getInt("id"));
        u.setUsername(rs.getString("username"));
        u.setEmail(rs.getString("email"));
        u.setPassword(rs.getString("password"));
        u.setStatut(rs.getString("statut"));
        Timestamp ts = rs.getTimestamp("derniere_connexion");
        if (ts != null) {
            u.setDerniereConnexion(ts.toLocalDateTime());
        }
        return u;
    }
}
