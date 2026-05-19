package org.example.tpchatjavafx.client.controller;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.geometry.HPos;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;
import org.example.tpchatjavafx.client.NetworkClient;
import org.example.tpchatjavafx.common.MessageType;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MeetingController implements Initializable {

    @FXML private Label lblTitle;
    @FXML private Label lblSubtitle;
    @FXML private Label lblVideoCaption;
    @FXML private TilePane centerContainer;
    @FXML private GridPane videoGrid;
    @FXML private ListView<String> participantsList;
    @FXML private Button btnMore;
    @FXML private Button btnVideo;
    @FXML private Button btnSpeaker;
    @FXML private Button btnMic;
    @FXML private Button btnEndCall;
    @FXML private Button btnSwitchCamera;
    @FXML private Button btnFlashlight;
    @FXML private Button btnAddParticipant;
    @FXML private VBox bottomControls;

    private NetworkClient networkClient;
    private String username;
    private int groupId;
    private int meetingId;
    private int localParticipantId = -1;
    private String callType = "AUDIO";
    private boolean isInitiator;

    private boolean micMuted = false;
    private boolean speakerOn = true;
    private boolean videoOn = true;

    private com.github.sarxos.webcam.Webcam webcam;
    private Thread videoThread;

    private final Map<Integer, VBox> participantContainers = new ConcurrentHashMap<>();
    private final Map<Integer, StackPane> participantVideoPanes = new ConcurrentHashMap<>();
    private final Map<Integer, ImageView> participantVideos = new ConcurrentHashMap<>();
    private final Map<Integer, Label> participantVideoPlaceholders = new ConcurrentHashMap<>();
    private final Map<Integer, Label> participantLabels = new ConcurrentHashMap<>();
    private final Map<Integer, Image> pendingParticipantFrames = new ConcurrentHashMap<>();
    private final Set<Integer> participantIds = ConcurrentHashMap.newKeySet();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println("[MEETING_UI] MeetingController initialise");
        if (participantsList != null) {
            participantsList.setItems(FXCollections.observableArrayList());
        }
        if (centerContainer != null) {
            centerContainer.setPrefColumns(2);
            centerContainer.setHgap(18);
            centerContainer.setVgap(18);
            centerContainer.setTileAlignment(Pos.CENTER);
            centerContainer.setAlignment(Pos.TOP_CENTER);
        }
        setupButtonActions();
    }

    private void setupButtonActions() {
        if (btnMore != null) btnMore.setOnAction(e -> showMoreOptions());
        if (btnVideo != null) btnVideo.setOnAction(e -> toggleVideo());
        if (btnSpeaker != null) btnSpeaker.setOnAction(e -> toggleSpeaker());
        if (btnMic != null) btnMic.setOnAction(e -> toggleMic());
        if (btnEndCall != null) btnEndCall.setOnAction(e -> endCall());
        if (btnSwitchCamera != null) btnSwitchCamera.setOnAction(e -> switchCamera());
        if (btnFlashlight != null) btnFlashlight.setOnAction(e -> toggleFlashlight());
        if (btnAddParticipant != null) btnAddParticipant.setOnAction(e -> handleAddParticipant());
    }

    public void setNetworkClient(NetworkClient client) {
        this.networkClient = client;
        if (client != null) {
            client.setActiveMeetingController(this);
            client.setOnMeetingNonParticipantsResponse(msg ->
                    Platform.runLater(() -> showAddParticipantDialog(msg.getContent())));
        }
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setGroupId(int groupId) {
        this.groupId = groupId;
        Platform.runLater(this::updateSubtitle);
    }

    public void setMeetingId(int meetingId) {
        this.meetingId = meetingId;
    }

    public void setInitiator(boolean initiator) {
        this.isInitiator = initiator;
    }

    public void setCallType(String callType) {
        this.callType = normalizeCallType(callType);
        System.out.println("[MEETING_UI] setCallType: " + this.callType);
        Platform.runLater(() -> {
            boolean isVideo = isVideoMeeting();
            if (lblTitle != null) {
                lblTitle.setText(isVideo ? "Appel video de groupe" : "Appel audio de groupe");
            }
            if (btnVideo != null) {
                btnVideo.setVisible(isVideo);
                btnVideo.setManaged(isVideo);
            }
            if (btnSwitchCamera != null) {
                btnSwitchCamera.setVisible(isVideo);
                btnSwitchCamera.setManaged(isVideo);
            }
            if (btnFlashlight != null) {
                btnFlashlight.setVisible(isVideo);
                btnFlashlight.setManaged(isVideo);
            }
            if (videoGrid != null) {
                videoGrid.setVisible(isVideo);
                videoGrid.setManaged(isVideo);
            }
            if (lblVideoCaption != null) {
                lblVideoCaption.setVisible(isVideo);
                lblVideoCaption.setManaged(isVideo);
            }
            if (centerContainer != null) {
                centerContainer.setVisible(!isVideo);
                centerContainer.setManaged(!isVideo);
            }
            updateSubtitle();
            if (isVideo && networkClient != null && (webcam == null || !webcam.isOpen())) {
                startVideoCapture();
            }
        });
    }

    public void startServices() {
        System.out.println("[MEETING_UI] startServices() callType=" + callType);
        if (isVideoMeeting()) {
            startVideoCapture();
        }
    }

    public void startVideoCapture() {
        if (!isVideoMeeting()) {
            System.out.println("[MEETING_UI] Pas VIDEO, pas de webcam");
            return;
        }
        if (videoThread != null && videoThread.isAlive() && webcam != null && webcam.isOpen()) {
            System.out.println("[MEETING_UI] Webcam deja active");
            return;
        }
        try {
            if (webcam != null && webcam.isOpen()) {
                webcam.close();
                try {
                    Thread.sleep(300);
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread().interrupt();
                }
            }
            webcam = com.github.sarxos.webcam.Webcam.getDefault();
            if (webcam == null) {
                System.err.println("[MEETING_UI] AUCUNE WEBCAM TROUVEE");
                return;
            }
            System.out.println("[MEETING_UI] Webcam: " + webcam.getName());
            webcam.setViewSize(com.github.sarxos.webcam.WebcamResolution.QVGA.getSize());
            webcam.open();
            videoOn = true;

            videoThread = new Thread(() -> {
                while (videoOn && webcam != null && webcam.isOpen()) {
                    try {
                        BufferedImage frame = webcam.getImage();
                        if (frame != null) {
                            Image fxImage = javafx.embed.swing.SwingFXUtils.toFXImage(frame, null);
                            int localVideoTargetId = resolveLocalVideoTargetId();
                            Platform.runLater(() -> updateParticipantVideo(localVideoTargetId, fxImage));
                            if (networkClient != null && meetingId > 0) {
                                byte[] encodedFrame = encodeMeetingFrame(frame);
                                if (encodedFrame != null && encodedFrame.length > 0) {
                                    System.out.println("[MEETING_UI] Envoi frame video meeting=" + meetingId
                                            + " taille=" + encodedFrame.length + " octets");
                                    networkClient.sendVideoFrame(encodedFrame, meetingId);
                                }
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("[MEETING_UI] Erreur frame: " + e.getMessage());
                    }
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException ignored) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }, "MeetingWebcam");
            videoThread.setDaemon(true);
            videoThread.start();
            System.out.println("[MEETING_UI] Capture video demarree");
        } catch (Exception e) {
            System.err.println("[MEETING_UI] ERREUR webcam: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void toggleMic() {
        micMuted = !micMuted;
        updateMicButton();
        if (networkClient != null && meetingId > 0) {
            networkClient.sendMicState(meetingId, !micMuted);
            networkClient.setMeetingMicEnabled(!micMuted);
        }
    }

    private void updateMicButton() {
        Platform.runLater(() -> {
            if (btnMic == null) return;
            FontIcon icon = new FontIcon(micMuted ? FontAwesomeSolid.MICROPHONE_SLASH : FontAwesomeSolid.MICROPHONE);
            icon.setIconSize(24);
            icon.setIconColor(micMuted ? Color.web("#ff1744") : Color.WHITE);
            btnMic.setGraphic(icon);
            btnMic.setStyle(micMuted
                    ? "-fx-background-color: #ff5c73; -fx-background-radius: 28; -fx-cursor: hand;"
                    : "-fx-background-color: #253239; -fx-background-radius: 28; -fx-cursor: hand;");
        });
    }

    private void toggleSpeaker() {
        speakerOn = !speakerOn;
        if (networkClient != null) {
            networkClient.setMeetingPlaybackVolume(speakerOn ? 1.0 : 0.0);
        }
        updateSpeakerButton();
    }

    private void updateSpeakerButton() {
        Platform.runLater(() -> {
            if (btnSpeaker == null) return;
            FontIcon icon = new FontIcon(speakerOn ? FontAwesomeSolid.VOLUME_UP : FontAwesomeSolid.VOLUME_MUTE);
            icon.setIconSize(24);
            icon.setIconColor(speakerOn ? Color.web("#01579b") : Color.web("#ff1744"));
            btnSpeaker.setGraphic(icon);
            btnSpeaker.setStyle(speakerOn
                    ? "-fx-background-color: #ffffff; -fx-background-radius: 32; -fx-cursor: hand;"
                    : "-fx-background-color: #253239; -fx-background-radius: 32; -fx-cursor: hand;");
        });
    }

    private void toggleVideo() {
        if (!isVideoMeeting()) return;
        videoOn = !videoOn;
        updateVideoButton();
        if (!videoOn && webcam != null && webcam.isOpen()) {
            webcam.close();
        } else if (videoOn && (webcam == null || !webcam.isOpen())) {
            startVideoCapture();
        }
        if (networkClient != null && meetingId > 0) {
            networkClient.sendVideoState(meetingId, videoOn);
        }
    }

    private void updateVideoButton() {
        Platform.runLater(() -> {
            if (btnVideo == null) return;
            FontIcon icon = new FontIcon(videoOn ? FontAwesomeSolid.VIDEO : FontAwesomeSolid.VIDEO_SLASH);
            icon.setIconSize(24);
            icon.setIconColor(videoOn ? Color.WHITE : Color.web("#ff1744"));
            btnVideo.setGraphic(icon);
            btnVideo.setStyle(videoOn
                    ? "-fx-background-color: #253239; -fx-background-radius: 28; -fx-cursor: hand;"
                    : "-fx-background-color: #ff5c73; -fx-background-radius: 28; -fx-cursor: hand;");
        });
    }

    private void switchCamera() {
        System.out.println("[MEETING_UI] Switch camera");
    }

    private void toggleFlashlight() {
        System.out.println("[MEETING_UI] Flashlight");
    }

    private void showMoreOptions() {
        ContextMenu menu = new ContextMenu();
        menu.setStyle("-fx-background-color: white; -fx-background-radius: 8;");

        MenuItem itemParticipants = new MenuItem("Participants");
        itemParticipants.setOnAction(e -> showParticipantsPanel());

        MenuItem itemInfo = new MenuItem("Infos de la reunion");
        itemInfo.setOnAction(e -> showMeetingInfo());

        menu.getItems().addAll(itemParticipants, new SeparatorMenuItem(), itemInfo);
        menu.show(btnMore, javafx.geometry.Side.TOP, 0, -5);
    }

    private void showParticipantsPanel() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Participants");
        int count = participantsList != null ? participantsList.getItems().size() : 0;
        dialog.setHeaderText("Participants (" + count + ")");
        ListView<String> list = new ListView<>();
        if (participantsList != null) {
            list.setItems(FXCollections.observableArrayList(participantsList.getItems()));
        }
        list.setPrefHeight(300);
        dialog.getDialogPane().setContent(list);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.showAndWait();
    }

    private void showMeetingInfo() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Infos");
        alert.setHeaderText("Reunion #" + meetingId);
        int count = participantsList != null ? participantsList.getItems().size() : 0;
        alert.setContentText("Type: " + callType + "\nGroupe: #" + groupId + "\nParticipants: " + count);
        alert.showAndWait();
    }

    private void handleAddParticipant() {
        if (networkClient == null || meetingId <= 0) return;
        org.example.tpchatjavafx.client.model.ChatMessage req = new org.example.tpchatjavafx.client.model.ChatMessage();
        req.setType(MessageType.MEETING_NON_PARTICIPANTS_REQUEST);
        req.setGroupId(groupId);
        req.setMeetingId(meetingId);
        networkClient.send(req);
    }

    public void showAddParticipantDialog(String membersCsv) {
        List<String> available = new ArrayList<>();
        Set<String> alreadyIn = new HashSet<>();
        if (participantsList != null) {
            alreadyIn.addAll(participantsList.getItems());
        }
        if (membersCsv != null && !membersCsv.isBlank()) {
            for (String m : membersCsv.split(",")) {
                String name = m.trim();
                if (!name.isEmpty() && !alreadyIn.contains(name)) {
                    available.add(name);
                }
            }
        }
        if (available.isEmpty()) {
            new Alert(Alert.AlertType.INFORMATION, "Tous les membres sont deja dans la reunion.").showAndWait();
            return;
        }

        Dialog<List<String>> dialog = new Dialog<>();
        dialog.setTitle("Ajouter participants");
        dialog.setHeaderText("Selectionnez:");
        VBox content = new VBox(10);
        content.setPadding(new Insets(20));
        List<CheckBox> cbs = new ArrayList<>();
        for (String m : available) {
            CheckBox cb = new CheckBox(m);
            cb.setStyle("-fx-text-fill: #01579b;");
            cbs.add(cb);
            content.getChildren().add(cb);
        }
        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setPrefHeight(300);
        dialog.getDialogPane().setContent(scroll);
        dialog.getDialogPane().getButtonTypes().addAll(
                new ButtonType("Inviter", ButtonBar.ButtonData.OK_DONE),
                ButtonType.CANCEL
        );
        dialog.setResultConverter(btn -> {
            if (btn != null && btn.getButtonData() == ButtonBar.ButtonData.OK_DONE) {
                List<String> sel = new ArrayList<>();
                for (CheckBox cb : cbs) {
                    if (cb.isSelected()) sel.add(cb.getText());
                }
                return sel;
            }
            return null;
        });
        Optional<List<String>> res = dialog.showAndWait();
        res.ifPresent(sel -> {
            for (String name : sel) {
                org.example.tpchatjavafx.client.model.ChatMessage inv = new org.example.tpchatjavafx.client.model.ChatMessage();
                inv.setType(MessageType.MEETING_INVITE_USER);
                inv.setGroupId(groupId);
                inv.setMeetingId(meetingId);
                inv.setContent(name);
                networkClient.send(inv);
            }
        });
    }

    public void addParticipant(int userId, String name, String avatarUrl) {
        if (participantIds.contains(userId)) return;
        if (name != null && name.equals(username) && userId > 0) {
            localParticipantId = userId;
        }
        if (name != null && name.equals(username) && participantIds.contains(-1) && userId != -1) {
            Image localFrame = null;
            ImageView localView = participantVideos.get(-1);
            if (localView != null) {
                localFrame = localView.getImage();
            }
            removeParticipant(-1);
            if (localFrame != null) {
                pendingParticipantFrames.put(userId, localFrame);
            }
        }
        participantIds.add(userId);

        Platform.runLater(() -> {
            if (participantsList != null && name != null && !participantsList.getItems().contains(name)) {
                participantsList.getItems().add(name);
            }
            VBox container = createParticipantContainer(userId, name);
            participantContainers.put(userId, container);
            Image pendingFrame = pendingParticipantFrames.remove(userId);
            if (pendingFrame != null) {
                ImageView view = participantVideos.get(userId);
                if (view != null) {
                    view.setImage(pendingFrame);
                }
            }

            if ("VIDEO".equals(callType) && videoGrid != null) {
                if (centerContainer != null) {
                    centerContainer.setVisible(false);
                    centerContainer.setManaged(false);
                }
                rebuildVideoGrid();
            } else if (centerContainer != null) {
                centerContainer.getChildren().add(container);
            }
            updateSubtitle();
        });
    }

    private boolean isVideoMeeting() {
        return "VIDEO".equalsIgnoreCase(normalizeCallType(callType));
    }

    private String normalizeCallType(String value) {
        if (value == null || value.isBlank()) {
            return "AUDIO";
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private byte[] encodeMeetingFrame(BufferedImage sourceFrame) throws IOException {
        if (sourceFrame == null) {
            return null;
        }

        BufferedImage frameToSend = sourceFrame;
        if (sourceFrame.getWidth() > 320 || sourceFrame.getHeight() > 240) {
            frameToSend = new BufferedImage(320, 240, BufferedImage.TYPE_3BYTE_BGR);
            Graphics2D graphics = frameToSend.createGraphics();
            try {
                graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                graphics.drawImage(sourceFrame, 0, 0, 320, 240, null);
            } finally {
                graphics.dispose();
            }
        }

        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
        if (!writers.hasNext()) {
            ByteArrayOutputStream fallback = new ByteArrayOutputStream();
            ImageIO.write(frameToSend, "jpg", fallback);
            return fallback.toByteArray();
        }

        ImageWriter writer = writers.next();
        ByteArrayOutputStream outputBytes = new ByteArrayOutputStream();
        try (MemoryCacheImageOutputStream output = new MemoryCacheImageOutputStream(outputBytes)) {
            writer.setOutput(output);
            ImageWriteParam params = writer.getDefaultWriteParam();
            if (params.canWriteCompressed()) {
                params.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                params.setCompressionQuality(0.45f);
            }
            writer.write(null, new javax.imageio.IIOImage(frameToSend, null, null), params);
        } finally {
            writer.dispose();
        }
        return outputBytes.toByteArray();
    }

    private VBox createParticipantContainer(int userId, String name) {
        boolean videoMeeting = isVideoMeeting();
        boolean localParticipant = userId == localParticipantId || (userId == -1 && localParticipantId <= 0);
        double tileWidth = videoMeeting ? (localParticipant ? 148 : 248) : 240;
        double mediaSize = videoMeeting ? (localParticipant ? 120 : 220) : 96;

        VBox container = new VBox(10);
        container.setAlignment(Pos.CENTER);
        container.setPadding(new Insets(14));
        container.setPrefWidth(tileWidth);
        container.setMaxWidth(tileWidth);
        container.setStyle(videoMeeting
                ? "-fx-background-color: rgba(255,255,255,0.18); -fx-background-radius: 26; "
                + "-fx-border-color: rgba(255,255,255,0.22); -fx-border-radius: 26; -fx-border-width: 1;"
                : "-fx-background-color: white; -fx-background-radius: 26; "
                + "-fx-border-color: rgba(10, 88, 202, 0.10); -fx-border-radius: 26; -fx-border-width: 1;");

        StackPane videoPane = new StackPane();
        videoPane.setPrefSize(mediaSize, mediaSize);
        videoPane.setMaxSize(mediaSize, mediaSize);
        videoPane.setStyle(videoMeeting
                ? "-fx-background-color: rgba(13, 27, 42, 0.82); -fx-background-radius: 24;"
                : "-fx-background-color: linear-gradient(to bottom right, #5ab2ff, #1976d2); -fx-background-radius: 48;");

        ImageView videoView = new ImageView();
        videoView.setFitWidth(mediaSize);
        videoView.setFitHeight(mediaSize);
        videoView.setPreserveRatio(true);

        Label videoPlaceholder = null;
        if (videoMeeting) {
            videoPlaceholder = new Label(userId == -1 ? "Camera active" : "Video en attente");
            videoPlaceholder.setStyle("-fx-text-fill: #edf4ff; -fx-font-size: 12; -fx-font-weight: bold;");
            videoPlaceholder.setWrapText(true);
            videoPlaceholder.setMaxWidth(110);
            videoPane.getChildren().addAll(videoView, videoPlaceholder);
        } else {
            Label initial = new Label(buildParticipantInitial(name));
            initial.setStyle("-fx-text-fill: white; -fx-font-size: 34; -fx-font-weight: bold;");
            videoPane.getChildren().add(initial);
        }

        Rectangle clip = new Rectangle(mediaSize, mediaSize);
        clip.setArcWidth(videoMeeting ? 24 : 96);
        clip.setArcHeight(videoMeeting ? 24 : 96);
        videoPane.setClip(clip);

        Label nameLabel = new Label(localParticipant ? name + " (Vous)" : name);
        nameLabel.setStyle(videoMeeting
                ? "-fx-text-fill: white; -fx-font-size: " + (localParticipant ? "13" : "16") + "; -fx-font-weight: bold;"
                : "-fx-text-fill: #0f3d91; -fx-font-size: 16; -fx-font-weight: bold;");
        nameLabel.setWrapText(true);
        nameLabel.setMaxWidth(tileWidth - 20);
        nameLabel.setAlignment(Pos.CENTER);

        FontIcon micIcon = new FontIcon(FontAwesomeSolid.MICROPHONE);
        micIcon.setIconSize(14);
        micIcon.setIconColor(Color.web("#25D366"));
        Label micStatus = new Label(videoMeeting ? "" : "Micro actif");
        micStatus.setStyle(videoMeeting
                ? "-fx-text-fill: #cfe0ff; -fx-font-size: 11;"
                : "-fx-text-fill: #6b7b93; -fx-font-size: 11;");
        HBox micIndicator = new HBox(6, micIcon, micStatus);
        micIndicator.setAlignment(Pos.CENTER);

        container.getChildren().addAll(videoPane, nameLabel, micIndicator);
        participantVideoPanes.put(userId, videoPane);
        participantVideos.put(userId, videoView);
        if (videoPlaceholder != null) {
            participantVideoPlaceholders.put(userId, videoPlaceholder);
        }
        participantLabels.put(userId, nameLabel);
        return container;
    }

    private void rebuildVideoGrid() {
        if (videoGrid == null) {
            return;
        }
        videoGrid.getChildren().clear();

        List<Integer> orderedIds = new ArrayList<>(participantContainers.keySet());
        orderedIds.sort((a, b) -> {
            if (Objects.equals(a, localParticipantId)) return -1;
            if (Objects.equals(b, localParticipantId)) return 1;
            if (a == -1) return -1;
            if (b == -1) return 1;
            return Integer.compare(a, b);
        });

        List<Integer> remoteIds = new ArrayList<>();
        Integer localId = null;
        for (Integer id : orderedIds) {
            if (id == localParticipantId || (id == -1 && localParticipantId <= 0)) {
                localId = id;
            } else {
                remoteIds.add(id);
            }
        }

        int count = remoteIds.size();
        for (int index = 0; index < remoteIds.size(); index++) {
            VBox container = participantContainers.get(remoteIds.get(index));
            if (container == null) {
                continue;
            }

            int col;
            int row;
            if (count == 3 && index == 2) {
                col = 0;
                row = 1;
                videoGrid.add(container, col, row, 2, 1);
                GridPane.setHalignment(container, HPos.CENTER);
            } else {
                col = index % 2;
                row = index / 2;
                videoGrid.add(container, col, row);
                GridPane.setHalignment(container, HPos.CENTER);
            }
        }

        if (localId != null) {
            VBox localContainer = participantContainers.get(localId);
            if (localContainer != null) {
                int localRow = Math.max(0, (count + 1) / 2);
                videoGrid.add(localContainer, 1, localRow);
                GridPane.setHalignment(localContainer, HPos.RIGHT);
                GridPane.setMargin(localContainer, new Insets(8, 0, 0, 0));
            }
        }
    }

    public void updateParticipantVideo(int userId, Image frame) {
        Platform.runLater(() -> {
            ImageView video = participantVideos.get(userId);
            Label placeholder = participantVideoPlaceholders.get(userId);
            StackPane videoPane = participantVideoPanes.get(userId);
            if (video != null && frame != null) {
                video.setImage(frame);
                if (placeholder != null) {
                    placeholder.setVisible(false);
                    placeholder.setManaged(false);
                }
                if (videoPane != null) {
                    videoPane.setStyle("-fx-background-color: #102027; -fx-background-radius: 20;");
                }
                System.out.println("[MEETING_UI] Frame affichee pour participant=" + userId);
            } else if (frame != null) {
                pendingParticipantFrames.put(userId, frame);
                System.out.println("[MEETING_UI] Frame mise en attente pour participant=" + userId);
            }
        });
    }

    public void updateParticipantFrame(String participantId, byte[] jpegFrame) {
        if (jpegFrame == null || jpegFrame.length == 0) return;
        try {
            int userId = Integer.parseInt(participantId);
            System.out.println("[MEETING_UI] Frame recue de participant=" + userId
                    + " taille=" + jpegFrame.length + " octets");
            updateParticipantVideo(userId, new Image(new ByteArrayInputStream(jpegFrame)));
        } catch (NumberFormatException ignored) {
        }
    }

    public void updateParticipantMicState(int userId, boolean micOn) {
        Platform.runLater(() -> {
            VBox container = participantContainers.get(userId);
            if (container != null && container.getChildren().size() > 2) {
                HBox micIndicator = (HBox) container.getChildren().get(2);
                if (!micIndicator.getChildren().isEmpty() && micIndicator.getChildren().get(0) instanceof FontIcon micIcon) {
                    micIcon.setIconLiteral(micOn ? "fas-microphone" : "fas-microphone-slash");
                    micIcon.setIconColor(micOn ? Color.web("#4caf50") : Color.web("#ff1744"));
                }
            }
        });
    }

    public void removeParticipant(int userId) {
        if (!participantIds.contains(userId)) return;
        participantIds.remove(userId);
        if (userId == localParticipantId) {
            localParticipantId = -1;
        }

        Platform.runLater(() -> {
            Label label = participantLabels.get(userId);
            if (label != null && participantsList != null) {
                participantsList.getItems().remove(label.getText());
            }
            VBox container = participantContainers.remove(userId);
            if (container != null) {
                if ("VIDEO".equals(callType) && videoGrid != null) {
                    rebuildVideoGrid();
                } else if (centerContainer != null) {
                    centerContainer.getChildren().remove(container);
                }
            }
            participantVideos.remove(userId);
            participantVideoPanes.remove(userId);
            participantVideoPlaceholders.remove(userId);
            participantLabels.remove(userId);
            pendingParticipantFrames.remove(userId);
            updateSubtitle();
            System.out.println("[MEETING_UI] Participant " + userId + " retire");
        });
    }

    public void removeParticipantByUsername(String participantUsername) {
        if (participantUsername == null) return;
        for (Map.Entry<Integer, Label> entry : participantLabels.entrySet()) {
            if (participantUsername.equals(entry.getValue().getText())) {
                removeParticipant(entry.getKey());
                return;
            }
        }
    }

    public void removeParticipantByName(String participantUsername) {
        removeParticipantByUsername(participantUsername);
    }

    public void updateParticipantsList(String participantsCsv) {
        if (participantsCsv == null || participantsCsv.isBlank()) return;
        for (String p : participantsCsv.split(",")) {
            String[] parts = p.split(":");
            if (parts.length >= 2) {
                try {
                    int id = Integer.parseInt(parts[0].trim());
                    String name = parts[1].trim();
                    if (!participantIds.contains(id)) {
                        addParticipant(id, name, null);
                    }
                } catch (NumberFormatException ignored) {
                }
            }
        }
    }

    public void syncParticipants(String csv) {
        Set<Integer> seenIds = new HashSet<>();
        if (csv != null && !csv.isBlank()) {
            for (String p : csv.split(",")) {
                String[] parts = p.split(":");
                if (parts.length >= 2) {
                    try {
                        seenIds.add(Integer.parseInt(parts[0].trim()));
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        }
        updateParticipantsList(csv);
        for (Integer existingId : new ArrayList<>(participantIds)) {
            if (existingId != -1 && !seenIds.contains(existingId)) {
                removeParticipant(existingId);
            }
        }
    }

    public void addAudioFrame(String participantId, byte[] audioData) {
        // Audio via relais UDP
    }

    public void handleMeetingEnded() {
        Platform.runLater(() -> {
            if (btnEndCall != null && btnEndCall.getScene() != null) {
                Stage stage = (Stage) btnEndCall.getScene().getWindow();
                if (stage != null) stage.close();
            }
        });
    }

    private void updateSubtitle() {
        if (lblSubtitle == null) return;
        int count = participantsList != null ? participantsList.getItems().size() : participantIds.size();
        lblSubtitle.setText(count + " participant(s)");
    }

    private int resolveLocalVideoTargetId() {
        if (localParticipantId > 0 && participantVideos.containsKey(localParticipantId)) {
            return localParticipantId;
        }
        return participantVideos.containsKey(-1) ? -1 : localParticipantId;
    }

    private String buildParticipantInitial(String name) {
        if (name == null || name.isBlank()) {
            return "?";
        }
        return name.trim().substring(0, 1).toUpperCase(Locale.ROOT);
    }

    private void endCall() {
        cleanup();
        if (networkClient != null && meetingId > 0) {
            if (isInitiator) {
                networkClient.endMeeting(meetingId);
            } else {
                networkClient.leaveMeeting(meetingId);
            }
        }
        Platform.runLater(() -> {
            if (btnEndCall != null && btnEndCall.getScene() != null) {
                Stage stage = (Stage) btnEndCall.getScene().getWindow();
                if (stage != null) stage.close();
            }
        });
    }

    public void cleanup() {
        videoOn = false;
        if (videoThread != null) {
            videoThread.interrupt();
        }
        if (webcam != null && webcam.isOpen()) {
            webcam.close();
        }
        if (networkClient != null) {
            networkClient.setActiveMeetingController(null);
            networkClient.closeUdpSockets();
        }
        pendingParticipantFrames.clear();
    }

    public static void openMeetingWindow(NetworkClient networkClient, int groupId, String callType,
                                         int meetingId, String title, String username, boolean isInitiator) throws IOException {
        URL resource = MeetingController.class.getResource("/fxml/meeting-window.fxml");
        if (resource == null) throw new IllegalStateException("meeting-window.fxml introuvable");

        FXMLLoader loader = new FXMLLoader(resource);
        Parent root = loader.load();
        MeetingController controller = loader.getController();

        controller.setCallType(callType != null ? callType : "AUDIO");
        controller.setGroupId(groupId);
        controller.setMeetingId(meetingId);
        controller.setInitiator(isInitiator);
        controller.setNetworkClient(networkClient);
        controller.setUsername(username);

        networkClient.setupUdpSockets();
        controller.startServices();

        Stage stage = new Stage();
        stage.setScene(new Scene(root, 900, 700));
        stage.setTitle("Reunion " + callType + " — " + title);
        stage.initModality(Modality.NONE);
        stage.setMinWidth(600);
        stage.setMinHeight(420);
        applyResponsiveStageSize(stage);

        stage.setOnShown(e -> controller.addParticipant(-1, username, null));
        stage.setOnCloseRequest(e -> {
            controller.cleanup();
            if (isInitiator) {
                networkClient.endMeeting(meetingId);
            } else {
                networkClient.leaveMeeting(meetingId);
            }
        });
        stage.show();
    }

    public static void applyResponsiveStageSize(Stage stage) {
        Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
        double width = Math.min(900, Math.max(640, bounds.getWidth() * 0.88));
        double height = Math.min(700, Math.max(420, bounds.getHeight() * 0.84));
        stage.setWidth(width);
        stage.setHeight(height);
        stage.setMaxWidth(bounds.getWidth());
        stage.setMaxHeight(bounds.getHeight());
        stage.centerOnScreen();
    }
}
