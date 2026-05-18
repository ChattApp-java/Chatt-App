package org.example.tpchatjavafx.client.video;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.image.Image;
import javafx.scene.layout.GridPane;
import org.example.tpchatjavafx.client.util.VideoGridCell;

import java.io.ByteArrayInputStream;
import java.util.LinkedHashMap;
import java.util.Map;

public class MeetingVideoDisplay {

    private GridPane grid;
    private final Map<String, VideoGridCell> participants = new LinkedHashMap<>();
    private int columns = 2;

    public void setGrid(GridPane grid) {
        this.grid = grid;
        if (grid != null) {
            grid.setHgap(8);
            grid.setVgap(8);
            grid.setPadding(new Insets(10));
            grid.widthProperty().addListener((obs, oldValue, newValue) -> resizeCells());
            grid.heightProperty().addListener((obs, oldValue, newValue) -> resizeCells());
        }
    }

    public void addParticipant(String participantId, String displayName) {
        if (participantId == null || displayName == null || grid == null) {
            return;
        }
        if (participants.containsKey(participantId)) {
            return;
        }
        VideoGridCell cell = new VideoGridCell(displayName);
        participants.put(participantId, cell);
        relayout();
    }

    public void removeParticipant(String participantId) {
        if (participantId == null || grid == null) {
            return;
        }
        participants.remove(participantId);
        relayout();
    }

    public void clearParticipants() {
        participants.clear();
        relayout();
    }

    public void updateFrame(String participantId, byte[] jpegFrame) {
        if (participantId == null || jpegFrame == null || !participants.containsKey(participantId)) {
            return;
        }
        VideoGridCell cell = participants.get(participantId);
        if (cell == null) return;
        Platform.runLater(() -> {
            Image image = new Image(new ByteArrayInputStream(jpegFrame));
            cell.setImage(image);
        });
    }

    private void relayout() {
        if (grid == null) {
            return;
        }
        Platform.runLater(() -> {
            grid.getChildren().clear();
            int count = participants.size();
            if (count <= 1) columns = 1;
            else if (count <= 4) columns = 2;
            else columns = 3;

            int row = 0;
            int col = 0;
            for (VideoGridCell cell : participants.values()) {
                grid.add(cell.getRoot(), col, row);
                col++;
                if (col >= columns) {
                    col = 0;
                    row++;
                }
            }
            resizeCells();
        });
    }

    private void resizeCells() {
        if (grid == null || participants.isEmpty()) {
            return;
        }
        int count = participants.size();
        int rows = (int) Math.ceil(count / (double) Math.max(columns, 1));
        double availableWidth = Math.max(240, grid.getWidth() - grid.getPadding().getLeft() - grid.getPadding().getRight());
        double availableHeight = Math.max(180, grid.getHeight() - grid.getPadding().getTop() - grid.getPadding().getBottom());
        double cellWidth = (availableWidth - (Math.max(columns, 1) - 1) * grid.getHgap()) / Math.max(columns, 1);
        double cellHeight = (availableHeight - (Math.max(rows, 1) - 1) * grid.getVgap()) / Math.max(rows, 1);
        for (VideoGridCell cell : participants.values()) {
            cell.resize(cellWidth, cellHeight);
        }
    }
}

