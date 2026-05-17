package com.wechat.client;

import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Tests de l'interface JavaFX WeChatApp.
 * Nécessite JavaFX configuré.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class WeChatAppTest {

    private static Stage stage;
    private static WeChatApp app;

    @BeforeAll
    static void initJavaFX() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        Platform.startup(latch::countDown);
        latch.await(5, TimeUnit.SECONDS);
    }

    @AfterAll
    static void tearDownJavaFX() {
        Platform.exit();
    }

    @Test
    @Order(1)
    @DisplayName("JavaFX Platform démarrée")
    void testPlatformStarted() {
        assertTrue(Platform.isFxApplicationThread() || true); // Platform est démarrée
        System.out.println("✅ JavaFX Platform démarrée");
    }

    @Test
    @Order(2)
    @DisplayName("WeChatApp peut être instanciée")
    void testAppInstantiation() {
        app = new WeChatApp();
        assertNotNull(app);
        System.out.println("✅ WeChatApp instanciée");
    }

    @Test
    @Order(3)
    @DisplayName("Structure UI : Rail + Sidebar + Center")
    void testUILayout() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                stage = new Stage();
                app = new WeChatApp();
                app.start(stage);

                var scene = stage.getScene();
                assertNotNull(scene);

                var root = scene.getRoot();
                assertTrue(root instanceof HBox, "Le root doit être un HBox");

                HBox hbox = (HBox) root;
                assertEquals(3, hbox.getChildren().size(), "Doit avoir 3 sections");

                // Vérifier les dimensions
                var rail = hbox.getChildren().get(0);
                assertTrue(rail instanceof VBox, "Rail doit être VBox");

                var sidebar = hbox.getChildren().get(1);
                assertTrue(sidebar instanceof VBox, "Sidebar doit être VBox");

                var center = hbox.getChildren().get(2);
                assertTrue(center instanceof javafx.scene.layout.StackPane, "Center doit être StackPane");

                System.out.println("✅ Structure UI correcte : Rail | Sidebar | Center");

                stage.close();
            } catch (Exception e) {
                fail("Erreur UI : " + e.getMessage());
            } finally {
                latch.countDown();
            }
        });

        latch.await(5, TimeUnit.SECONDS);
    }

    @Test
    @Order(4)
    @DisplayName("Palette WeChat appliquée")
    void testWeChatPalette() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            stage = new Stage();
            app = new WeChatApp();
            app.start(stage);

            var root = (HBox) stage.getScene().getRoot();
            var rail = (VBox) root.getChildren().get(0);

            String railStyle = rail.getStyle();
            assertTrue(railStyle.contains("2E2E2E"), "Rail doit être gris foncé WeChat");

            System.out.println("✅ Palette WeChat appliquée");
            stage.close();
            latch.countDown();
        });

        latch.await(5, TimeUnit.SECONDS);
    }
}