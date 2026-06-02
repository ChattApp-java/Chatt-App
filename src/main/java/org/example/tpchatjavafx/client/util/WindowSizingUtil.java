package org.example.tpchatjavafx.client.util;

import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;
import javafx.stage.Stage;

public final class WindowSizingUtil {

    private WindowSizingUtil() {
    }

    public static void applyResponsiveStageSize(Stage stage, double preferredWidth, double preferredHeight,
                                                double minWidth, double minHeight) {
        Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
        double width = Math.min(preferredWidth, Math.max(minWidth, bounds.getWidth() * 0.88));
        double height = Math.min(preferredHeight, Math.max(minHeight, bounds.getHeight() * 0.84));
        stage.setMinWidth(minWidth);
        stage.setMinHeight(minHeight);
        stage.setWidth(width);
        stage.setHeight(height);
        stage.setMaxWidth(bounds.getWidth());
        stage.setMaxHeight(bounds.getHeight());
        stage.centerOnScreen();
    }
}
