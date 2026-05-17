package com.wechat.service;

import com.wechat.common.Protocol;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import com.wechat.server.ChatServer;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.TimeUnit;

/**
 * Tests JUnit du serveur socket.
 * Démarre un serveur, teste la connexion, puis l'arrête.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ServerConnectionTest {

    private static ChatServer server;
    private static Thread serverThread;

    @BeforeAll
    static void startServer() throws InterruptedException {
        server = new ChatServer();
        serverThread = new Thread(server::start);
        serverThread.setDaemon(true);
        serverThread.start();

        // Attendre que le serveur démarre
        Thread.sleep(1000);
        System.out.println("🚀 Serveur de test démarré");
    }

    @Test
    @Order(1)
    @DisplayName("Le serveur est en cours d'exécution")
    void testServerRunning() {
        assertTrue(server.isRunning(), "Le serveur doit être en cours d'exécution");
        System.out.println("✅ Serveur running = true");
    }

    @Test
    @Order(2)
    @DisplayName("Connexion TCP acceptée")
    void testConnectionAccepted() {
        assertDoesNotThrow(() -> {
            try (Socket socket = new Socket(Protocol.SERVER_HOST, Protocol.SERVER_PORT)) {
                assertTrue(socket.isConnected(), "Le socket doit être connecté");
                assertFalse(socket.isClosed(), "Le socket ne doit pas être fermé");
                System.out.println("✅ Connexion acceptée sur port " + Protocol.SERVER_PORT);
            }
        });
    }

    @Test
    @Order(3)
    @DisplayName("Connexion multiple acceptée")
    void testMultipleConnections() {
        assertDoesNotThrow(() -> {
            Socket socket1 = new Socket(Protocol.SERVER_HOST, Protocol.SERVER_PORT);
            Socket socket2 = new Socket(Protocol.SERVER_HOST, Protocol.SERVER_PORT);
            Socket socket3 = new Socket(Protocol.SERVER_HOST, Protocol.SERVER_PORT);

            assertTrue(socket1.isConnected());
            assertTrue(socket2.isConnected());
            assertTrue(socket3.isConnected());

            socket1.close();
            socket2.close();
            socket3.close();

            System.out.println("✅ 3 connexions simultanées acceptées");
        });
    }

    @Test
    @Order(4)
    @DisplayName("Aucun client connecté au démarrage")
    void testNoClientsAtStart() {
        assertEquals(0, server.getConnectedCount(), "Aucun client ne doit être connecté au démarrage");
        System.out.println("✅ Clients connectés au démarrage : " + server.getConnectedCount());
    }

    @AfterAll
    static void stopServer() {
        if (server != null) {
            server.stop();
            System.out.println("🛑 Serveur de test arrêté");
        }
    }
}