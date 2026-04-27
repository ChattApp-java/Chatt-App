
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.Text;

/**
 * Scrollable chat message area (JavaFX).
 * Messages are added via addMessage() / addSystemMessage().
 */
public class ChatPanel extends ScrollPane {

    private final VBox content;

    public ChatPanel() {
        content = new VBox(4);
        content.setPadding(new Insets(16, 8, 16, 8));
        content.setBackground(new Background(new BackgroundFill(
                UIConstants.BG_DARK,        // ✅ déjà javafx.scene.paint.Color
                CornerRadii.EMPTY,
                Insets.EMPTY
        )));

        content.setFillWidth(true);

        setContent(content);
        setFitToWidth(true);
        setBorder(Border.EMPTY);
        setHbarPolicy(ScrollBarPolicy.NEVER);
        setVbarPolicy(ScrollBarPolicy.AS_NEEDED);

        setBackground(new Background(new BackgroundFill(
                UIConstants.BG_DARK,        // ✅
                CornerRadii.EMPTY,
                Insets.EMPTY
        )));

        setStyle(
                "-fx-background: "       + UIConstants.toHex(UIConstants.BG_DARK) + ";" +
                        "-fx-background-color: " + UIConstants.toHex(UIConstants.BG_DARK) + ";" +
                        "-fx-padding: 0;"
        );

        if (getClass().getResource("/chat_scroll.css") != null) {
            getStylesheets().add(getClass().getResource("/chat_scroll.css").toExternalForm());
        }
    }

    // ── Public API ────────────────────────────────────────────

    public void addMessage(String sender, String text, String time, boolean mine) {
        HBox wrapper = new HBox();
        wrapper.setAlignment(mine ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        wrapper.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(wrapper, Priority.ALWAYS);
        wrapper.getChildren().add(new MessageBubble(sender, text, time, mine));
        content.getChildren().add(wrapper);
        scrollBottom();
    }

    public void addSystemMessage(String text) {
        HBox wrapper = new HBox();
        wrapper.setAlignment(Pos.CENTER);
        wrapper.setMaxWidth(Double.MAX_VALUE);
        wrapper.setPadding(new Insets(4, 0, 4, 0));

        StackPane pill = new StackPane();
        pill.setPadding(new Insets(4, 14, 4, 14));
        pill.setBackground(new Background(new BackgroundFill(
                UIConstants.BG_SURFACE,     // ✅
                new CornerRadii(14),
                Insets.EMPTY
        )));

        Text label = new Text(text);
        label.setFont(Font.font("SansSerif", FontPosture.ITALIC, 11));
        label.setFill(UIConstants.TEXT_MUTED); // ✅

        pill.getChildren().add(label);
        wrapper.getChildren().add(pill);
        content.getChildren().add(wrapper);
    }

    // ── Helpers ───────────────────────────────────────────────

    private void scrollBottom() {
        Platform.runLater(() -> setVvalue(1.0));
    }

    // ✅ toFxColor() et toHex() locaux supprimés
    //    UIConstants retourne déjà javafx.scene.paint.Color
    //    et UIConstants.toHex() prend un javafx Color
}