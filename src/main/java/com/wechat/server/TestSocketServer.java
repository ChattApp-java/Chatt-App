package com.wechat.server;

import com.wechat.common.Protocol;
import com.wechat.common.Protocol.NetworkMessage;
import com.wechat.common.Protocol.MessageType;

import java.io.*;
import java.net.Socket;
import java.util.logging.Logger;

/**
 * Test du serveur socket.
 * Simule un client pour vérifier la connexion et l'échange de messages.
 *
 * Pour tester avec telnet (mode texte brut) :
 *   telnet localhost 5000
 *
 * Pour tester avec ce client de test :
 *   Lancer ServerMain d'abord, puis exécuter cette classe.
 */
public class TestSocketServer {

    private static final Logger LOGGER = Logger.getLogger(TestSocketServer.class.getName());

    public static void main(String[] args) {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  🧪 TEST CLIENT SOCKET - WeChat Server");
        System.out.println("═══════════════════════════════════════════════════\n");

        String host = Protocol.SERVER_HOST;
        int port = Protocol.SERVER_PORT;

        try (Socket socket = new Socket(host, port)) {
            System.out.println("✅ Connexion établie avec " + host + ":" + port);
            System.out.println("   Local : " + socket.getLocalAddress() + ":" + socket.getLocalPort());
            System.out.println("   Remote : " + socket.getInetAddress() + ":" + socket.getPort());

            // Initialiser les streams (output AVANT input !)
            ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream());
            output.flush();
            ObjectInputStream input = new ObjectInputStream(socket.getInputStream());

            System.out.println("\n📡 Streams initialisés");

            // Test 1 : Envoyer un PING
            System.out.println("\n─── Test 1 : PING ───");
            NetworkMessage ping = new NetworkMessage(MessageType.PING);
            output.writeObject(ping);
            output.flush();
            System.out.println("📤 Envoyé : " + ping.getType());

            NetworkMessage pong = (NetworkMessage) input.readObject();
            System.out.println("📥 Reçu : " + pong.getType() + (pong.isSuccess() ? " ✅" : " ❌"));

            // Test 2 : Tentative de login (échouera sans user en DB)
            System.out.println("\n─── Test 2 : LOGIN ───");
            NetworkMessage login = NetworkMessage.loginRequest("test_user", "test_pass");
            output.writeObject(login);
            output.flush();
            System.out.println("📤 Envoyé : LOGIN_REQUEST (test_user)");

            NetworkMessage loginResponse = (NetworkMessage) input.readObject();
            System.out.println("📥 Reçu : " + loginResponse.getType());
            System.out.println("   Succès : " + loginResponse.isSuccess());
            System.out.println("   Message : " + loginResponse.getContent());

            // Test 3 : Envoyer un message texte (échouera sans auth)
            System.out.println("\n─── Test 3 : MESSAGE (sans auth) ───");
            NetworkMessage textMsg = NetworkMessage.textMessage(null, 999L, "Hello Server!");
            output.writeObject(textMsg);
            output.flush();
            System.out.println("📤 Envoyé : TEXT_MESSAGE");

            try {
                NetworkMessage error = (NetworkMessage) input.readObject();
                System.out.println("📥 Reçu : " + error.getType() + " | " + error.getErrorMessage());
            } catch (Exception e) {
                System.out.println("   (Pas de réponse d'erreur attendue)");
            }

            // Test 4 : TYPING
            System.out.println("\n─── Test 4 : TYPING ───");
            NetworkMessage typing = NetworkMessage.typing(null, 999L);
            output.writeObject(typing);
            output.flush();
            System.out.println("📤 Envoyé : TYPING");

            // Attendre un peu
            Thread.sleep(500);

            System.out.println("\n═══════════════════════════════════════════════════");
            System.out.println("  ✅ TOUS LES TESTS SOCKET TERMINÉS");
            System.out.println("═══════════════════════════════════════════════════");

        } catch (java.net.ConnectException e) {
            System.err.println("\n❌ CONNEXION REFUSÉE");
            System.err.println("   Le serveur n'est probablement pas démarré sur " + host + ":" + port);
            System.err.println("   Lancez d'abord : com.wechat.server.ServerMain");
        } catch (Exception e) {
            System.err.println("\n❌ Erreur : " + e.getMessage());
            e.printStackTrace();
        }
    }
}