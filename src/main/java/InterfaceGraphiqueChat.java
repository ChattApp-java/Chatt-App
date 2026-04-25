import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

public class InterfaceGraphiqueChat extends Application {
    private final ClientReseau client = new ClientReseau();
    private final ObservableList<String> contacts = FXCollections.observableArrayList();
    private final VBox zoneMessages = new VBox(10);
    private String destinataireActuel = null;

    @Override
    public void start(Stage stage) {
        client.setActions(
                nom -> montrerChat(stage, nom),
                liste -> contacts.setAll(liste),
                msg -> ajouterBulle(msg, false)
        );
        montrerLogin(stage);
    }

    private void montrerLogin(Stage stage) {
        VBox racine = new VBox(20);
        racine.setAlignment(Pos.CENTER);
        racine.setPadding(new Insets(40));
        racine.setStyle("-fx-background-color: #101d25;");

        Label iconeChat = new Label("💬");
        iconeChat.setStyle("-fx-font-size: 70px; -fx-text-fill: #323d45;");

        Label titreApp = new Label("ChatApp");
        titreApp.setStyle("-fx-font-size: 36px; -fx-font-weight: bold; -fx-text-fill: white;");

        Label sousTitre = new Label("Connectez-vous pour discuter");
        sousTitre.setStyle("-fx-text-fill: #8696a0; -fx-font-size: 14px;");

        TextField champUtilisateur = new TextField();
        champUtilisateur.setPromptText("Nom d'utilisateur");
        styleChamp(champUtilisateur);

        PasswordField champMdp = new PasswordField();
        champMdp.setPromptText("Mot de passe");
        styleChamp(champMdp);

        Button boutonSeConnecter = new Button("Se connecter");
        boutonSeConnecter.setMaxWidth(Double.MAX_VALUE);
        boutonSeConnecter.setStyle("-fx-background-color: #00a884; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 16px; -fx-background-radius: 20; -fx-padding: 12;");

        boutonSeConnecter.setOnAction(e -> {
            if (!champUtilisateur.getText().isEmpty()) {
                client.connecter(champUtilisateur.getText());
            }
        });

        Hyperlink lienSInscrire = new Hyperlink("Pas de compte ? S'inscrire");
        lienSInscrire.setStyle("-fx-text-fill: #00a884; -fx-underline: false;");

        racine.getChildren().addAll(iconeChat, titreApp, sousTitre, champUtilisateur, champMdp, boutonSeConnecter, lienSInscrire);
        stage.setScene(new Scene(racine, 400, 600));
        stage.setTitle("ChatApp - Connexion");
        stage.show();
    }

    private void styleChamp(Control c) {
        c.setStyle("-fx-background-color: #2a3942; -fx-text-fill: white; -fx-prompt-text-fill: #8696a0; -fx-background-radius: 5; -fx-padding: 12; -fx-border-color: #00a884; -fx-border-width: 0 0 1 0;");
    }

    private void montrerChat(Stage stage, String monNom) {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #0b141a;");

        ListView<String> listV = new ListView<>(contacts);
        listV.setStyle("-fx-control-inner-background: #111b21; -fx-background-color: #111b21;");
        listV.getSelectionModel().selectedItemProperty().addListener((obs, old, val) -> destinataireActuel = val);

        ScrollPane scroll = new ScrollPane(zoneMessages);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: #0b141a; -fx-background-color: transparent;");

        TextField input = new TextField();
        input.setPromptText("Tapez un message...");
        styleChamp(input);

        Button send = new Button("➤");
        send.setStyle("-fx-background-color: #00a884; -fx-text-fill: white; -fx-background-radius: 50;");
        send.setOnAction(e -> {
            if (destinataireActuel != null && !input.getText().isEmpty()) {
                client.envoyer(destinataireActuel, input.getText());
                ajouterBulle("Moi : " + input.getText(), true);
                input.clear();
            }
        });

        HBox bottom = new HBox(10, input, send);
        bottom.setPadding(new Insets(10));
        HBox.setHgrow(input, Priority.ALWAYS);

        root.setLeft(listV);
        root.setCenter(scroll);
        root.setBottom(bottom);

        stage.setScene(new Scene(root, 800, 600));
    }

    private void ajouterBulle(String texte, boolean isMoi) {
        Platform.runLater(() -> {
            Label bulle = new Label(texte);
            bulle.setPadding(new Insets(10));
            bulle.setStyle("-fx-background-color: " + (isMoi ? "#005c4b" : "#202c33") + "; -fx-text-fill: white; -fx-background-radius: 10;");
            HBox ligne = new HBox(bulle);
            ligne.setAlignment(isMoi ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
            zoneMessages.getChildren().add(ligne);
        });
    }

    public static void main(String[] args) { launch(args); }
}