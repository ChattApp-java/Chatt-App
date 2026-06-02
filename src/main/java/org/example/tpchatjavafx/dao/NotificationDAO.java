package org.example.tpchatjavafx.dao;

import org.example.tpchatjavafx.model.Notification;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class NotificationDAO {

    public Notification create(Notification notif) throws SQLException {
        String sql = "INSERT INTO notification (utilisateur_id, contenu, type, est_lue, date_creation) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setInt(1, notif.getUtilisateurId());
            pstmt.setString(2, notif.getContenu());
            pstmt.setString(3, notif.getType());
            pstmt.setBoolean(4, notif.isEstLue());
            pstmt.setTimestamp(5, notif.getDateCreation() != null ? new Timestamp(notif.getDateCreation().getTime()) : new Timestamp(System.currentTimeMillis()));

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating notification failed, no rows affected.");
            }

            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    notif.setId(generatedKeys.getInt(1));
                }
            }
            return notif;
        }
    }
}
