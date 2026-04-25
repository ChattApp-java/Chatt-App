package projet.java;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 * Incoming call notification dialog (JavaFX).
 * Shows contact name, call type.
 * Accept / Decline buttons set isAccepte().
 */
public class NotificationAppel extends Stage {

    private boolean accepte = false;

    public NotificationAppel(Stage parent, String contact, String typeAppel) {
        initOwner(parent);
        initModality(Modality.APPLICATION_MODAL);
        initStyle(StageStyle.TRANSPARENT);
        setTitle("Appel entrant");
        setResizable(false);

        // ── Root ─────────────────────────────────────────────
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(24, 24, 20, 24));
        root.setBackground(new Background(new BackgroundFill(
                UIConstants.BG_SURFACE,         // ✅
                new CornerRadii(20),
                Insets.EMPTY
        )));
        root.setBorder(new Border(new BorderStroke(
                UIConstants.BORDER_COLOR,        // ✅
                BorderStrokeStyle.SOLID,
                new CornerRadii(20),
                new BorderWidths(1)
        )));

        // ── TOP: Icon + call type + contact name ──────────────
        HBox topRow = new HBox(10);
        topRow.setAlignment(Pos.CENTER_LEFT);

        String typeIcon = typeAppel.equalsIgnoreCase("Vidéo") ? "📹" : "📞";
        Label iconLabel = new Label(typeIcon);
        iconLabel.setFont(Font.font("Segoe UI Emoji", 22));

        VBox typeCol = new VBox(2);

        Label incoming = new Label("Appel " + typeAppel);
        incoming.setFont(Font.font("SansSerif", FontWeight.NORMAL, 11));
        incoming.setTextFill(UIConstants.TEXT_MUTED);           // ✅

        Label contactLabel = new Label(contact);
        contactLabel.setFont(Font.font("SansSerif", FontWeight.BOLD, 17));
        contactLabel.setTextFill(UIConstants.TEXT_PRIMARY);     // ✅

        typeCol.getChildren().addAll(incoming, contactLabel);
        topRow.getChildren().addAll(iconLabel, typeCol);
        root.setTop(topRow);

        // ── CENTER: Sub-label ─────────────────────────────────
        Label sub = new Label("souhaite vous parler…");
        sub.setFont(Font.font("SansSerif", FontPosture.ITALIC, 13));
        sub.setTextFill(UIConstants.TEXT_MUTED);                // ✅
        sub.setPadding(new Insets(10, 0, 10, 0));
        root.setCenter(sub);

        // ── BOTTOM: Buttons ───────────────────────────────────
        HBox btnPanel = new HBox(12);
        btnPanel.setAlignment(Pos.CENTER);
        btnPanel.setPadding(new Insets(4, 0, 0, 0));

        Button btnRefuser  = styledButton("✖  Refuser",  UIConstants.DANGER);   // ✅
        Button btnAccepter = styledButton("✔  Accepter", UIConstants.SUCCESS);  // ✅

        btnRefuser.setMaxWidth(Double.MAX_VALUE);
        btnAccepter.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(btnRefuser,  Priority.ALWAYS);
        HBox.setHgrow(btnAccepter, Priority.ALWAYS);

        btnRefuser.setOnAction(e  -> { accepte = false; close(); });
        btnAccepter.setOnAction(e -> { accepte = true;  close(); });

        btnPanel.getChildren().addAll(btnRefuser, btnAccepter);
        root.setBottom(btnPanel);

        // ── Scene (transparent background) ───────────────────
        Scene scene = new Scene(root, 340, 210);
        scene.setFill(Color.TRANSPARENT);
        setScene(scene);

        if (parent != null) {
            setX(parent.getX() + parent.getWidth()  / 2 - 170);
            setY(parent.getY() + parent.getHeight() / 2 - 105);
        }
    }

    public boolean isAccepte() { return accepte; }

    // ── Helpers ───────────────────────────────────────────────

    private static Button styledButton(String text, Color bg) {  // ✅ javafx Color
        Button btn = new Button(text);
        btn.setFont(Font.font("SansSerif", FontWeight.BOLD, 13));
        btn.setTextFill(Color.WHITE);
        btn.setPadding(new Insets(10, 0, 10, 0));
        btn.setStyle(
                "-fx-background-color: " + UIConstants.toHex(bg) + ";" +  // ✅
                        "-fx-background-radius: 10;" +
                        "-fx-cursor: hand;"
        );
        btn.setOnMouseEntered(e -> btn.setOpacity(0.85));
        btn.setOnMouseExited(e  -> btn.setOpacity(1.0));
        return btn;
    }

    // ✅ toFxColor() et toHex() locaux supprimés
}