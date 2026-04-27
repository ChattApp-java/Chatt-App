package frontend;

import base_de_donnees.DAOUtilisateur;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import modele.Utilisateur;

public class LoginApp extends VBox {

    private Stage primaryStage;
    private DAOUtilisateur daoUser;
    private boolean isLoginMode = true;

    private TextField txtUsername;
    private PasswordField txtPassword;
    private TextField txtEmail;
    private Button btnSubmit;
    private Button btnToggle;
    private Label lblError;

    public LoginApp(Stage stage) {
        this.primaryStage = stage;
        this.daoUser = new DAOUtilisateur();
        
        this.setAlignment(Pos.CENTER);
        this.setSpacing(20);
        this.setPadding(new Insets(40));
        this.getStyleClass().add("glass-pane");
        this.setMaxWidth(400);
        this.setMaxHeight(500);

        Label lblTitle = new Label("Chatt-App");
        lblTitle.getStyleClass().add("label-header");
        
        Label lblSubtitle = new Label("Connectez-vous pour continuer");
        lblSubtitle.getStyleClass().add("label-subheader");

        txtUsername = new TextField();
        txtUsername.setPromptText("Nom d'utilisateur");
        
        txtPassword = new PasswordField();
        txtPassword.setPromptText("Mot de passe");
        
        txtEmail = new TextField();
        txtEmail.setPromptText("Email");
        txtEmail.setManaged(false);
        txtEmail.setVisible(false);

        lblError = new Label();
        lblError.setStyle("-fx-text-fill: #ef4444;"); // Red color for errors
        lblError.setVisible(false);

        btnSubmit = new Button("Se connecter");
        btnSubmit.getStyleClass().add("button-primary");
        btnSubmit.setMaxWidth(Double.MAX_VALUE);
        btnSubmit.setOnAction(e -> handleSubmit());

        btnToggle = new Button("Pas de compte ? S'inscrire");
        btnToggle.getStyleClass().add("button-secondary");
        btnToggle.setOnAction(e -> toggleMode());

        this.getChildren().addAll(lblTitle, lblSubtitle, txtUsername, txtEmail, txtPassword, lblError, btnSubmit, btnToggle);
    }

    private void toggleMode() {
        isLoginMode = !isLoginMode;
        if (isLoginMode) {
            btnSubmit.setText("Se connecter");
            btnToggle.setText("Pas de compte ? S'inscrire");
            txtEmail.setManaged(false);
            txtEmail.setVisible(false);
        } else {
            btnSubmit.setText("S'inscrire");
            btnToggle.setText("Déjà un compte ? Se connecter");
            txtEmail.setManaged(true);
            txtEmail.setVisible(true);
        }
        lblError.setVisible(false);
    }

    private void handleSubmit() {
        String username = txtUsername.getText().trim();
        String password = txtPassword.getText().trim();
        
        if (username.isEmpty() || password.isEmpty()) {
            showError("Veuillez remplir tous les champs.");
            return;
        }

        if (isLoginMode) {
            Utilisateur user = daoUser.connecter(username, password);
            if (user != null) {
                try {
                    client.ConnexionServeur conn = new client.ConnexionServeur();
                    String status = conn.connecterEtRecupererStatut(username, "127.0.0.1");
                    
                    if (status.equals("OK")) {
                        openMainChat(user, conn);
                    } else {
                        showError("Erreur serveur : " + status);
                    }
                } catch (Exception e) {
                    showError("Impossible de se connecter au serveur (127.0.0.1:5000). Vérifiez qu'il est lancé.");
                    e.printStackTrace();
                }
            } else {
                showError("Identifiants incorrects.");
            }
        } else {
            String email = txtEmail.getText().trim();
            if (email.isEmpty()) {
                showError("Veuillez saisir un email.");
                return;
            }
            if (daoUser.userExiste(username)) {
                showError("Ce nom d'utilisateur existe déjà.");
                return;
            }
            Utilisateur newUser = new Utilisateur(0, username, password, email, false, null);
            if (daoUser.inscrire(newUser)) {
                showError("Inscription réussie. Connectez-vous !");
                lblError.setStyle("-fx-text-fill: #22c55e;"); // Green
                toggleMode();
            } else {
                showError("Erreur lors de l'inscription.");
            }
        }
    }

    private void showError(String msg) {
        lblError.setText(msg);
        lblError.setVisible(true);
    }

    private void openMainChat(Utilisateur user, client.ConnexionServeur conn) {
        MainChatApp chatApp = new MainChatApp(primaryStage, user, conn);
        
        // Démarrer l'écouteur de messages
        client.EcouteurMessages ecouteur = new client.EcouteurMessages(conn, chatApp);
        Thread t = new Thread(ecouteur);
        t.setDaemon(true);
        t.start();

        Scene scene = new Scene(chatApp, 1000, 700);
        scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
        primaryStage.setScene(scene);
        primaryStage.centerOnScreen();
    }
}
