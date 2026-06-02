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
        if (contactExists(utilisateurId, contactId)) {
            return false;
        }
        String sql = "INSERT INTO contact (utilisateur_id, contact_id) VALUES (?, ?)";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, utilisateurId);
            stmt.setInt(2, contactId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {

            if (e.getErrorCode() == 1062) {
                return false;
            }
            throw e;
        }
    }

    public boolean contactExists(int utilisateurId, int contactId) throws SQLException {
        String sql = "SELECT 1 FROM contact " +
                     "WHERE utilisateur_id = ? AND contact_id = ? AND COALESCE(is_deleted, FALSE) = FALSE LIMIT 1";
        String fallbackSql = "SELECT 1 FROM contact WHERE utilisateur_id = ? AND contact_id = ? LIMIT 1";
        try {
            return contactExistsWithSql(utilisateurId, contactId, sql);
        } catch (SQLException e) {
            if (!isMissingColumn(e)) throw e;
            return contactExistsWithSql(utilisateurId, contactId, fallbackSql);
        }
    }

    public boolean deleteContact(int utilisateurId, int contactId) throws SQLException {
        return hardDelete(contactId, utilisateurId);
    }

    public boolean hardDelete(int contactId, int userId) throws SQLException {
        String sql = "DELETE FROM contact WHERE utilisateur_id = ? AND contact_id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, contactId);
            return stmt.executeUpdate() > 0;
        }
    }

    public List<Utilisateur> findByUserId(int utilisateurId) throws SQLException {
        return getContacts(utilisateurId);
    }

    public List<Utilisateur> getContacts(int utilisateurId) throws SQLException {
        List<Utilisateur> contacts = new ArrayList<>();
        String sql = "SELECT u.* FROM utilisateur u " +
                     "JOIN contact c ON u.id = c.contact_id " +
                     "WHERE c.utilisateur_id = ? AND COALESCE(c.is_deleted, FALSE) = FALSE";
        String fallbackSql = "SELECT u.* FROM utilisateur u " +
                     "JOIN contact c ON u.id = c.contact_id " +
                     "WHERE c.utilisateur_id = ?";
        try {
            loadContacts(utilisateurId, contacts, sql);
        } catch (SQLException e) {
            if (!isMissingColumn(e)) throw e;
            contacts.clear();
            loadContacts(utilisateurId, contacts, fallbackSql);
        }
        return contacts;
    }

    private void loadContacts(int utilisateurId, List<Utilisateur> contacts, String sql) throws SQLException {
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
                    if (rs.getTimestamp("derniereConnexion") != null) {
                        u.setDerniereConnexion(rs.getTimestamp("derniereConnexion").toLocalDateTime());
                    }
                    contacts.add(u);
                }
            }
        }
    }

    private boolean contactExistsWithSql(int utilisateurId, int contactId, String sql) throws SQLException {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, utilisateurId);
            stmt.setInt(2, contactId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    private boolean isMissingColumn(SQLException e) {
        return e.getErrorCode() == 1054 || "42S22".equals(e.getSQLState());
    }
}
