package org.example.tpchatjavafx.dao;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ConnexionDAO {

    public void setEnLigne(int utilisateurId, String socketId, boolean estEnLigne) throws SQLException {
        Connection conn = DatabaseConnection.getInstance().getConnection();
        conn.setAutoCommit(false);
        try {
            if (estEnLigne) {
                String sqlInsert = "INSERT INTO connexion (utilisateur_id, socketId, estEnLigne) VALUES (?, ?, ?)";
                try (PreparedStatement stmt1 = conn.prepareStatement(sqlInsert)) {
                    stmt1.setInt(1, utilisateurId);
                    stmt1.setString(2, socketId);
                    stmt1.setBoolean(3, true);
                    stmt1.executeUpdate();
                }
            } else {
                String dateDeconnexionColumn = resolveDateDeconnexionColumn(conn);
                String sqlUpdate = "UPDATE connexion SET estEnLigne = ?, " + dateDeconnexionColumn + " = CURRENT_TIMESTAMP WHERE socketId = ?";
                try (PreparedStatement stmt1 = conn.prepareStatement(sqlUpdate)) {
                    stmt1.setBoolean(1, false);
                    stmt1.setString(2, socketId);
                    stmt1.executeUpdate();
                }
            }

            // Update user global status
            String sqlUser = "UPDATE utilisateur SET statut = ?, derniereConnexion = CURRENT_TIMESTAMP WHERE id = ?";
            try (PreparedStatement stmt2 = conn.prepareStatement(sqlUser)) {
                stmt2.setString(1, estEnLigne ? "EN_LIGNE" : "NON_CONNECTE");
                stmt2.setInt(2, utilisateurId);
                stmt2.executeUpdate();
            }

            conn.commit();
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
            conn.close();
        }
    }

    private String resolveDateDeconnexionColumn(Connection conn) throws SQLException {
        DatabaseMetaData metaData = conn.getMetaData();
        if (columnExists(metaData, "connexion", "dateDeconnexion")) {
            return "dateDeconnexion";
        }
        if (columnExists(metaData, "connexion", "date_deconnexion")) {
            return "date_deconnexion";
        }
        throw new SQLException("Colonne de date de deconnexion introuvable dans la table connexion.");
    }

    private boolean columnExists(DatabaseMetaData metaData, String tableName, String columnName) throws SQLException {
        try (ResultSet rs = metaData.getColumns(null, null, tableName, columnName)) {
            if (rs.next()) return true;
        }
        try (ResultSet rs = metaData.getColumns(null, null, tableName.toUpperCase(), columnName)) {
            if (rs.next()) return true;
        }
        try (ResultSet rs = metaData.getColumns(null, null, tableName, columnName.toUpperCase())) {
            return rs.next();
        }
    }
}
