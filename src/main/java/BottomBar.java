import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * Bottom input bar (JavaFX):
 *  - Rounded input container (dark surface)
 *  - Placeholder text hint
 *  - Send button (accent colour, pill shape)
 *  - Top separator line
 */
public class BottomBar extends HBox {

    private static final String PLACEHOLDER = "Écrire un message…";

    private final TextField field;

    public BottomBar(Runnable sendAction) {
        setSpacing(10);
        setAlignment(Pos.CENTER);
        setPadding(new Insets(12, 16, 12, 16));

        // Top border separator
        setBorder(new Border(new BorderStroke(
                UIConstants.BORDER_COLOR,       // ✅ déjà javafx Color
                Color.TRANSPARENT,
                Color.TRANSPARENT,
                Color.TRANSPARENT,
                BorderStrokeStyle.SOLID,
                BorderStrokeStyle.NONE,
                BorderStrokeStyle.NONE,
                BorderStrokeStyle.NONE,
                CornerRadii.EMPTY,
                new BorderWidths(1, 0, 0, 0),
                Insets.EMPTY
        )));

        setBackground(new Background(new BackgroundFill(
                UIConstants.BG_PANEL,           // ✅
                CornerRadii.EMPTY,
                Insets.EMPTY
        )));

        // ── Input container (rounded) ────────────────────────
        StackPane inputContainer = new StackPane();
        inputContainer.setPadding(new Insets(0, 12, 0, 16));
        inputContainer.setBackground(new Background(new BackgroundFill(
                UIConstants.BG_INPUT,           // ✅
                new CornerRadii(24),
                Insets.EMPTY
        )));
        HBox.setHgrow(inputContainer, Priority.ALWAYS);

        field = new TextField();
        field.setFont(Font.font("SansSerif", FontWeight.NORMAL, 14));
        field.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-text-fill: "         + UIConstants.toHex(UIConstants.TEXT_MUTED) + ";" +
                        "-fx-prompt-text-fill: "  + UIConstants.toHex(UIConstants.TEXT_MUTED) + ";" +
                        "-fx-padding: 10 0 10 0;" +
                        "-fx-border-width: 0;"    +
                        "-fx-background-insets: 0;"
        );
        field.setPromptText(PLACEHOLDER);
        field.setText(PLACEHOLDER);

        // Placeholder behaviour
        field.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (isFocused) {
                if (field.getText().equals(PLACEHOLDER)) {
                    field.setText("");
                }
                field.setStyle(
                        "-fx-background-color: transparent;" +
                                "-fx-text-fill: "         + UIConstants.toHex(UIConstants.TEXT_PRIMARY) + ";" +
                                "-fx-padding: 10 0 10 0;" +
                                "-fx-border-width: 0;"    +
                                "-fx-background-insets: 0;"
                );
                // Focus ring
                inputContainer.setBorder(new Border(new BorderStroke(
                        UIConstants.ACCENT.deriveColor(0, 1, 1, 0.47), // ✅
                        BorderStrokeStyle.SOLID,
                        new CornerRadii(24),
                        new BorderWidths(2)
                )));
            } else {
                if (field.getText().isEmpty()) {
                    field.setText(PLACEHOLDER);
                    field.setStyle(
                            "-fx-background-color: transparent;" +
                                    "-fx-text-fill: "         + UIConstants.toHex(UIConstants.TEXT_MUTED) + ";" +
                                    "-fx-padding: 10 0 10 0;" +
                                    "-fx-border-width: 0;"    +
                                    "-fx-background-insets: 0;"
                    );
                }
                inputContainer.setBorder(Border.EMPTY);
            }
        });

        field.setOnAction(e -> sendAction.run());

        StackPane.setAlignment(field, Pos.CENTER_LEFT);
        inputContainer.getChildren().add(field);

        // ── Send button ──────────────────────────────────────
        Button send = pillButton("Envoyer  ›", UIConstants.ACCENT);
        send.setOnAction(e -> sendAction.run());

        getChildren().addAll(inputContainer, send);
    }

    // ── Public API ────────────────────────────────────────────

    public String getMessage() {
        String txt = field.getText();
        return txt.equals(PLACEHOLDER) ? "" : txt;
    }

    public void clear() {
        field.setText("");
    }

    // ── Helpers ───────────────────────────────────────────────

    private static Button pillButton(String text, Color bg) {  // ✅ javafx Color
        Button btn = new Button(text);
        btn.setFont(Font.font("SansSerif", FontWeight.BOLD, 13));
        btn.setTextFill(Color.WHITE);
        btn.setStyle(
                "-fx-background-color: " + UIConstants.toHex(bg) + ";" +
                        "-fx-background-radius: 50;" +
                        "-fx-padding: 10 20 10 20;" +
                        "-fx-cursor: hand;"
        );
        btn.setOnMouseEntered(e -> btn.setOpacity(0.85));
        btn.setOnMouseExited(e  -> btn.setOpacity(1.0));
        return btn;
    }

    // ✅ toFxColor() et toHex() locaux supprimés
}