package com.example.database;

import com.example.model.Message;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MessageDAO {

    public MessageDAO() {
    }

    public int sauvegarderMessage(Message msg) {
        String sql = "INSERT INTO messages (id_sender, id_receiver, contenu, statut) VALUES (?, ?, ?, 'non_lu')";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, msg.getId_sender());
            stmt.setInt(2, msg.getId_receiver());
            stmt.setString(3, msg.getContenu());

            stmt.executeUpdate();

            ResultSet keys = stmt.getGeneratedKeys();
            if (keys.next()) {
                int id = keys.getInt(1);
                System.out.println("[DB] Message sauvegardé, id=" + id);
                return id;
            }

        } catch (SQLException e) {
            System.err.println("[DB] Erreur sauvegarde message : " + e.getMessage());
        }
        return -1;
    }

    public List<Message> getHistorique(int idUser1, int idUser2) {
        List<Message> liste = new ArrayList<>();
        String sql = "SELECT * FROM messages "
                   + "WHERE (id_sender = ? AND id_receiver = ?) "
                   + "   OR (id_sender = ? AND id_receiver = ?) "
                   + "ORDER BY date_envoi ASC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, idUser1);
            stmt.setInt(2, idUser2);
            stmt.setInt(3, idUser2);
            stmt.setInt(4, idUser1);

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                liste.add(extraireMessage(rs));
            }
            System.out.println("[DB] Historique récupéré : " + liste.size() + " messages.");

        } catch (SQLException e) {
            System.err.println("[DB] Erreur historique : " + e.getMessage());
        }
        return liste;
    }

    public List<Message> getMessagesNonLus(int idReceiver) {
        List<Message> liste = new ArrayList<>();
        String sql = "SELECT * FROM messages "
                   + "WHERE id_receiver = ? AND statut = 'non_lu' "
                   + "ORDER BY date_envoi ASC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, idReceiver);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                liste.add(extraireMessage(rs));
            }
            System.out.println("[DB] Messages non lus pour user " + idReceiver + " : " + liste.size());

        } catch (SQLException e) {
            System.err.println("[DB] Erreur non lus : " + e.getMessage());
        }
        return liste;
    }

    public void marquerCommeLu(int idMessage) {
        String sql = "UPDATE messages SET statut = 'lu' WHERE id_message = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, idMessage);
            stmt.executeUpdate();

        } catch (SQLException e) {
            System.err.println("[DB] Erreur marquerCommeLu : " + e.getMessage());
        }
    }

    public void marquerTousLus(int idSender, int idReceiver) {
        String sql = "UPDATE messages SET statut = 'lu' "
                   + "WHERE id_sender = ? AND id_receiver = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, idSender);
            stmt.setInt(2, idReceiver);
            int rows = stmt.executeUpdate();
            System.out.println("[DB] " + rows + " messages marqués comme lus.");

        } catch (SQLException e) {
            System.err.println("[DB] Erreur marquerTousLus : " + e.getMessage());
        }
    }

    public boolean supprimerMessage(int idMessage) {
        String sql = "DELETE FROM messages WHERE id_message = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, idMessage);
            int rows = stmt.executeUpdate();
            return rows > 0;

        } catch (SQLException e) {
            System.err.println("[DB] Erreur suppression : " + e.getMessage());
            return false;
        }
    }

    private Message extraireMessage(ResultSet rs) throws SQLException {
        return new Message(
                rs.getInt("id_message"),
                rs.getInt("id_sender"),
                rs.getInt("id_receiver"),
                rs.getString("contenu"),
                rs.getTimestamp("date_envoi").toLocalDateTime(),
                rs.getString("statut")
        );
    }
}