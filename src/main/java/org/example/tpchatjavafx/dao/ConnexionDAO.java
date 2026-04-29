package org.example.tpchatjavafx.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
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
                String sqlUpdate = "UPDATE connexion SET estEnLigne = ?, dateDeconnexion = CURRENT_TIMESTAMP WHERE socketId = ?";
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
}
