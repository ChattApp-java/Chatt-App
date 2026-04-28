package org.example.tpchatjavafx.dao;

import org.example.tpchatjavafx.model.FichierMedia;
import org.example.tpchatjavafx.model.Video;
import org.example.tpchatjavafx.model.Vocal;

import java.sql.*;

public class FichierMediaDAO {

    public FichierMedia create(FichierMedia f) throws SQLException {
        String sql = "INSERT INTO fichier_media (message_id, nom_fichier, chemin_acces, taille, type) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            pstmt.setInt(1, f.getMessageId());
            pstmt.setString(2, f.getNomFichier());
            pstmt.setString(3, f.getCheminAcces());
            pstmt.setLong(4, f.getTaille());
            pstmt.setString(5, f.getType());
            
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating fichier_media failed, no rows affected.");
            }

            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    f.setId(generatedKeys.getInt(1));
                } else {
                    throw new SQLException("Creating fichier_media failed, no ID obtained.");
                }
            }
            
            // Gestion de l'héritage
            if (f instanceof Vocal vocal) {
                String sqlVocal = "INSERT INTO vocal (id, duree) VALUES (?, ?)";
                try (PreparedStatement pstVocal = conn.prepareStatement(sqlVocal)) {
                    pstVocal.setInt(1, vocal.getId());
                    pstVocal.setInt(2, vocal.getDuree());
                    pstVocal.executeUpdate();
                }
            } else if (f instanceof Video video) {
                String sqlVideo = "INSERT INTO video (id, resolution, duree) VALUES (?, ?, ?)";
                try (PreparedStatement pstVideo = conn.prepareStatement(sqlVideo)) {
                    pstVideo.setInt(1, video.getId());
                    pstVideo.setString(2, video.getResolution());
                    pstVideo.setInt(3, video.getDuree());
                    pstVideo.executeUpdate();
                }
            }
            
            return f;
        }
    }

    public FichierMedia findByMessageId(int messageId) throws SQLException {
        String sql = "SELECT * FROM fichier_media WHERE message_id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, messageId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    FichierMedia f = new FichierMedia();
                    f.setId(rs.getInt("id"));
                    f.setMessageId(rs.getInt("message_id"));
                    f.setNomFichier(rs.getString("nom_fichier"));
                    f.setCheminAcces(rs.getString("chemin_acces"));
                    f.setTaille(rs.getLong("taille"));
                    f.setType(rs.getString("type"));
                    return f;
                }
            }
        }
        return null;
    }
}
