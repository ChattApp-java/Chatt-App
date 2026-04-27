package com.chatapp.audio;

import java.net.ServerSocket;
import java.net.Socket;

public class TestServeurAudio {
    public static void main(String[] args) throws Exception {

        System.out.println("Serveur en attente sur port 6000...");
        ServerSocket serveur = new ServerSocket(6000);
        Socket socket = serveur.accept();  // attend la connexion
        System.out.println("Client connecté ✓");

        // Reçoit l'audio et le joue
        RecepteurAudio recepteur = new RecepteurAudio();
        recepteur.demarrer(socket);

        System.out.println("Réception en cours... (appuie Entrée pour arrêter)");
        System.in.read();  // attend que tu appuies sur Entrée

        recepteur.arreter();
        socket.close();
        serveur.close();
    }
}

