import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import java.util.List;

public class InterfaceGraphiqueChat extends Application {

    private String utilisateurConnecte;
    private final UserDAO userDAO = new UserDAO();
    private final MessageDAO messageDAO = new MessageDAO(); // Ajout du DAO des messages

    @Override
    public void start(Stage stage) {
        montrerLogin(stage);
    }

    private void montrerLogin(Stage stage) {
        VBox root = new VBox(18);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(45));
        root.setStyle("-fx-background-color: linear-gradient(to bottom, #f8fafc, #e0f2fe);");

        Label logo = new Label("💬");
        logo.setStyle("-fx-font-size: 65px;");

        Label titre = new Label("ChattApp");
        titre.setStyle("-fx-text-fill: #0f172a; -fx-font-size: 38px; -fx-font-weight: bold;");

        Label sousTitre = new Label("Connectez-vous pour continuer");
        sousTitre.setStyle("-fx-text-fill: #64748b; -fx-font-size: 15px;");

        TextField champUser = new TextField();
        champUser.setPromptText("Nom d'utilisateur");
        styleChamp(champUser);

        PasswordField champPass = new PasswordField();
        champPass.setPromptText("Mot de passe");
        styleChamp(champPass);

        Button btnLogin = new Button("Se connecter");
        styleBoutonPrincipal(btnLogin);

        btnLogin.setOnAction(e -> {
            String username = champUser.getText().trim();
            String password = champPass.getText().trim();

            if (username.isEmpty() || password.isEmpty()) {
                afficherErreur("Veuillez remplir tous les champs.");
                return;
            }

            User user = userDAO.authentifier(username, password);

            if (user != null) {
                utilisateurConnecte = user.getUsername();
                montrerAccueil(stage, utilisateurConnecte);
            } else {
                afficherErreur("Nom d'utilisateur ou mot de passe incorrect.");
            }
        });

        Hyperlink lienInscription = new Hyperlink("Créer un nouveau compte");
        lienInscription.setStyle("-fx-text-fill: #2563eb; -fx-font-size: 14px; -fx-border-color: transparent;");
        lienInscription.setOnAction(e -> montrerInscription(stage));

        root.getChildren().addAll(logo, titre, sousTitre, champUser, champPass, btnLogin, lienInscription);

        Scene scene = new Scene(root, 430, 620);
        stage.setTitle("ChattApp - Connexion");
        stage.setScene(scene);
        stage.show();
    }

    private void montrerInscription(Stage stage) {
        VBox root = new VBox(18);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(45));
        root.setStyle("-fx-background-color: linear-gradient(to bottom, #ffffff, #dbeafe);");

        Label logo = new Label("✨");
        logo.setStyle("-fx-font-size: 55px;");

        Label titre = new Label("Créer un compte");
        titre.setStyle("-fx-text-fill: #0f172a; -fx-font-size: 32px; -fx-font-weight: bold;");

        TextField champUser = new TextField();
        champUser.setPromptText("Nom d'utilisateur");
        styleChamp(champUser);

        TextField champEmail = new TextField();
        champEmail.setPromptText("Email");
        styleChamp(champEmail);

        PasswordField champPass = new PasswordField();
        champPass.setPromptText("Mot de passe");
        styleChamp(champPass);

        Button btnCreer = new Button("S'inscrire");
        styleBoutonPrincipal(btnCreer);

        btnCreer.setOnAction(e -> {
            String username = champUser.getText().trim();
            String email = champEmail.getText().trim();
            String password = champPass.getText().trim();

            if (username.isEmpty() || password.isEmpty() || email.isEmpty()) {
                afficherErreur("Tous les champs sont obligatoires.");
                return;
            }

            User newUser = new User(0, username, password, email, false, null);

            try {
                if (userDAO.inscrire(newUser)) {
                    afficherInfo("Compte créé avec succès.");
                    montrerLogin(stage);
                } else {
                    afficherErreur("Ce nom d'utilisateur existe déjà.");
                }
            } catch (Exception ex) {
                afficherErreur("Erreur de base de données : " + ex.getMessage());
            }
        });

        Button btnRetour = new Button("Retour à la connexion");
        styleBoutonSecondaire(btnRetour);
        btnRetour.setOnAction(e -> montrerLogin(stage));

        root.getChildren().addAll(logo, titre, champUser, champEmail, champPass, btnCreer, btnRetour);

        Scene scene = new Scene(root, 430, 620);
        stage.setTitle("ChattApp - Inscription");
        stage.setScene(scene);
    }

    private void montrerAccueil(Stage stage, String username) {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #f8fafc;");

        Label header = new Label("ChattApp");
        header.setStyle("-fx-text-fill: white; -fx-font-size: 24px; -fx-font-weight: bold; -fx-padding: 18; -fx-background-color: #2563eb;");

        // TEST : Chargement de l'historique lors de la connexion
        List<Message> historiqueTest = messageDAO.getHistorique(username, "Lamiae");

        Label message = new Label("Bienvenue " + username + "\n" +
                "Historique récupéré : " + historiqueTest.size() + " messages.");
        message.setStyle("-fx-text-fill: #1e293b; -fx-font-size: 20px; -fx-font-weight: bold; -fx-text-alignment: center;");

        Button btnDeconnexion = new Button("Déconnexion");
        styleBoutonSecondaire(btnDeconnexion);

        btnDeconnexion.setOnAction(e -> {
            userDAO.deconnecter(username);
            utilisateurConnecte = null;
            montrerLogin(stage);
        });

        VBox center = new VBox(25, message, btnDeconnexion);
        center.setAlignment(Pos.CENTER);

        root.setTop(header);
        root.setCenter(center);

        Scene scene = new Scene(root, 900, 600);
        stage.setTitle("ChattApp");
        stage.setScene(scene);
    }

    private void styleChamp(Control champ) {
        champ.setMaxWidth(310);
        champ.setStyle("-fx-background-color: white; -fx-text-fill: #0f172a; -fx-prompt-text-fill: #94a3b8; -fx-background-radius: 14; -fx-border-radius: 14; -fx-border-color: #cbd5e1; -fx-border-width: 1; -fx-padding: 13; -fx-font-size: 14px;");
    }

    private void styleBoutonPrincipal(Button bouton) {
        bouton.setMaxWidth(310);
        bouton.setStyle("-fx-background-color: #2563eb; -fx-text-fill: white; -fx-font-size: 15px; -fx-font-weight: bold; -fx-background-radius: 14; -fx-padding: 13 22 13 22; -fx-cursor: hand;");
    }

    private void styleBoutonSecondaire(Button bouton) {
        bouton.setMaxWidth(310);
        bouton.setStyle("-fx-background-color: white; -fx-text-fill: #2563eb; -fx-font-size: 14px; -fx-font-weight: bold; -fx-background-radius: 14; -fx-border-color: #2563eb; -fx-border-radius: 14; -fx-padding: 11 20 11 20; -fx-cursor: hand;");
    }

    private void afficherErreur(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void afficherInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Information");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @Override
    public void stop() {
        if (utilisateurConnecte != null) {
            userDAO.deconnecter(utilisateurConnecte);
        }
        DatabaseConnection.closePool();
    }

    public static void main(String[] args) {
        launch(args);
    }
}