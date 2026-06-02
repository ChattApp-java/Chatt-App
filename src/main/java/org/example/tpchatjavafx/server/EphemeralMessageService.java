package org.example.tpchatjavafx.server;

import org.example.tpchatjavafx.dao.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class EphemeralMessageService {

    private boolean running = false;
    private Thread workerThread;

    public void start() {
        if (running) return;
        running = true;
        workerThread = new Thread(() -> {
            while (running) {
                try {
                    deleteExpiredMessages();

                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    System.out.println("EphemeralMessageService interrupted.");
                } catch (Exception e) {
                    System.err.println("Error in EphemeralMessageService: " + e.getMessage());
                }
            }
        });
        workerThread.setDaemon(true);
        workerThread.start();
        System.out.println("EphemeralMessageService started.");
    }

    public void stop() {
        running = false;
        if (workerThread != null) {
            workerThread.interrupt();
        }
        System.out.println("EphemeralMessageService stopped.");
    }

    private void deleteExpiredMessages() {

        String sql = "DELETE m FROM message m " +
                     "JOIN groupe g ON m.groupe_id = g.id " +
                     "WHERE g.ephemeral_timer > 0 " +
                     "AND m.date_envoi < DATE_SUB(NOW(), INTERVAL g.ephemeral_timer SECOND)";

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            int deletedCount = stmt.executeUpdate();
            if (deletedCount > 0) {
                System.out.println("[Ephemeral] Deleted " + deletedCount + " expired messages.");

            }
        } catch (SQLException e) {
            System.err.println("[Ephemeral] Database error during deletion: " + e.getMessage());
        }
    }
}
