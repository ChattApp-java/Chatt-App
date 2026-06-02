package org.example.tpchatjavafx.server;

import java.io.Closeable;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.Inet4Address;
import java.util.Enumeration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class UDPRelayServer implements Closeable {
    public static final int DEFAULT_AUDIO_PORT = 6000;
    public static final int DEFAULT_VIDEO_PORT = 6001;

    private final int audioPort;
    private final int videoPort;
    private final String host;
    private final Map<Integer, Map<Integer, ParticipantEndpoint>> meetings = new ConcurrentHashMap<>();
    private volatile boolean running;
    private DatagramSocket audioSocket;
    private DatagramSocket videoSocket;
    private Thread audioThread;
    private Thread videoThread;

    public UDPRelayServer() {
        this(DEFAULT_AUDIO_PORT, DEFAULT_VIDEO_PORT, detectAdvertisedHost());
    }

    public UDPRelayServer(int audioPort, int videoPort, String host) {
        this.audioPort = audioPort;
        this.videoPort = videoPort;
        this.host = host;
    }

    public synchronized void start() throws SocketException {
        if (running) return;
        audioSocket = new DatagramSocket(audioPort);
        videoSocket = new DatagramSocket(videoPort);
        running = true;
        audioThread = new Thread(() -> relayLoop(audioSocket, MediaKind.AUDIO), "udp-audio-relay");
        videoThread = new Thread(() -> relayLoop(videoSocket, MediaKind.VIDEO), "udp-video-relay");
        audioThread.setDaemon(true);
        videoThread.setDaemon(true);
        audioThread.start();
        videoThread.start();
    }

    public void registerParticipant(int meetingId, int userId, InetAddress address, int audioClientPort, int videoClientPort) {
        meetings.computeIfAbsent(meetingId, id -> new ConcurrentHashMap<>())
                .put(userId, new ParticipantEndpoint(userId, address, audioClientPort, videoClientPort));
    }

    public void unregisterParticipant(int meetingId, int userId) {
        Map<Integer, ParticipantEndpoint> participants = meetings.get(meetingId);
        if (participants == null) return;
        participants.remove(userId);
        if (participants.isEmpty()) meetings.remove(meetingId);
    }

    public void unregisterMeeting(int meetingId) {
        meetings.remove(meetingId);
    }

    public int getAudioPort() { return audioPort; }
    public int getVideoPort() { return videoPort; }
    public String getHost() { return host; }

    private static String detectAdvertisedHost() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces != null && interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                if (!networkInterface.isUp() || networkInterface.isLoopback() || networkInterface.isVirtual()) {
                    continue;
                }
                Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress address = addresses.nextElement();
                    if (address instanceof Inet4Address && !address.isLoopbackAddress()) {
                        return address.getHostAddress();
                    }
                }
            }
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return "127.0.0.1";
        }
    }

    private void relayLoop(DatagramSocket socket, MediaKind kind) {
        byte[] buffer = new byte[65507];
        while (running) {
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            try {
                socket.receive(packet);
                RelayHeader header = RelayHeader.from(packet.getData(), packet.getLength());
                if (header == null) continue;
                Map<Integer, ParticipantEndpoint> participants = meetings.get(header.meetingId);
                if (participants == null) continue;
                ParticipantEndpoint sender = participants.get(header.userId);
                if (sender != null) sender.updateAddress(packet.getAddress(), packet.getPort(), kind);
                for (ParticipantEndpoint participant : participants.values()) {
                    if (participant.userId == header.userId) continue;
                    InetSocketAddress target = participant.socketAddress(kind);
                    if (target == null || target.getPort() <= 0) continue;
                    DatagramPacket outbound = new DatagramPacket(packet.getData(), packet.getLength(), target);
                    socket.send(outbound);
                }
            } catch (SocketException e) {
                if (running) System.err.println("Erreur UDP relay: " + e.getMessage());
            } catch (IOException e) {
                System.err.println("Erreur UDP relay: " + e.getMessage());
            }
        }
    }

    @Override
    public synchronized void close() {
        running = false;
        if (audioSocket != null) audioSocket.close();
        if (videoSocket != null) videoSocket.close();
        meetings.clear();
    }

    private enum MediaKind { AUDIO, VIDEO }

    private static class ParticipantEndpoint {
        private final int userId;
        private volatile InetAddress address;
        private volatile int audioPort;
        private volatile int videoPort;

        ParticipantEndpoint(int userId, InetAddress address, int audioPort, int videoPort) {
            this.userId = userId;
            this.address = address;
            this.audioPort = audioPort;
            this.videoPort = videoPort;
        }

        void updateAddress(InetAddress address, int observedPort, MediaKind kind) {
            this.address = address;
            if (observedPort <= 0) {
                return;
            }
            if (kind == MediaKind.AUDIO) {
                audioPort = observedPort;
            }
            if (kind == MediaKind.VIDEO) {
                videoPort = observedPort;
            }
        }

        InetSocketAddress socketAddress(MediaKind kind) {
            int port = kind == MediaKind.AUDIO ? audioPort : videoPort;
            return address == null || port <= 0 ? null : new InetSocketAddress(address, port);
        }
    }

    private record RelayHeader(int meetingId, int userId) {
        static RelayHeader from(byte[] data, int length) {
            if (length < 8) return null;
            int meetingId = readInt(data, 0);
            int userId = readInt(data, 4);
            if (meetingId <= 0 || userId <= 0) return null;
            return new RelayHeader(meetingId, userId);
        }

        private static int readInt(byte[] data, int offset) {
            return ((data[offset] & 0xFF) << 24)
                    | ((data[offset + 1] & 0xFF) << 16)
                    | ((data[offset + 2] & 0xFF) << 8)
                    | (data[offset + 3] & 0xFF);
        }
    }
}
