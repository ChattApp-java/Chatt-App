package com.chatapp.server;


public class ServerMain {

    public static void main(String[] args) {

        System.out.println("Démarrage des services...\n");

        // 1. Relais audio UDP
        AudioRelayServer audioRelay = new AudioRelayServer();
        Thread audioThread = new Thread(audioRelay);
        audioThread.setDaemon(true);
        audioThread.setName("AudioRelay-UDP-5001");
        audioThread.start();

        // 2. Relais vidéo UDP
        VideoRelayServer videoRelay = new VideoRelayServer();
        Thread videoThread = new Thread(videoRelay);
        videoThread.setDaemon(true);
        videoThread.setName("VideoRelay-UDP-5002");
        videoThread.start();

        // 3. Serveur TCP principal (bloquant — lancé en dernier)
        new Server().start();
    }
}

