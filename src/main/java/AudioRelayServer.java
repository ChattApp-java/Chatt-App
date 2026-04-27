import java.net.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AudioRelayServer.java
 * Relais UDP audio avec protocole BINAIRE.
 * Format paquet : [1 byte sessionIdLen][N bytes sessionId][M bytes audio PCM]
 * Avantages : pas de Base64 (~33% surcharge), pas de split string, parsing rapide.
 */
public class AudioRelayServer implements Runnable {

    private static final int BUFFER_SIZE = 4096;
    private static final int MAX_SESSION_ID_LEN = 64;

    private DatagramSocket udpSocket;
    private volatile boolean running = true;
    private final Map<String, SessionUDP> sessions = new ConcurrentHashMap<>();

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
            byte[] buffer = new byte[BUFFER_SIZE];

            while (running) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                udpSocket.receive(packet);

                byte[] data = packet.getData();
                int len = packet.getLength();
                InetAddress addr = packet.getAddress();
                int port = packet.getPort();

                if (len < 1) continue;

                // ── Format binaire : [sessionIdLen][sessionId][audioData]
                int sessionIdLen = data[0] & 0xFF;
                if (sessionIdLen > MAX_SESSION_ID_LEN || len < 1 + sessionIdLen) {
                    System.err.println("[AUDIO RELAY] Paquet malformé reçu (len=" + len + ", sidLen=" + sessionIdLen + ")");
                    continue;
                }

                String sessionId = new String(data, 1, sessionIdLen, java.nio.charset.StandardCharsets.UTF_8);
                int audioOffset = 1 + sessionIdLen;
                int audioLen = len - audioOffset;

                if (audioLen <= 0) {
                    // Pas de données audio = paquet d'enregistrement
                    handleRegister(sessionId, addr, port);
                    continue;
                }

                byte[] audioData = new byte[audioLen];
                System.arraycopy(data, audioOffset, audioData, 0, audioLen);
                handleRelay(sessionId, addr, port, audioData);
            }

        } catch (Exception e) {
            if (running) {
                System.err.println("[AUDIO RELAY] Erreur : " + e.getMessage());
            }
        } finally {
            if (udpSocket != null && !udpSocket.isClosed()) {
                udpSocket.close();
            }
        }
    }

    private void handleRegister(String sessionId, InetAddress addr, int port) {
        SessionUDP session = sessions.computeIfAbsent(sessionId, k -> null);

        if (session == null) {
            sessions.put(sessionId, new SessionUDP(addr, port));
            System.out.println("[AUDIO RELAY] Session [" + sessionId + "] — Participant 1 : "
                    + addr.getHostAddress() + ":" + port);
        } else if (!session.peer2Ready) {
            session.addr2 = addr;
            session.port2 = port;
            session.peer2Ready = true;
            System.out.println("[AUDIO RELAY] Session [" + sessionId + "] — Participant 2 : "
                    + addr.getHostAddress() + ":" + port);
        }
    }

    private void handleRelay(String sessionId, InetAddress senderAddr,
                             int senderPort, byte[] audioData) throws Exception {
        SessionUDP session = sessions.get(sessionId);
        if (session == null || !session.peer2Ready) return;

        InetAddress destAddr;
        int destPort;

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

    public void endSession(String sessionId) {
        sessions.remove(sessionId);
        System.out.println("[AUDIO RELAY] Session [" + sessionId + "] terminée.");
    }

    public static String makeSessionId(String user1, String user2) {
        return user1.compareTo(user2) < 0
                ? user1 + ":" + user2
                : user2 + ":" + user1;
    }

    public void stop() {
        running = false;
        if (udpSocket != null && !udpSocket.isClosed()) {
            udpSocket.close();
        }
    }
}

