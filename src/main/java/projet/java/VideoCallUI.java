package projet.java;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

/**
 * Video call window (JavaFX) with:
 *  - Two side-by-side video panels (placeholder)
 *  - Camera-off placeholder with avatar initials
 *  - Bottom control bar: Mute / Cam / Hang-up
 */
public class VideoCallUI extends Stage {

    private final ImageView ecranMoi   = new ImageView();
    private final ImageView ecranAutre = new ImageView();

    private boolean camOn = true;
    private boolean micOn = true;

    public VideoCallUI(String me, String other) {
        setTitle("Appel Vidéo");
        setResizable(true);

        // ── Video panels ──────────────────────────────────────
        HBox videosPanel = new HBox(3);
        videosPanel.setBackground(new Background(new BackgroundFill(
                UIConstants.BG_DARK, CornerRadii.EMPTY, Insets.EMPTY
        )));

        StackPane otherPanel = makeVideoPanel(other, false, ecranAutre);
        StackPane myPanel    = makeVideoPanel(me,    true,  ecranMoi);

        HBox.setHgrow(otherPanel, Priority.ALWAYS);
        HBox.setHgrow(myPanel,    Priority.ALWAYS);

        videosPanel.getChildren().addAll(otherPanel, myPanel);

        // ── Controls bar ──────────────────────────────────────
        HBox controls = new HBox(16);
        controls.setAlignment(Pos.CENTER);
        controls.setPadding(new Insets(14, 0, 14, 0));
        controls.setBackground(new Background(new BackgroundFill(
                UIConstants.BG_PANEL, CornerRadii.EMPTY, Insets.EMPTY
        )));

        Button btnMic = Utils.pillButton("🎙️  Micro",    UIConstants.BG_SURFACE, 50);
        Button btnCam = Utils.pillButton("📷  Caméra",   UIConstants.BG_SURFACE, 50);
        Button btnEnd = Utils.pillButton("📵  Terminer", UIConstants.DANGER,     50);

        btnMic.setOnAction(e -> {
            micOn = !micOn;
            btnMic.setText(micOn ? "🎙️  Micro" : "🔇  Micro");
        });

        btnCam.setOnAction(e -> {
            camOn = !camOn;
            btnCam.setText(camOn ? "📷  Caméra" : "🚫  Caméra");
        });

        btnEnd.setOnAction(e -> close());

        controls.getChildren().addAll(btnMic, btnCam, btnEnd);

        // ── Root ──────────────────────────────────────────────
        BorderPane root = new BorderPane();
        root.setCenter(videosPanel);
        root.setBottom(controls);

        Scene scene = new Scene(root, 820, 520);
        setScene(scene);
        show();
    }

    // ── Video panel builder ───────────────────────────────────

    private StackPane makeVideoPanel(String name, boolean isMe, ImageView screen) {
        StackPane outer = new StackPane();
        outer.setMinWidth(0);

        // Gradient background
        LinearGradient gradient = new LinearGradient(
                0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.rgb(22, 23, 40)),
                new Stop(1.0, Color.rgb(10, 10, 20))
        );
        outer.setBackground(new Background(new BackgroundFill(
                gradient, CornerRadii.EMPTY, Insets.EMPTY
        )));

        // ImageView fills panel when camera is active
        screen.setPreserveRatio(true);
        screen.setSmooth(true);
        screen.fitWidthProperty().bind(outer.widthProperty());
        screen.fitHeightProperty().bind(outer.heightProperty());

        // ── Avatar placeholder (centre) ───────────────────────
        VBox centre = new VBox(14);
        centre.setAlignment(Pos.CENTER);

        StackPane avatar = Utils.makeAvatar(name, 72);

        Label nameLabel = new Label(name + (isMe ? "  (Vous)" : ""));
        nameLabel.setFont(Font.font("SansSerif", FontWeight.BOLD, 15));
        nameLabel.setTextFill(UIConstants.TEXT_PRIMARY);

        centre.getChildren().addAll(avatar, nameLabel);

        // ── Name overlay (top-left) ───────────────────────────
        Label overlay = new Label(name);
        overlay.setFont(Font.font("SansSerif", FontWeight.BOLD, 12));
        overlay.setTextFill(UIConstants.TEXT_MUTED);
        overlay.setPadding(new Insets(8, 0, 0, 10));
        StackPane.setAlignment(overlay, Pos.TOP_LEFT);

        outer.getChildren().addAll(screen, centre, overlay);

        return outer;
    }

    // ── For camera integration ────────────────────────────────

    /** ImageView in "Vous" panel — set its image to display your camera feed. */
    public ImageView getEcranMoi()   { return ecranMoi; }

    /** ImageView in contact panel — set its image to display remote feed. */
    public ImageView getEcranAutre() { return ecranAutre; }
}