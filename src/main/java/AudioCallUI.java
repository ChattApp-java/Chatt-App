
import javafx.animation.AnimationTimer;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Duration;

public class AudioCallUI extends Stage {

    private int     seconds = 0;
    private boolean muted   = false;

    private Timeline      callTimer;
    private AnimationTimer pulseTimer;

    public AudioCallUI(String other) {
        setTitle("Appel Audio");
        setResizable(false);

        // ── Root ──────────────────────────────────────────────
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: " + UIConstants.toHex(UIConstants.BG_DARK) + ";"); // ✅

        // ── CENTRE ────────────────────────────────────────────
        VBox centre = new VBox(0);
        centre.setAlignment(Pos.TOP_CENTER);
        centre.setPadding(new Insets(50, 30, 20, 30));

        PulsingAvatarCanvas pulse = new PulsingAvatarCanvas(other, 90);
        VBox.setMargin(pulse, new Insets(0, 0, 24, 0));

        Text nameLabel = new Text(other);
        nameLabel.setFont(Font.font("SansSerif", FontWeight.BOLD, 22));
        nameLabel.setFill(UIConstants.TEXT_PRIMARY);  // ✅

        Text timerLabel = new Text("00:00");
        timerLabel.setFont(Font.font("SansSerif", FontWeight.NORMAL, 14));
        timerLabel.setFill(UIConstants.ONLINE_GREEN); // ✅
        VBox.setMargin(timerLabel, new Insets(8, 0, 0, 0));

        callTimer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            seconds++;
            timerLabel.setText(String.format("%02d:%02d", seconds / 60, seconds % 60));
        }));
        callTimer.setCycleCount(Timeline.INDEFINITE);
        callTimer.play();

        centre.getChildren().addAll(pulse, nameLabel, timerLabel);
        root.setCenter(centre);

        // ── BOTTOM: Controls ──────────────────────────────────
        HBox controls = new HBox(20);
        controls.setAlignment(Pos.CENTER);
        controls.setPadding(new Insets(0, 0, 24, 0));

        Button btnMute   = pillButton("🎙️  Muet",       UIConstants.BG_SURFACE); // ✅
        Button btnHangUp = pillButton("📵  Raccrocher",  UIConstants.DANGER);     // ✅

        btnMute.setOnAction(e -> {
            muted = !muted;
            btnMute.setText(muted ? "🔇  Muet" : "🎙️  Muet");
        });
        btnHangUp.setOnAction(e -> stopAndClose());

        controls.getChildren().addAll(btnMute, btnHangUp);
        root.setBottom(controls);

        setOnCloseRequest(e -> stopAndClose());

        Scene scene = new Scene(root, 340, 420);
        setScene(scene);
        show();
        pulse.startAnimation();
    }

    // ── Helpers ───────────────────────────────────────────────

    private void stopAndClose() {
        if (callTimer  != null) callTimer.stop();
        if (pulseTimer != null) pulseTimer.stop();
        close();
    }

    /** Styled pill button — prend un javafx Color ✅ */
    private static Button pillButton(String text, Color bg) {
        Button btn = new Button(text);
        btn.setFont(Font.font("SansSerif", FontWeight.BOLD, 13));
        btn.setTextFill(Color.WHITE);
        btn.setStyle(
                "-fx-background-color: " + UIConstants.toHex(bg) + ";" +
                        "-fx-background-radius: 50;"   +
                        "-fx-padding: 12 20 12 20;"    +
                        "-fx-cursor: hand;"
        );
        btn.setOnMouseEntered(e -> btn.setOpacity(0.85));
        btn.setOnMouseExited(e  -> btn.setOpacity(1.0));
        return btn;
    }

    // ── Inner class: Pulsing avatar ───────────────────────────
    static class PulsingAvatarCanvas extends Canvas {

        private final String name;
        private final int    avatarSize;
        private       double pulse = 0.0;
        private AnimationTimer anim;

        PulsingAvatarCanvas(String name, int avatarSize) {
            super(avatarSize + 30, avatarSize + 30);
            this.name       = name;
            this.avatarSize = avatarSize;
        }

        void startAnimation() {
            anim = new AnimationTimer() {
                @Override public void handle(long now) {
                    pulse = (pulse + 0.05) % (Math.PI * 2);
                    draw();
                }
            };
            anim.start();
        }

        private void draw() {
            GraphicsContext gc = getGraphicsContext2D();
            double w  = getWidth();
            double h  = getHeight();
            double cx = w / 2;
            double cy = h / 2;

            gc.clearRect(0, 0, w, h);

            // ── Outer pulse ring ─────────────────────────────
            double alpha = 0.3 + 0.25 * Math.sin(pulse);
            double extra = 6   + 5    * Math.sin(pulse);
            double r     = avatarSize / 2.0 + extra;

            Color accent = UIConstants.ACCENT; // ✅ déjà javafx Color
            gc.setFill(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), alpha));
            gc.fillOval(cx - r, cy - r, r * 2, r * 2);

            // ── Avatar circle ────────────────────────────────
            gc.setFill(Utils.getAvatarColor(name)); // ✅ retourne déjà javafx Color
            double half = avatarSize / 2.0;
            gc.fillOval(cx - half, cy - half, avatarSize, avatarSize);

            // ── Initials ─────────────────────────────────────
            String ini     = Utils.getInitials(name);
            int  fontSize  = avatarSize / 3;
            gc.setFill(Color.WHITE);
            gc.setFont(Font.font("SansSerif", FontWeight.BOLD, fontSize));

            Text helper = new Text(ini);
            helper.setFont(gc.getFont());
            double textW = helper.getLayoutBounds().getWidth();
            double textH = helper.getLayoutBounds().getHeight();
            gc.fillText(ini, cx - textW / 2, cy + textH / 4);
        }

        // ✅ toFxColor() locale supprimée
    }
}