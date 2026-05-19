package org.example.tpchatjavafx.client.video;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.example.tpchatjavafx.client.NetworkClient;
import org.example.tpchatjavafx.client.util.WindowSizingUtil;

public class VideoCallWindow {

    private static Stage window;
    private static VideoCallController controller;
    private static boolean closingFromCode;

    public static void open(NetworkClient client, String me, String other, boolean caller) {
        try {
            FXMLLoader loader = new FXMLLoader(VideoCallWindow.class.getResource("/fxml/video-call.fxml"));
            Scene scene = new Scene(loader.load());

            controller = loader.getController();
            controller.init(client, me, other);

            window = new Stage();
            window.setTitle("Appel video - " + other);
            window.setScene(scene);
            window.setResizable(true);
            WindowSizingUtil.applyResponsiveStageSize(window, 860, 560, 620, 430);

            window.setOnCloseRequest(e -> {
                if (!closingFromCode && controller != null) {
                    e.consume();
                    controller.onEndCall();
                }
            });

            window.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void closeCurrent() {
        if (window != null) {
            closingFromCode = true;
            window.close();
            closingFromCode = false;
            window = null;
            controller = null;
        }
    }
}
