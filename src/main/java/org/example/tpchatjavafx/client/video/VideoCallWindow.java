package org.example.tpchatjavafx.client.video;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.example.tpchatjavafx.client.NetworkClient;

public class VideoCallWindow {

    private static Stage window;

    public static void open(NetworkClient client, String me, String other, boolean caller) {
        try {
            FXMLLoader loader = new FXMLLoader(VideoCallWindow.class.getResource("/fxml/video-call.fxml"));
            Scene scene = new Scene(loader.load());

            VideoCallController controller = loader.getController();
            controller.init(client, me, other);

            window = new Stage();
            window.setTitle("Video Call with " + other);
            window.setScene(scene);
            window.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void closeCurrent() {
        if (window != null) window.close();
    }
}
