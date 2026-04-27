package frontend.composants;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import modele.Message;
import java.time.format.DateTimeFormatter;

public class ChatBubbleFX extends HBox {

    public ChatBubbleFX(Message msg, boolean isSender) {
        super();
        
        VBox bubble = new VBox(5);
        Label content = new Label(msg.getContenu());
        content.setWrapText(true);
        content.setMaxWidth(400);
        
        Label time = new Label(msg.getDate_envoi().format(DateTimeFormatter.ofPattern("HH:mm")));
        time.getStyleClass().add("bubble-time");
        
        if (isSender) {
            this.setAlignment(Pos.CENTER_RIGHT);
            content.getStyleClass().add("bubble-sender");
            bubble.setAlignment(Pos.TOP_RIGHT);
        } else {
            this.setAlignment(Pos.CENTER_LEFT);
            content.getStyleClass().add("bubble-receiver");
            bubble.setAlignment(Pos.TOP_LEFT);
        }
        
        bubble.getChildren().addAll(content, time);
        this.getChildren().add(bubble);
        this.setPadding(new Insets(5, 10, 5, 10));
    }
}
