import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * Top navigation bar (JavaFX) with:
 *  - Avatar + name + "En ligne" status
 *  - Audio & video call buttons
 *  - Bottom separator line
 */
public class TopBar extends BorderPane {

    public final Button btnAudio;
    public final Button btnVideo;

    public TopBar(String user) {
        setPadding(new Insets(12, 18, 12, 18));
        setBackground(new Background(new BackgroundFill(
                UIConstants.BG_PANEL,           // ✅
                CornerRadii.EMPTY,
                Insets.EMPTY
        )));

        // Bottom separator line
        setBorder(new Border(new BorderStroke(
                Color.TRANSPARENT,
                Color.TRANSPARENT,
                UIConstants.BORDER_COLOR,        // ✅
                Color.TRANSPARENT,
                BorderStrokeStyle.NONE,
                BorderStrokeStyle.NONE,
                BorderStrokeStyle.SOLID,
                BorderStrokeStyle.NONE,
                CornerRadii.EMPTY,
                new BorderWidths(0, 0, 1, 0),
                Insets.EMPTY
        )));

        // ── LEFT : Avatar + Name + Status ────────────────────
        HBox leftPanel = new HBox(12);
        leftPanel.setAlignment(Pos.CENTER_LEFT);

        StackPane avatar = Utils.makeAvatar(user, 40);

        VBox nameCol = new VBox(2);
        nameCol.setAlignment(Pos.CENTER_LEFT);

        Label nameLabel = new Label(user);
        nameLabel.setFont(Font.font("SansSerif", FontWeight.BOLD, 15));
        nameLabel.setTextFill(UIConstants.TEXT_PRIMARY);        // ✅

        HBox statusRow = new HBox(4);
        statusRow.setAlignment(Pos.CENTER_LEFT);

        Circle dot = new Circle(4, UIConstants.ONLINE_GREEN);  // ✅

        Label statusLabel = new Label("En ligne");
        statusLabel.setFont(Font.font("SansSerif", FontWeight.NORMAL, 11));
        statusLabel.setTextFill(UIConstants.ONLINE_GREEN);     // ✅

        statusRow.getChildren().addAll(dot, statusLabel);
        nameCol.getChildren().addAll(nameLabel, statusRow);
        leftPanel.getChildren().addAll(avatar, nameCol);
        setLeft(leftPanel);

        // ── RIGHT : Call buttons ──────────────────────────────
        HBox btnPanel = new HBox(8);
        btnPanel.setAlignment(Pos.CENTER_RIGHT);

        btnAudio = new Button("📞  Audio");
        btnAudio.setFont(Font.font("SansSerif", FontWeight.NORMAL, 13));
        btnAudio.setTextFill(UIConstants.TEXT_PRIMARY);        // ✅
        btnAudio.setStyle(
                "-fx-background-color: " + UIConstants.toHex(UIConstants.BG_SURFACE)    + ";" +
                        "-fx-background-radius: 10;"                                              +
                        "-fx-border-color: "      + UIConstants.toHex(UIConstants.BORDER_COLOR) + ";" +
                        "-fx-border-radius: 10;"  +
                        "-fx-border-width: 1;"    +
                        "-fx-padding: 6 14 6 14;" +
                        "-fx-cursor: hand;"
        );
        btnAudio.setOnMouseEntered(e -> btnAudio.setOpacity(0.85));
        btnAudio.setOnMouseExited(e  -> btnAudio.setOpacity(1.0));

        btnVideo = new Button("📹  Vidéo");
        btnVideo.setFont(Font.font("SansSerif", FontWeight.BOLD, 13));
        btnVideo.setTextFill(Color.WHITE);
        btnVideo.setStyle(
                "-fx-background-color: " + UIConstants.toHex(UIConstants.ACCENT) + ";" +  // ✅
                        "-fx-background-radius: 10;" +
                        "-fx-padding: 7 15 7 15;"    +
                        "-fx-cursor: hand;"
        );
        btnVideo.setOnMouseEntered(e -> btnVideo.setOpacity(0.85));
        btnVideo.setOnMouseExited(e  -> btnVideo.setOpacity(1.0));

        btnPanel.getChildren().addAll(btnAudio, btnVideo);
        setRight(btnPanel);

        // ✅ toFxColor() et toHex() locaux supprimés
    }
}