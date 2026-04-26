package serveur;

import commun.Protocole;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Relais vidéo TCP : reçoit d'un client et renvoie à l'autre.
 */
public class RelaisVideo implements Runnable {

    private volatile boolean running = true;
    private ServerSocket serverSocket;

    private Socket client1;
    private Socket client2;
    private InputStream in1, in2;
    private OutputStream out1, out2;
    private boolean firstConnected = false;

    @Override
    public void run() {
        try {
            serverSocket = new ServerSocket(Protocole.PORT_VIDEO);
            System.out.println("[VIDEO RELAY] En écoute sur TCP port " + Protocole.PORT_VIDEO);

            while (running) {
                Socket socket = serverSocket.accept();
                System.out.println("[VIDEO RELAY] Client connecté : " + socket.getInetAddress());

                if (!firstConnected) {
                    client1 = socket;
                    in1 = client1.getInputStream();
                    out1 = client1.getOutputStream();
                    firstConnected = true;
                    System.out.println("[VIDEO RELAY] Participant 1 enregistré");

                    new Thread(this::relayer1vers2).start();

                } else if (client2 == null) {
                    client2 = socket;
                    in2 = client2.getInputStream();
                    out2 = client2.getOutputStream();
                    System.out.println("[VIDEO RELAY] Participant 2 enregistré");

                    new Thread(this::relayer2vers1).start();
                }
            }

        } catch (IOException e) {
            if (running) {
                System.err.println("[VIDEO RELAY] Erreur : " + e.getMessage());
            }
        }
    }

    private void relayer1vers2() {
        byte[] buffer = new byte[65535];
        try {
            while (running && client2 == null) {
                Thread.sleep(100);
            }
            int len;
            while (running && (len = in1.read(buffer)) != -1) {
                out2.write(buffer, 0, len);
                out2.flush();
            }
        } catch (Exception e) {
            System.out.println("[VIDEO RELAY] Relais 1→2 terminé");
        }
    }

    private void relayer2vers1() {
        byte[] buffer = new byte[65535];
        try {
            int len;
            while (running && (len = in2.read(buffer)) != -1) {
                out1.write(buffer, 0, len);
                out1.flush();
            }
        } catch (Exception e) {
            System.out.println("[VIDEO RELAY] Relais 2→1 terminé");
        }
    }

    public void resetParticipants() {
        client1 = null; client2 = null;
        in1 = null; in2 = null;
        out1 = null; out2 = null;
        firstConnected = false;
        System.out.println("[VIDEO RELAY] Session réinitialisée.");
    }

    public void arreter() {
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) serverSocket.close();
        } catch (IOException ignored) {}
    }
}