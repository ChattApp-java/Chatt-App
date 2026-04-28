package org.example.tpchatjavafx.dao;

import org.example.tpchatjavafx.model.Appel;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AppelDAO {

    public Appel create(Appel appel) throws SQLException {
        String sql = "INSERT INTO appel (type_appel, date_heure, duree, statut, expediteur_id, destinataire_id) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            pstmt.setString(1, appel.getTypeAppel());
            pstmt.setTimestamp(2, appel.getDateHeure() != null ? new Timestamp(appel.getDateHeure().getTime()) : new Timestamp(System.currentTimeMillis()));
            pstmt.setInt(3, appel.getDuree());
            pstmt.setString(4, appel.getStatut());
            pstmt.setInt(5, appel.getExpediteurId());
            pstmt.setInt(6, appel.getDestinataireId());
            
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
}
