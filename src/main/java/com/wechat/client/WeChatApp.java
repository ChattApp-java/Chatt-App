package com.wechat.client;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.util.Arrays;
import java.util.List;

/**
 * WeChatApp - Application principale JavaFX.
 * Structure : Rail (68px) | Sidebar (285px) | Zone centrale (flexible)
 */
public class WeChatApp extends Application {

    // ═══ PALETTE WECHAT ═══
    private static final String COLOR_BG_MAIN      = "#F5F5F5";
    private static final String COLOR_BG_RAIL      = "#2E2E2E";
    private static final String COLOR_BG_SIDEBAR   = "#FFFFFF";
    private static final String COLOR_BG_CHAT      = "#F5F5F5";
    private static final String COLOR_ACCENT       = "#07C160";  // Vert WeChat
    private static final String COLOR_ACCENT_HOVER = "#06AD56";
    private static final String COLOR_TEXT_PRIMARY = "#000000";
    private static final String COLOR_TEXT_SECOND  = "#999999";
    private static final String COLOR_BORDER       = "#E5E5E5";
    private static final String COLOR_RAIL_ICON    = "#888888";
    private static final String COLOR_RAIL_ACTIVE  = "#07C160";

    // ═══ DIMENSIONS ═══
    private static final double RAIL_WIDTH    = 68;
    private static final double SIDEBAR_WIDTH = 285;
    private static final double WINDOW_WIDTH  = 1100;
    private static final double WINDOW_HEIGHT = 750;

    // ═══ ÉTAT ═══
    private VBox railMessages, railGroups, railCalls;
    private VBox sidebarContent;
    private StackPane centerContent;
    private Label currentSectionLabel;
    private String currentSection = "messages";

    @Override
    public void start(Stage primaryStage) {
        // ─── Layout principal ───
        HBox root = new HBox();
        root.setPrefSize(WINDOW_WIDTH, WINDOW_HEIGHT);
        root.setMinSize(900, 600);

        // ─── 1. RAIL GAUCHE (68px) ───
        VBox rail = createRail();

        // ─── 2. SIDEBAR (285px) ───
        VBox sidebar = createSidebar();

        // ─── 3. ZONE CENTRALE ───
        StackPane center = createCenter();

        // Assembler
        root.getChildren().addAll(rail, sidebar, center);
        HBox.setHgrow(center, Priority.ALWAYS);

        // ─── Scene ───
        Scene scene = new Scene(root);
        scene.setFill(Color.web(COLOR_BG_MAIN));

        primaryStage.setTitle("WeChat");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(800);
        primaryStage.setMinHeight(500);
        primaryStage.show();

        // Charger les conversations mock
        loadMockConversations();
    }

    // ═══════════════════════════════════════════════════════════
    // RAIL GAUCHE
    // ═══════════════════════════════════════════════════════════

    private VBox createRail() {
        VBox rail = new VBox();
        rail.setPrefWidth(RAIL_WIDTH);
        rail.setMinWidth(RAIL_WIDTH);
        rail.setMaxWidth(RAIL_WIDTH);
        rail.setStyle("-fx-background-color: " + COLOR_BG_RAIL + ";");
        rail.setAlignment(Pos.TOP_CENTER);
        rail.setPadding(new Insets(12, 0, 12, 0));
        rail.setSpacing(8);

        // Logo WeChat (cercle vert)
        StackPane logo = createRailIcon("💬", COLOR_ACCENT, true);
        logo.setStyle("-fx-background-color: " + COLOR_ACCENT + "; -fx-background-radius: 8;");
        logo.setPrefSize(44, 44);
        Label logoLabel = new Label("💬");
        logoLabel.setFont(Font.font("Segoe UI Emoji", 22));
        logo.getChildren().setAll(logoLabel);
        VBox.setMargin(logo, new Insets(0, 0, 16, 0));

        // Section : Messages
        railMessages = createRailNavItem("💬", "Messages", true);
        railMessages.setOnMouseClicked(e -> switchSection("messages"));

        // Section : Groupes
        railGroups = createRailNavItem("👥", "Groupes", false);
        railGroups.setOnMouseClicked(e -> switchSection("groups"));

        // Section : Appels
        railCalls = createRailNavItem("📞", "Appels", false);
        railCalls.setOnMouseClicked(e -> switchSection("calls"));

        // Spacer
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        // Profil
        VBox railProfile = createRailNavItem("👤", "Profil", false);
        railProfile.setOnMouseClicked(e -> switchSection("profile"));

        // Paramètres
        VBox railSettings = createRailNavItem("⚙️", "Paramètres", false);
        railSettings.setOnMouseClicked(e -> switchSection("settings"));

        rail.getChildren().addAll(
                logo,
                railMessages,
                railGroups,
                railCalls,
                spacer,
                railProfile,
                railSettings
        );

        return rail;
    }

    private VBox createRailNavItem(String emoji, String tooltip, boolean active) {
        VBox item = new VBox();
        item.setAlignment(Pos.CENTER);
        item.setPrefSize(RAIL_WIDTH, 52);
        item.setCursor(javafx.scene.Cursor.HAND);

        Label icon = new Label(emoji);
        icon.setFont(Font.font("Segoe UI Emoji", 20));
        icon.setTextFill(Color.web(active ? COLOR_RAIL_ACTIVE : COLOR_RAIL_ICON));

        // Indicateur actif (barre verte à gauche)
        if (active) {
            item.setStyle("-fx-border-width: 0 0 0 3; -fx-border-color: " + COLOR_ACCENT + ";");
        }

        // Hover effect
        item.setOnMouseEntered(e -> {
            if (!isActiveSection(item)) {
                item.setStyle("-fx-background-color: #3E3E3E;");
            }
        });
        item.setOnMouseExited(e -> {
            if (!isActiveSection(item)) {
                item.setStyle("-fx-background-color: transparent;");
            }
        });

        item.getChildren().add(icon);
        return item;
    }

    private boolean isActiveSection(VBox item) {
        return (currentSection.equals("messages") && item == railMessages) ||
                (currentSection.equals("groups") && item == railGroups) ||
                (currentSection.equals("calls") && item == railCalls);
    }

    private StackPane createRailIcon(String emoji, String color, boolean isLogo) {
        StackPane pane = new StackPane();
        pane.setAlignment(Pos.CENTER);
        return pane;
    }

    // ═══════════════════════════════════════════════════════════
    // SIDEBAR
    // ═══════════════════════════════════════════════════════════

    private VBox createSidebar() {
        VBox sidebar = new VBox();
        sidebar.setPrefWidth(SIDEBAR_WIDTH);
        sidebar.setMinWidth(SIDEBAR_WIDTH);
        sidebar.setMaxWidth(SIDEBAR_WIDTH);
        sidebar.setStyle("-fx-background-color: " + COLOR_BG_SIDEBAR + ";" +
                "-fx-border-width: 0 1 0 0; -fx-border-color: " + COLOR_BORDER + ";");

        // ─── En-tête avec titre ───
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(16, 16, 12, 16));
        header.setSpacing(12);

        currentSectionLabel = new Label("Messages");
        currentSectionLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 20));
        currentSectionLabel.setTextFill(Color.web(COLOR_TEXT_PRIMARY));

        // Bouton nouveau message
        Button newMsgBtn = new Button("✏️");
        newMsgBtn.setFont(Font.font("Segoe UI Emoji", 14));
        newMsgBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");
        newMsgBtn.setOnAction(e -> showNewMessageDialog());

        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);

        header.getChildren().addAll(currentSectionLabel, headerSpacer, newMsgBtn);

        // ─── Barre de recherche ───
        HBox searchBox = new HBox();
        searchBox.setAlignment(Pos.CENTER_LEFT);
        searchBox.setPadding(new Insets(0, 16, 12, 16));
        searchBox.setSpacing(8);
        searchBox.setStyle("-fx-background-color: " + COLOR_BG_MAIN + ";" +
                "-fx-background-radius: 6;");
        searchBox.setPadding(new Insets(8, 12, 8, 12));
        HBox.setMargin(searchBox, new Insets(0, 16, 12, 16));

        Label searchIcon = new Label("🔍");
        searchIcon.setFont(Font.font("Segoe UI Emoji", 12));
        searchIcon.setTextFill(Color.web(COLOR_TEXT_SECOND));

        TextField searchField = new TextField();
        searchField.setPromptText("Rechercher...");
        searchField.setStyle("-fx-background-color: transparent; -fx-border-width: 0;");
        searchField.setFont(Font.font("Segoe UI", 13));
        HBox.setHgrow(searchField, Priority.ALWAYS);

        searchBox.getChildren().addAll(searchIcon, searchField);

        // ─── Liste des conversations ───
        sidebarContent = new VBox();
        sidebarContent.setSpacing(0);
        VBox.setVgrow(sidebarContent, Priority.ALWAYS);

        // ScrollPane pour la liste
        ScrollPane scrollPane = new ScrollPane(sidebarContent);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setStyle("-fx-background: " + COLOR_BG_SIDEBAR + "; -fx-background-color: transparent;");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        sidebar.getChildren().addAll(header, searchBox, scrollPane);
        return sidebar;
    }

    // ═══════════════════════════════════════════════════════════
    // ZONE CENTRALE
    // ═══════════════════════════════════════════════════════════

    private StackPane createCenter() {
        centerContent = new StackPane();
        centerContent.setStyle("-fx-background-color: " + COLOR_BG_CHAT + ";");
        centerContent.setAlignment(Pos.CENTER);

        // Message par défaut
        VBox emptyState = new VBox();
        emptyState.setAlignment(Pos.CENTER);
        emptyState.setSpacing(16);

        Label icon = new Label("💬");
        icon.setFont(Font.font("Segoe UI Emoji", 64));
        icon.setTextFill(Color.web("#CCCCCC"));

        Label title = new Label("Sélectionnez une conversation");
        title.setFont(Font.font("Segoe UI", 18));
        title.setTextFill(Color.web(COLOR_TEXT_SECOND));

        Label subtitle = new Label("Choisissez un contact dans la liste pour commencer à discuter");
        subtitle.setFont(Font.font("Segoe UI", 13));
        subtitle.setTextFill(Color.web("#BBBBBB"));

        emptyState.getChildren().addAll(icon, title, subtitle);
        centerContent.getChildren().add(emptyState);

        return centerContent;
    }

    // ═══════════════════════════════════════════════════════════
    // NAVIGATION
    // ═══════════════════════════════════════════════════════════

    private void switchSection(String section) {
        currentSection = section;

        // Reset tous les rails
        resetRailStyle(railMessages);
        resetRailStyle(railGroups);
        resetRailStyle(railCalls);

        // Activer le rail sélectionné
        VBox activeRail = switch (section) {
            case "messages" -> railMessages;
            case "groups" -> railGroups;
            case "calls" -> railCalls;
            default -> railMessages;
        };
        activeRail.setStyle("-fx-border-width: 0 0 0 3; -fx-border-color: " + COLOR_ACCENT + ";");
        ((Label) activeRail.getChildren().get(0)).setTextFill(Color.web(COLOR_RAIL_ACTIVE));

        // Mettre à jour le titre sidebar
        String title = switch (section) {
            case "messages" -> "Messages";
            case "groups" -> "Groupes";
            case "calls" -> "Appels";
            case "profile" -> "Profil";
            case "settings" -> "Paramètres";
            default -> "Messages";
        };
        currentSectionLabel.setText(title);

        // Charger le contenu approprié
        sidebarContent.getChildren().clear();
        switch (section) {
            case "messages" -> loadMockConversations();
            case "groups" -> loadMockGroups();
            case "calls" -> loadMockCalls();
            default -> loadMockConversations();
        }
    }

    private void resetRailStyle(VBox rail) {
        rail.setStyle("-fx-background-color: transparent;");
        if (!rail.getChildren().isEmpty()) {
            ((Label) rail.getChildren().get(0)).setTextFill(Color.web(COLOR_RAIL_ICON));
        }
    }

    // ═══════════════════════════════════════════════════════════
    // MOCK DATA - Conversations
    // ═══════════════════════════════════════════════════════════

    private void loadMockConversations() {
        List<MockConversation> conversations = Arrays.asList(
                new MockConversation("Alice Martin", "Salut ! Tu viens ce soir ?", "14:32", 2, true, "🟢"),
                new MockConversation("Bob Dupont", "J'ai envoyé le fichier", "13:15", 0, false, "🟢"),
                new MockConversation("Équipe Projet", "Sarah: D'accord pour 15h", "12:48", 5, true, "👥"),
                new MockConversation("Charlie", "👍", "Hier", 0, false, "⚫"),
                new MockConversation("Famille", "Maman: À table !", "Hier", 1, true, "👥"),
                new MockConversation("David L.", "Merci beaucoup !", "Lun", 0, false, "⚫"),
                new MockConversation("Support WeChat", "Votre ticket a été résolu", "Lun", 0, false, "🤖")
        );

        for (MockConversation conv : conversations) {
            sidebarContent.getChildren().add(createConversationItem(conv));
        }
    }

    private void loadMockGroups() {
        List<MockConversation> groups = Arrays.asList(
                new MockConversation("Équipe Projet", "12 membres", "Actif", 0, true, "👥"),
                new MockConversation("Famille", "5 membres", "Actif", 0, true, "👥"),
                new MockConversation("Les Amis", "8 membres", "Actif", 0, true, "👥"),
                new MockConversation("Dév Web", "45 membres", "Actif", 0, true, "👥")
        );

        for (MockConversation group : groups) {
            sidebarContent.getChildren().add(createConversationItem(group));
        }
    }

    private void loadMockCalls() {
        List<MockConversation> calls = Arrays.asList(
                new MockConversation("Alice Martin", "Appel vocal · 5 min", "14:32", 0, false, "📞"),
                new MockConversation("Bob Dupont", "Appel manqué", "13:15", 0, true, "📵"),
                new MockConversation("Équipe Projet", "Appel groupe · 45 min", "Hier", 0, false, "👥")
        );

        for (MockConversation call : calls) {
            sidebarContent.getChildren().add(createConversationItem(call));
        }
    }

    private HBox createConversationItem(MockConversation conv) {
        HBox item = new HBox();
        item.setAlignment(Pos.CENTER_LEFT);
        item.setPadding(new Insets(12, 16, 12, 16));
        item.setSpacing(12);
        item.setCursor(javafx.scene.Cursor.HAND);
        item.setStyle("-fx-background-color: " + COLOR_BG_SIDEBAR + ";");

        // Avatar
        StackPane avatar = new StackPane();
        avatar.setPrefSize(48, 48);
        avatar.setMinSize(48, 48);
        avatar.setMaxSize(48, 48);

        Circle circle = new Circle(24);
        circle.setFill(Color.web(conv.isGroup() ? "#07C160" : "#E0E0E0"));

        Label avatarLabel = new Label(conv.avatar());
        avatarLabel.setFont(Font.font("Segoe UI Emoji", 22));

        avatar.getChildren().addAll(circle, avatarLabel);

        // Info
        VBox info = new VBox();
        info.setSpacing(4);
        HBox.setHgrow(info, Priority.ALWAYS);

        HBox nameRow = new HBox();
        nameRow.setAlignment(Pos.CENTER_LEFT);
        nameRow.setSpacing(8);

        Label name = new Label(conv.name());
        name.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        name.setTextFill(Color.web(COLOR_TEXT_PRIMARY));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label time = new Label(conv.time());
        time.setFont(Font.font("Segoe UI", 11));
        time.setTextFill(Color.web(COLOR_TEXT_SECOND));

        nameRow.getChildren().addAll(name, spacer, time);

        HBox msgRow = new HBox();
        msgRow.setAlignment(Pos.CENTER_LEFT);
        msgRow.setSpacing(8);

        Label preview = new Label(conv.preview());
        preview.setFont(Font.font("Segoe UI", 12));
        preview.setTextFill(Color.web(conv.isUnread() ? COLOR_TEXT_PRIMARY : COLOR_TEXT_SECOND));

        Region msgSpacer = new Region();
        HBox.setHgrow(msgSpacer, Priority.ALWAYS);

        msgRow.getChildren().addAll(preview, msgSpacer);

        // Badge de notification
        if (conv.unreadCount() > 0) {
            Label badge = new Label(String.valueOf(conv.unreadCount()));
            badge.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
            badge.setTextFill(Color.WHITE);
            badge.setAlignment(Pos.CENTER);
            badge.setPrefSize(20, 20);
            badge.setMinSize(20, 20);
            badge.setStyle("-fx-background-color: " + COLOR_ACCENT + "; -fx-background-radius: 10;");
            msgRow.getChildren().add(badge);
        }

        info.getChildren().addAll(nameRow, msgRow);
        item.getChildren().addAll(avatar, info);

        // Hover
        item.setOnMouseEntered(e -> item.setStyle("-fx-background-color: #F0F0F0;"));
        item.setOnMouseExited(e -> item.setStyle("-fx-background-color: " + COLOR_BG_SIDEBAR + ";"));

        // Click
        item.setOnMouseClicked(e -> openConversation(conv));

        return item;
    }

    private void openConversation(MockConversation conv) {
        centerContent.getChildren().clear();

        // Zone de chat (simplifiée pour l'instant)
        VBox chatArea = new VBox();
        chatArea.setAlignment(Pos.CENTER);
        chatArea.setSpacing(16);

        Label avatar = new Label(conv.avatar());
        avatar.setFont(Font.font("Segoe UI Emoji", 48));

        Label name = new Label(conv.name());
        name.setFont(Font.font("Segoe UI", FontWeight.BOLD, 20));
        name.setTextFill(Color.web(COLOR_TEXT_PRIMARY));

        Label status = new Label("Cette conversation s'affichera ici");
        status.setFont(Font.font("Segoe UI", 13));
        status.setTextFill(Color.web(COLOR_TEXT_SECOND));

        chatArea.getChildren().addAll(avatar, name, status);
        centerContent.getChildren().add(chatArea);
    }

    private void showNewMessageDialog() {
        // TODO: Ouvrir dialog de nouveau message
        System.out.println("Nouveau message...");
    }

    // ═══════════════════════════════════════════════════════════
    // MAIN
    // ═══════════════════════════════════════════════════════════

    public static void main(String[] args) {
        launch(args);
    }

    // ═══════════════════════════════════════════════════════════
    // RECORD - Mock Conversation
    // ═══════════════════════════════════════════════════════════

    private record MockConversation(String name, String preview, String time,
                                    int unreadCount, boolean isUnread, String avatar) {
        boolean isGroup() {
            return avatar.equals("👥");
        }
    }
}