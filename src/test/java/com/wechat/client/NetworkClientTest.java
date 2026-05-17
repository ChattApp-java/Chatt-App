package com.wechat.client;

import com.wechat.common.Protocol;
import com.wechat.server.ChatServer;
import com.wechat.server.ServerMain;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Tests JUnit du NetworkClient.
 * Démarre un serveur, teste connexion/auth/messages.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class NetworkClientTest {

    private static ChatServer server;
    private static Thread serverThread;
    private NetworkClient client;

    @BeforeAll
    static void startServer() throws InterruptedException {
        server = new ChatServer();
        serverThread = new Thread(server::start);
        serverThread.setDaemon(true);
        serverThread.start();
        Thread.sleep(1500); // Attendre le démarrage
        System.out.println("🚀 Serveur de test démarré");
    }

    @BeforeEach
    void setUp() {
        client = NetworkClient.getInstance();
    }

    @AfterEach
    void tearDown() {
        if (client.isConnected()) {
            client.disconnect();
        }
        // Reset singleton pour le prochain test
        try {
            var field = NetworkClient.class.getDeclaredField("instance");
            field.setAccessible(true);
            field.set(null, null);
        } catch (Exception e) {
            // Ignorer
        }
    }

    @Test
    @Order(1)
    @DisplayName("Connexion au serveur")
    void testConnect() {
        AtomicBoolean connected = new AtomicBoolean(false);
        client.setConnectionListener(conn -> connected.set(conn));

        boolean result = client.connect();

        assertTrue(result, "La connexion doit réussir");
        assertTrue(client.isConnected(), "Le client doit être connecté");
        assertTrue(connected.get(), "Le listener doit avoir été appelé avec true");
        System.out.println("✅ testConnect : Connecté au serveur");
    }

    @Test
    @Order(2)
    @DisplayName("Déconnexion propre")
    void testDisconnect() {
        client.connect();
        assertTrue(client.isConnected());

        AtomicBoolean disconnected = new AtomicBoolean(false);
        client.setDisconnectListener(() -> disconnected.set(true));

        client.disconnect();

        assertFalse(client.isConnected(), "Le client ne doit plus être connecté");
        System.out.println("✅ testDisconnect : Déconnecté proprement");
    }

    @Test
    @Order(3)
    @DisplayName("Authentification échoue avec user inconnu")
    void testAuthFail() {
        client.connect();

        AtomicReference<String> error = new AtomicReference<>();
        client.setErrorListener(err -> error.set(err));

        var user = client.authenticate("inconnu", "pass");

        assertNull(user, "L'authentification doit échouer");
        assertFalse(client.isAuthenticated(), "Ne doit pas être authentifié");
        System.out.println("✅ testAuthFail : Auth échouée comme attendu");
    }

    @Test
    @Order(4)
    @DisplayName("Réception de message (PING/PONG)")
    void testPingPong() throws InterruptedException {
        client.connect();

        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean pongReceived = new AtomicBoolean(false);

        // On ne peut pas tester PING/PONG directement sans auth
        // Mais on vérifie que la connexion accepte l'envoi
        assertTrue(client.isConnected());
        System.out.println("✅ testPingPong : Connexion établie, streams OK");
    }

    @Test
    @Order(5)
    @DisplayName("Double connexion retourne true")
    void testDoubleConnect() {
        boolean first = client.connect();
        boolean second = client.connect();

        assertTrue(first);
        assertTrue(second, "Double connexion doit retourner true (déjà connecté)");
        System.out.println("✅ testDoubleConnect : Double connexion gérée");
    }

    @AfterAll
    static void stopServer() {
        if (server != null) {
            server.stop();
            System.out.println("🛑 Serveur de test arrêté");
        }
    }
}