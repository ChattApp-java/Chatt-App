package frontend;

import audio.EmetteurAudio;
import audio.RecepteurAudio;
import commun.Protocole;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import video.EmetteurVideo;
import video.RecepteurVideo;
import java.time.Instant;
import base_de_donnees.DAOAppel;

public class AppelWindow {

    private Stage stage;
    private String type;
    private String partner;
    private client.ConnexionServeur connexion;

    private EmetteurAudio emetteurAudio;
    private RecepteurAudio recepteurAudio;
    private EmetteurVideo emetteurVideo;
    private RecepteurVideo recepteurVideo;
    private Instant startTime;
    private DAOAppel daoAppel = new DAOAppel();
    private int idEmetteur;
    private int idRecepteur;

    public AppelWindow(String type, String partner, client.ConnexionServeur connexion, int idEmetteur, int idRecepteur) {
        this.type = type;
        this.partner = partner;
        this.connexion = connexion;
        this.idEmetteur = idEmetteur;
        this.idRecepteur = idRecepteur;
        this.startTime = Instant.now();
        setupUI();
    }

    private void setupUI() {
        stage = new Stage();
        stage.setTitle("Appel " + type + " - " + partner);

        VBox root = new VBox(20);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(30));
        root.getStyleClass().add("glass-pane");

        Label lblUser = new Label(partner);
        lblUser.getStyleClass().add("label-header");

        Label lblStatus = new Label("En communication...");
        lblStatus.getStyleClass().add("label-subheader");

        Button btnHangup = new Button("🛑 Raccrocher");
        btnHangup.getStyleClass().add("button-danger");
        btnHangup.setOnAction(e -> raccrocher());

        root.getChildren().addAll(lblUser, lblStatus, btnHangup);

        Scene scene = new Scene(root, 400, 300);
        scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
        stage.setScene(scene);
        stage.show();

        stage.setOnCloseRequest(e -> raccrocher());
    }

    public void demarrerFlux(String host) {
        try {
            // Audio
            emetteurAudio = new EmetteurAudio();
            emetteurAudio.demarrer(host);
            
            recepteurAudio = new RecepteurAudio();
            recepteurAudio.demarrer(host);

            // Vidéo
            if (type.equalsIgnoreCase("video")) {
                emetteurVideo = new EmetteurVideo();
                emetteurVideo.demarrer(host);
                
                // Note: RecepteurVideo afficherait l'image dans l'UI idéalement
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void raccrocher() {
        if (emetteurAudio != null) emetteurAudio.arreter();
        if (recepteurAudio != null) recepteurAudio.arreter();
        if (emetteurVideo != null) emetteurVideo.arreter();
        
        long duration = java.time.Duration.between(startTime, Instant.now()).getSeconds();
        daoAppel.enregistrerAppel(idEmetteur, idRecepteur, type, (int)duration);

        connexion.envoyer(Protocole.CALL_END + Protocole.SEP + partner);
        Platform.runLater(() -> stage.close());
    }

    public void fermer() {
        Platform.runLater(() -> stage.close());
    }
}
