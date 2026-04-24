package com.chatapp.server;

import com.chatapp.protocol.Protocol;

import java.net.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AudioRelayServer implements Runnable {

    private static final int BUFFER_SIZE = 4096;

    private DatagramSocket   udpSocket;
    private volatile boolean running = true;

   
    private final Map<String, SessionUDP> sessions = new ConcurrentHashMap<>();

    // Classe interne représentant une session audio entre 2 participants
    private static class SessionUDP {
        InetAddress addr1; int port1;
        InetAddress addr2; int port2;
        boolean peer1Ready = false;
        boolean peer2Ready = false;

        SessionUDP(InetAddress a1, int p1) {
            this.addr1 = a1;
            this.port1 = p1;
            this.peer1Ready = true;
        }
    }

    @Override
    public void run() {
        try {
            udpSocket = new DatagramSocket(Protocol.PORT_AUDIO);
            System.out.println("[AUDIO RELAY] En écoute sur UDP port " + Protocol.PORT_AUDIO);

            // Format attendu au début de chaque paquet : "SESSION_ID:username\n" + données audio
            // Ici on garde un format simple : les 32 premiers octets = sessionId ASCII padded
            byte[] buffer = new byte[BUFFER_SIZE];

            while (running) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                udpSocket.receive(packet);

                String data       = new String(packet.getData(), 0, packet.getLength());
                InetAddress addr  = packet.getAddress();
                int         port  = packet.getPort();

                // ── Format : "REGISTER|sessionId" pour s'enregistrer
                //             "DATA|sessionId|<audio_bytes>" pour envoyer
                if (data.startsWith("REGISTER|")) {
                    String sessionId = data.split("\\|")[1].trim();
                    handleRegister(sessionId, addr, port);
                } else if (data.startsWith("DATA|")) {
                    String[] parts    = data.split("\\|", 3);
                    String sessionId  = parts[1].trim();
                    byte[] audioData  = java.util.Base64.getDecoder().decode(parts[2]);
                    handleRelay(sessionId, addr, port, audioData);
                }
            }

        } catch (Exception e) {
            if (running) System.err.println("[AUDIO RELAY] Erreur : " + e.getMessage());
        } finally {
            if (udpSocket != null && !udpSocket.isClosed()) udpSocket.close();
        }
    }

    // Enregistrer un participant dans une session
    private void handleRegister(String sessionId, InetAddress addr, int port) {
        SessionUDP session = sessions.get(sessionId);

        if (session == null) {
            // Premier participant
            sessions.put(sessionId, new SessionUDP(addr, port));
            System.out.println("[AUDIO RELAY] Session [" + sessionId + "] — Participant 1 : "
                    + addr.getHostAddress() + ":" + port);
        } else if (!session.peer2Ready) {
            // Deuxième participant
            session.addr2 = addr;
            session.port2 = port;
            session.peer2Ready = true;
            System.out.println("[AUDIO RELAY] Session [" + sessionId + "] — Participant 2 : "
                    + addr.getHostAddress() + ":" + port);
        }
    }

    // Relayer les données audio vers l'autre participant
    private void handleRelay(String sessionId, InetAddress senderAddr,
                              int senderPort, byte[] audioData) throws Exception {
        SessionUDP session = sessions.get(sessionId);
        if (session == null || !session.peer2Ready) return;

        InetAddress destAddr;
        int         destPort;

        if (senderAddr.equals(session.addr1) && senderPort == session.port1) {
            destAddr = session.addr2;
            destPort = session.port2;
        } else {
            destAddr = session.addr1;
            destPort = session.port1;
        }

        DatagramPacket relay = new DatagramPacket(audioData, audioData.length, destAddr, destPort);
        udpSocket.send(relay);
    }

    // ✅ Terminer une session spécifique (appeler depuis ClientHandler à CALL_END)
    public void endSession(String sessionId) {
        sessions.remove(sessionId);
        System.out.println("[AUDIO RELAY] Session [" + sessionId + "] terminée.");
    }

    // Crée un sessionId canonique depuis deux usernames (ordre alphabétique)
    public static String makeSessionId(String user1, String user2) {
        return user1.compareTo(user2) < 0
                ? user1 + ":" + user2
                : user2 + ":" + user1;
    }

    public void stop() {
        running = false;
        if (udpSocket != null) udpSocket.close();
    }
}

