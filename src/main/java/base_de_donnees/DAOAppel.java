package base_de_donnees;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DAOAppel {

    public void enregistrerAppel(int idEmetteur, int idRecepteur, String type, int dureeSec) {
        String sql = "INSERT INTO appels_historique (id_emetteur, id_recepteur, type_appel, duree_sec) VALUES (?, ?, ?, ?)";
        Connection conn = ConnexionBD.getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idEmetteur);
            stmt.setInt(2, idRecepteur);
            stmt.setString(3, type);
            stmt.setInt(4, dureeSec);
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[DB] Erreur enregistrerAppel : " + e.getMessage());
        }
    }

    public List<String[]> getHistorique(int idUser) {
        List<String[]> liste = new ArrayList<>();
        String sql = "SELECT a.*, u1.username as emetteur, u2.username as recepteur " +
                     "FROM appels_historique a " +
                     "JOIN users u1 ON a.id_emetteur = u1.id_user " +
                     "JOIN users u2 ON a.id_recepteur = u2.id_user " +
                     "WHERE a.id_emetteur = ? OR a.id_recepteur = ? " +
                     "ORDER BY a.date_debut DESC";
        
        Connection conn = ConnexionBD.getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idUser);
            stmt.setInt(2, idUser);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                String[] item = new String[5];
                item[0] = rs.getString("emetteur");
                item[1] = rs.getString("recepteur");
                item[2] = rs.getString("type_appel");
                item[3] = rs.getString("date_debut");
                item[4] = String.valueOf(rs.getInt("duree_sec"));
                liste.add(item);
            }
        } catch (SQLException e) {
            System.err.println("[DB] Erreur getHistorique appels : " + e.getMessage());
        }
        return liste;
    }
}
