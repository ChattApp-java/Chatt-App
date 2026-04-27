package frontend.composants;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import javafx.scene.shape.Circle;
import modele.Utilisateur;

public class UserListCell extends ListCell<Utilisateur> {

    private HBox content;
    private Label name;
    private Circle statusIndicator;

    public UserListCell() {
        super();
        name = new Label();
        name.getStyleClass().add("label-normal");
        
        statusIndicator = new Circle(5);
        
        content = new HBox(10);
        content.setAlignment(Pos.CENTER_LEFT);
        content.getChildren().addAll(statusIndicator, name);
    }

    @Override
    protected void updateItem(Utilisateur user, boolean empty) {
        super.updateItem(user, empty);
        if (user != null && !empty) {
            name.setText(user.getUsername());
            if (user.isStatus()) {
                statusIndicator.getStyleClass().setAll("status-online");
            } else {
                statusIndicator.getStyleClass().setAll("status-offline");
            }
            setGraphic(content);
        } else {
            setGraphic(null);
        }
    }
}
