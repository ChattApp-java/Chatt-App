package com.chatapp.audio;

import java.net.Socket;

public class TestClientAudio {
    public static void main(String[] args) throws Exception {

        System.out.println("Connexion au serveur...");
        Socket socket = new Socket("localhost", 6000);  // même PC
        System.out.println("Connecté ✓");

        // Capture le micro et l'envoie
        EmetteurAudio emetteur = new EmetteurAudio();
        emetteur.demarrer(socket);

        System.out.println("Envoi en cours... (appuie Entrée pour arrêter)");
        System.in.read();

        emetteur.arreter();
        socket.close();
    }
}

