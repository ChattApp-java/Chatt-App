package projet.java;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.*;
import javafx.scene.control.Label;

/**
 * A chat bubble (JavaFX) with:
 *  - avatar circle on the left  (received)
 *  - no avatar, right-aligned   (sent)
 *  - sender name + time as subtitle
 *  - rounded bubble background
 */
public class MessageBubble extends HBox {

    private static final int MAX_WIDTH   = 260;
    private static final int AVATAR_SIZE = 34;
    private static final int ARC         = 20;

    public MessageBubble(String sender, String text, String time, boolean mine) {
        setSpacing(8);
        setPadding(new Insets(4, 6, 4, 6));
        setAlignment(Pos.BOTTOM_LEFT);

        // ── Avatar (received only) ───────────────────────────
        if (!mine) {
            StackPane avatar = Utils.makeAvatar(sender, AVATAR_SIZE);
            getChildren().add(avatar);
        }

        // ── Bubble ───────────────────────────────────────────
        VBox bubble = new VBox(4);
        bubble.setPadding(new Insets(10, 14, 10, 14));
        bubble.setMaxWidth(MAX_WIDTH + 28);

        Color bubbleColor = mine
                ? UIConstants.MSG_SENT  // ✅
                : UIConstants.MSG_RECV; // ✅

        bubble.setBackground(new Background(new BackgroundFill(
                bubbleColor,
                new CornerRadii(ARC),
                Insets.EMPTY
        )));

        // Subtle border on received
        if (!mine) {
            bubble.setBorder(new Border(new BorderStroke(
                    UIConstants.BORDER_COLOR,   // ✅
                    BorderStrokeStyle.SOLID,
                    new CornerRadii(ARC),
                    new BorderWidths(1)
            )));
        }

        // Drop shadow simulation via wrapper
        StackPane bubbleWrap = new StackPane();

        VBox shadow = new VBox();
        shadow.setBackground(new Background(new BackgroundFill(
                Color.color(0, 0, 0, 0.15),
                new CornerRadii(ARC),
                new Insets(-2, -2, 2, 2)
        )));
        shadow.setMaxWidth(MAX_WIDTH + 28);
        shadow.setMaxHeight(Region.USE_COMPUTED_SIZE);

        bubbleWrap.getChildren().addAll(shadow, bubble);
        bubbleWrap.setAlignment(Pos.CENTER);

        // ── Sender name (received only) ──────────────────────
        if (!mine) {
            Label nameLabel = new Label(sender);
            nameLabel.setFont(Font.font("SansSerif", FontWeight.BOLD, 11));
            nameLabel.setTextFill(UIConstants.TEXT_ACCENT);     // ✅
            bubble.getChildren().add(nameLabel);
        }

        // ── Message text ─────────────────────────────────────
        Label msg = new Label(text);
        msg.setFont(Font.font("SansSerif", FontWeight.NORMAL, 14));
        msg.setTextFill(UIConstants.TEXT_PRIMARY);              // ✅
        msg.setWrapText(true);
        msg.setMaxWidth(MAX_WIDTH);
        bubble.getChildren().add(msg);

        // ── Timestamp ────────────────────────────────────────
        Label timeLabel = new Label(time);
        timeLabel.setFont(Font.font("SansSerif", FontWeight.NORMAL, 10));
        timeLabel.setTextFill(UIConstants.TEXT_MUTED);          // ✅
        if (mine) {
            bubble.setAlignment(Pos.CENTER_RIGHT);
            timeLabel.setAlignment(Pos.CENTER_RIGHT);
        } else {
            timeLabel.setAlignment(Pos.CENTER_LEFT);
        }
        bubble.getChildren().add(timeLabel);

        getChildren().add(bubbleWrap);

        // Spacer on right for received
        if (!mine) {
            Region spacer = new Region();
            spacer.setMinWidth(40);
            getChildren().add(spacer);
        }
    }

    // ✅ toFxColor() locale supprimée
}