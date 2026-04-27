import java.net.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
/**
 * VideoRelayServer.java
 * Relais UDP vidéo avec routage par session.
 * Supporte plusieurs appels vidéo simultanés via ConcurrentHashMap.
 */
public class VideoRelayServer implements Runnable {

    private static final int BUFFER_SIZE = 65535;
    private static final int MAX_SESSION_ID_LEN = 64;

    private DatagramSocket udpSocket;
    private volatile boolean running = true;
    private final Map<String, SessionVideo> sessions = new ConcurrentHashMap<>();

    private static class SessionVideo {
        InetAddress addr1; int port1;
        InetAddress addr2; int port2;
        boolean peer1Ready = false;
        boolean peer2Ready = false;

        SessionVideo(InetAddress a1, int p1) {
            this.addr1 = a1;
            this.port1 = p1;
            this.peer1Ready = true;
        }
    }

    @Override
    public void run() {
        try {
            udpSocket = new DatagramSocket(Protocol.PORT_VIDEO);
            System.out.println("[VIDEO RELAY] En écoute sur UDP port " + Protocol.PORT_VIDEO);
            byte[] buffer = new byte[BUFFER_SIZE];

            while (running) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                udpSocket.receive(packet);

                byte[] data = packet.getData();
                int len = packet.getLength();
                InetAddress senderAddr = packet.getAddress();
                int senderPort = packet.getPort();

                if (len < 1) continue;

                // Format binaire : [sessionIdLen][sessionId][videoData]
                int sessionIdLen = data[0] & 0xFF;
                if (sessionIdLen > MAX_SESSION_ID_LEN || len < 1 + sessionIdLen) {
                    System.err.println("[VIDEO RELAY] Paquet malformé (len=" + len + ", sidLen=" + sessionIdLen + ")");
                    continue;
                }

                String sessionId = new String(data, 1, sessionIdLen, java.nio.charset.StandardCharsets.UTF_8);
                int videoOffset = 1 + sessionIdLen;
                int videoLen = len - videoOffset;

                SessionVideo session = sessions.get(sessionId);
                if (session == null) {
                    sessions.put(sessionId, new SessionVideo(senderAddr, senderPort));
                    System.out.println("[VIDEO RELAY] Session [" + sessionId + "] — Participant 1 : "
                            + senderAddr.getHostAddress() + ":" + senderPort);
                    continue;
                }

                if (!session.peer2Ready && (!senderAddr.equals(session.addr1) || senderPort != session.port1)) {
                    session.addr2 = senderAddr;
                    session.port2 = senderPort;
                    session.peer2Ready = true;
                    System.out.println("[VIDEO RELAY] Session [" + sessionId + "] — Participant 2 : "
                            + senderAddr.getHostAddress() + ":" + senderPort);
                    continue;
                }

                if (videoLen > 0 && session.peer2Ready) {
                    InetAddress destAddr;
                    int destPort;
                    if (senderAddr.equals(session.addr1) && senderPort == session.port1) {
                        destAddr = session.addr2;
                        destPort = session.port2;
                    } else {
                        destAddr = session.addr1;
                        destPort = session.port1;
                    }

                    byte[] videoData = new byte[videoLen];
                    System.arraycopy(data, videoOffset, videoData, 0, videoLen);
                    DatagramPacket relay = new DatagramPacket(videoData, videoData.length, destAddr, destPort);
                    udpSocket.send(relay);
                }
            }

        } catch (Exception e) {
            if (running) {
                System.err.println("[VIDEO RELAY] Erreur : " + e.getMessage());
            }
        } finally {
            if (udpSocket != null && !udpSocket.isClosed()) {
                udpSocket.close();
            }
        }
    }

    public void endSession(String sessionId) {
        sessions.remove(sessionId);
        System.out.println("[VIDEO RELAY] Session [" + sessionId + "] terminée.");
    }

    public void stop() {
        running = false;
        if (udpSocket != null && !udpSocket.isClosed()) {
            udpSocket.close();
        }
    }
}

