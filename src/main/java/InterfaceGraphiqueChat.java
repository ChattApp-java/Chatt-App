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

import java.sql.*;

public class InterfaceGraphiqueChat extends Application {

    private final ObservableList<String> contacts =
            FXCollections.observableArrayList();

    private final VBox zoneMessages = new VBox(10);

    private String destinataireActuel = null;

    /*
       CONFIGURATION MYSQL
    */
    private static final String URL =
            "jdbc:mysql://localhost:3306/chatapp"; // ⚠️ corrigé port standard

    private static final String USER = "root";
    private static final String PASSWORD = "";

    @Override
    public void start(Stage stage) {
        montrerLogin(stage);
    }

    /*
       =========================
       LOGIN
       =========================
    */

    private void montrerLogin(Stage stage) {

        VBox racine = new VBox(18);
        racine.setAlignment(Pos.CENTER);
        racine.setPadding(new Insets(40));

        racine.setStyle("-fx-background-color: linear-gradient(to bottom,#1e1e2f,#12121c);");

        Label titre = new Label("ChatSecure");
        titre.setStyle("-fx-text-fill: white; -fx-font-size: 34px; -fx-font-weight: bold;");

        TextField champUser = new TextField();
        champUser.setPromptText("Nom d'utilisateur");
        styleChamp(champUser);

        PasswordField champPass = new PasswordField();
        champPass.setPromptText("Mot de passe");
        styleChamp(champPass);

        Button btnLogin = new Button("Connexion");
        styleBouton(btnLogin);

        btnLogin.setOnAction(e -> {

            String username = champUser.getText().trim();
            String password = champPass.getText().trim();

            if (verifierConnexion(username, password)) {

                chargerUtilisateurs(username);
                montrerChat(stage, username);

            } else {
                afficherErreur("Login incorrect");
            }
        });

        Hyperlink inscrire = new Hyperlink("Créer un compte");
        inscrire.setOnAction(e -> montrerInscription(stage));

        racine.getChildren().addAll(titre, champUser, champPass, btnLogin, inscrire);

        stage.setScene(new Scene(racine, 420, 620));
        stage.setTitle("ChatSecure");
        stage.show();
    }

    /*
       =========================
       INSCRIPTION CORRIGÉE
       =========================
    */

    private void montrerInscription(Stage stage) {

        VBox root = new VBox(18);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(40));
        root.setStyle("-fx-background-color: #161625;");

        TextField username = new TextField();
        username.setPromptText("Nom d'utilisateur");
        styleChamp(username);

        PasswordField password = new PasswordField();
        password.setPromptText("Mot de passe");
        styleChamp(password);

        Button creer = new Button("S'inscrire");
        styleBouton(creer);

        creer.setOnAction(e -> {

            String user = username.getText().trim();
            String pass = password.getText().trim();

            if (user.isEmpty() || pass.isEmpty()) {
                afficherErreur("Champs obligatoires");
                return;
            }

            if (utilisateurExiste(user)) {
                afficherErreur("Utilisateur existe déjà");
                return;
            }

            boolean ok = enregistrerUtilisateur(user, pass);

            if (ok) {
                Alert a = new Alert(Alert.AlertType.INFORMATION);
                a.setContentText("Compte créé !");
                a.showAndWait();

                montrerLogin(stage);
            }
        });

        Button retour = new Button("Retour");
        styleBouton(retour);
        retour.setOnAction(e -> montrerLogin(stage));

        root.getChildren().addAll(username, password, creer, retour);

        stage.setScene(new Scene(root, 420, 620));
    }

    /*
       =========================
       INSCRIPTION DB CORRIGÉE
       =========================
    */

    private boolean enregistrerUtilisateur(String username, String password) {

        String sql = "INSERT INTO utilisateurs(username, password) VALUES(?, ?)";

        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);
            ps.setString(2, password);

            return ps.executeUpdate() > 0;

        } catch (SQLIntegrityConstraintViolationException e) {
            afficherErreur("Nom déjà utilisé");
            return false;

        } catch (Exception e) {
            e.printStackTrace();
            afficherErreur("Erreur serveur");
            return false;
        }
    }

    /*
       =========================
       UTILISATEUR EXISTE
       =========================
    */

    private boolean utilisateurExiste(String username) {

        String sql = "SELECT id FROM utilisateurs WHERE username=?";

        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);

            ResultSet rs = ps.executeQuery();
            return rs.next();

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /*
       =========================
       LOGIN CHECK
       =========================
    */

    private boolean verifierConnexion(String username, String password) {

        String sql = "SELECT id FROM utilisateurs WHERE username=? AND password=?";

        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);
            ps.setString(2, password);

            return ps.executeQuery().next();

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /*
       =========================
       UTILITAIRES UI
       =========================
    */

    private void styleChamp(Control c) {
        c.setStyle("-fx-background-color:#252b3b; -fx-text-fill:white; -fx-padding:10;");
    }

    private void styleBouton(Button b) {
        b.setStyle("-fx-background-color:#6c63ff; -fx-text-fill:white;");
    }

    private void afficherErreur(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setContentText(msg);
        a.showAndWait();
    }

    /*
       =========================
       CHAT (inchangé simplifié)
       =========================
    */

    private void montrerChat(Stage stage, String user) {
        BorderPane root = new BorderPane();
        root.setCenter(new Label("Chat OK pour " + user));

        stage.setScene(new Scene(root, 900, 600));
    }

    private void chargerUtilisateurs(String monNom) {
        contacts.clear();
    }

    public static void main(String[] args) {
        launch(args);
    }
}