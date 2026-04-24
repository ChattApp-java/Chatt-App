package com.chatapp.server;

import com.chatapp.protocol.Protocol;

import java.net.*;


public class VideoRelayServer implements Runnable {

    private static final int BUFFER_SIZE = 65535;

    private DatagramSocket   udpSocket;
    private volatile boolean running = true;

    private InetAddress addr1;
    private int         port1;
    private InetAddress addr2;
    private int         port2;
    private boolean     firstRegistered = false;


    @Override
    public void run() {
        try {
            udpSocket = new DatagramSocket(Protocol.PORT_VIDEO);
            System.out.println("[VIDEO RELAY] En écoute sur UDP port "
                    + Protocol.PORT_VIDEO);

            byte[] buffer = new byte[BUFFER_SIZE];

            while (running) {
                DatagramPacket packet =
                        new DatagramPacket(buffer, buffer.length);
                udpSocket.receive(packet);

                InetAddress senderAddr = packet.getAddress();
                int         senderPort = packet.getPort();

                // Enregistrement participant 1
                if (!firstRegistered) {
                    addr1 = senderAddr;
                    port1 = senderPort;
                    firstRegistered = true;
                    System.out.println("[VIDEO RELAY] Participant 1 : "
                            + addr1.getHostAddress() + ":" + port1);
                    continue;
                }

                // Enregistrement participant 2
                if (addr2 == null
                        && (!senderAddr.equals(addr1) || senderPort != port1)) {
                    addr2 = senderAddr;
                    port2 = senderPort;
                    System.out.println("[VIDEO RELAY] Participant 2 : "
                            + addr2.getHostAddress() + ":" + port2);
                }

                // Relais : envoyer à l'autre participant
                if (addr2 != null) {
                    InetAddress destAddr;
                    int         destPort;

                    if (senderAddr.equals(addr1) && senderPort == port1) {
                        destAddr = addr2;
                        destPort = port2;
                    } else {
                        destAddr = addr1;
                        destPort = port1;
                    }

                    DatagramPacket relay = new DatagramPacket(
                            packet.getData(),
                            packet.getLength(),
                            destAddr, destPort);
                    udpSocket.send(relay);
                }
            }

        } catch (Exception e) {
            if (running) {
                System.err.println("[VIDEO RELAY] Erreur : "
                        + e.getMessage());
            }
        } finally {
            if (udpSocket != null && !udpSocket.isClosed()) {
                udpSocket.close();
            }
        }
    }


    public void resetParticipants() {
        addr1 = null; port1 = 0;
        addr2 = null; port2 = 0;
        firstRegistered = false;
        System.out.println("[VIDEO RELAY] Session réinitialisée.");
    }

    public void stop() {
        running = false;
        if (udpSocket != null) udpSocket.close();
    }
}

