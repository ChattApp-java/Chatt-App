package org.example.tpchatjavafx.dao;

import org.example.tpchatjavafx.model.Utilisateur;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ContactDAO {

    public boolean addContact(int utilisateurId, int contactId) throws SQLException {
        String sql = "INSERT INTO contact (utilisateur_id, contact_id) VALUES (?, ?)";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, utilisateurId);
            stmt.setInt(2, contactId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            // Ignorer si le contact existe déjà (violation de contrainte unique)
            if (e.getErrorCode() == 1062) {
                return false;
            }
            throw e;
        }
    }

    public List<Utilisateur> getContacts(int utilisateurId) throws SQLException {
        List<Utilisateur> contacts = new ArrayList<>();
        String sql = "SELECT u.* FROM utilisateur u " +
                     "JOIN contact c ON u.id = c.contact_id " +
                     "WHERE c.utilisateur_id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, utilisateurId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Utilisateur u = new Utilisateur();
                    u.setId(rs.getInt("id"));
                    u.setUsername(rs.getString("username"));
                    u.setEmail(rs.getString("email"));
                    u.setStatut(rs.getString("statut"));
                    if (rs.getTimestamp("derniere_connexion") != null) {
                        u.setDerniereConnexion(rs.getTimestamp("derniere_connexion").toLocalDateTime());
                    }
                    contacts.add(u);
                }
            }
        }
        return contacts;
    }
}
