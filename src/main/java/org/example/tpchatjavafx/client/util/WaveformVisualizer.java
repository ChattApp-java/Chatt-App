package org.example.tpchatjavafx.client.util;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;

public class WaveformVisualizer extends Canvas {
    private List<Double> data = new ArrayList<>();
    private int playbackPosition = 0;

    public WaveformVisualizer() {
        this(220, 34);
    }

    public WaveformVisualizer(double width, double height) {
        super(width, height);
        widthProperty().addListener((obs, oldValue, newValue) -> draw());
        heightProperty().addListener((obs, oldValue, newValue) -> draw());
    }

    public void setData(List<Double> amplitudeData) {
        this.data = amplitudeData == null ? new ArrayList<>() : new ArrayList<>(amplitudeData);
        draw();
    }

    public void setPlaybackPosition(int pos) {
        this.playbackPosition = Math.max(0, pos);
        draw();
    }

    private void draw() {
        GraphicsContext gc = getGraphicsContext2D();
        double width = getWidth();
        double height = getHeight();
        gc.clearRect(0, 0, width, height);
        if (data.isEmpty() || width <= 0 || height <= 0) return;

        double barWidth = Math.max(2, width / data.size());
        double centerY = height / 2;
        for (int i = 0; i < data.size(); i++) {
            double normalized = Math.max(0.08, Math.min(1.0, data.get(i)));
            double barHeight = normalized * height * 0.82;
            gc.setFill(i < playbackPosition ? Color.WHITE : Color.rgb(125, 211, 252));
            gc.fillRoundRect(i * barWidth, centerY - barHeight / 2, Math.max(1, barWidth - 1), barHeight, 2, 2);
        }
    }
}
