package com.chatapp.audio;

import javax.sound.sampled.*;
import java.net.*;
import java.util.Base64;
import com.chatapp.protocol.Protocol;

public class GestionnaireAudioUDP {
    private static final int CHUNK_SIZE = 1024;
    private static final AudioFormat FORMAT = CapteurAudio.getFormat();
    private static final int FRAME_SIZE = CapteurAudio.FRAME_SIZE;

    private DatagramSocket socket;
    private TargetDataLine microphone;
    private SourceDataLine hautParleur;
    private InetAddress serverAddr;
    private final int serverPort = Protocol.PORT_AUDIO;
    private String sessionId;
    private volatile boolean running = false;
    private Thread sendThread, recvThread;

    public void demarrer(String serverHost, String sessionId) throws Exception {
        this.serverAddr = InetAddress.getByName(serverHost);
        this.sessionId = sessionId;
        this.socket = new DatagramSocket();

        // Enregistrer la session aupres du relais
        byte[] reg = ("REGISTER|" + sessionId).getBytes(java.nio.charset.StandardCharsets.UTF_8);
        socket.send(new DatagramPacket(reg, reg.length, serverAddr, serverPort));

        // Microphone
        DataLine.Info micInfo = new DataLine.Info(TargetDataLine.class, FORMAT);
        microphone = (TargetDataLine) AudioSystem.getLine(micInfo);
        microphone.open(FORMAT);
        microphone.start();

        // Haut-parleur
        DataLine.Info spkInfo = new DataLine.Info(SourceDataLine.class, FORMAT);
        hautParleur = (SourceDataLine) AudioSystem.getLine(spkInfo);
        hautParleur.open(FORMAT);
        hautParleur.start();

        running = true;

        sendThread = new Thread(() -> {
            byte[] buffer = new byte[CHUNK_SIZE];
            while (running) {
                try {
                    int len = microphone.read(buffer, 0, buffer.length);
                    if (len > 0) {
                        // Ne coder que les octets reels lus
                        byte[] actual = java.util.Arrays.copyOf(buffer, len);
                        String b64 = Base64.getEncoder().encodeToString(actual);
                        String payload = "DATA|" + sessionId + "|" + b64;
                        byte[] pkt = payload.getBytes(java.nio.charset.StandardCharsets.UTF_8);
                        socket.send(new DatagramPacket(pkt, pkt.length, serverAddr, serverPort));
                    }
                } catch (Exception e) {
                    if (running) e.printStackTrace();
                }
            }
        }, "Audio-Send");

        recvThread = new Thread(() -> {
            byte[] buf = new byte[8192];
            while (running) {
                try {
                    DatagramPacket p = new DatagramPacket(buf, buf.length);
                    socket.receive(p);

                    // IMPORTANT : tronquer a un multiple entier de frameSize
                    int len = p.getLength();
                    int validLen = len - (len % Math.max(FRAME_SIZE, 1));

                    if (validLen > 0) {
                        hautParleur.write(p.getData(), p.getOffset(), validLen);
                    }
                } catch (Exception e) {
                    if (running) e.printStackTrace();
                }
            }
        }, "Audio-Recv");

        sendThread.setDaemon(true);
        recvThread.setDaemon(true);
        sendThread.start();
        recvThread.start();
        System.out.println("[AUDIO UDP] Demarre pour session: " + sessionId);
    }

    public void arreter() {
        running = false;
        if (microphone != null) {
            microphone.stop();
            microphone.close();
        }
        if (hautParleur != null) {
            hautParleur.drain();
            hautParleur.stop();
            hautParleur.close();
        }
        if (socket != null && !socket.isClosed()) socket.close();
        System.out.println("[AUDIO UDP] Arrete.");
    }
}

