package frontend;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class ChatClient extends Application {

    @Override
    public void start(Stage primaryStage) {
        LoginApp login = new LoginApp(primaryStage);
        Scene scene = new Scene(login, 400, 500);
        scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
        
        primaryStage.setTitle("Chatt-App");
        primaryStage.setScene(scene);
        primaryStage.centerOnScreen();
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
