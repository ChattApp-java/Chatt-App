package org.example.tpchatjavafx.client.util;

import javafx.scene.Node;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.SVGPath;

import java.util.Locale;

public final class FileIconResolver {

    private FileIconResolver() {
    }

    public static Node getIconForFile(String filename) {
        String extension = getExtension(filename);
        IconSpec spec = switch (extension) {
            case "jpg", "jpeg", "png", "gif" -> new IconSpec(
                    "M4 5 C4 4.45 4.45 4 5 4 H19 C19.55 4 20 4.45 20 5 V19 C20 19.55 19.55 20 19 20 H5 C4.45 20 4 19.55 4 19 V5 Z M6 17 H18 L14.25 12 L11.25 16 L9.25 13.5 L6 17 Z M8.5 10 C9.33 10 10 9.33 10 8.5 C10 7.67 9.33 7 8.5 7 C7.67 7 7 7.67 7 8.5 C7 9.33 7.67 10 8.5 10 Z",
                    "file-icon-green");
            case "mp4", "avi" -> new IconSpec(
                    "M4 6 C4 5.45 4.45 5 5 5 H15 C15.55 5 16 5.45 16 6 V10.5 L20 7 V17 L16 13.5 V18 C16 18.55 15.55 19 15 19 H5 C4.45 19 4 18.55 4 18 V6 Z",
                    "file-icon-red");
            case "mp3", "wav" -> new IconSpec(
                    "M12 3 V14.55 C11.41 14.21 10.73 14 10 14 C8.34 14 7 15.12 7 16.5 C7 17.88 8.34 19 10 19 C11.66 19 13 17.88 13 16.5 V7 H18 V3 H12 Z",
                    "file-icon-purple");
            case "pdf" -> new IconSpec(
                    "M6 2 H14 L20 8 V21 C20 21.55 19.55 22 19 22 H6 C5.45 22 5 21.55 5 21 V3 C5 2.45 5.45 2 6 2 Z M13 3.5 V9 H18.5 M7.2 17.5 H8.25 V15.85 H9.05 C10.12 15.85 10.8 15.18 10.8 14.25 C10.8 13.32 10.12 12.65 9.05 12.65 H7.2 V17.5 Z M8.25 14.98 V13.52 H8.95 C9.44 13.52 9.73 13.8 9.73 14.25 C9.73 14.7 9.44 14.98 8.95 14.98 H8.25 Z M11.55 17.5 H13.25 C14.68 17.5 15.58 16.57 15.58 15.08 C15.58 13.58 14.68 12.65 13.25 12.65 H11.55 V17.5 Z M12.6 16.6 V13.55 H13.18 C14.02 13.55 14.5 14.1 14.5 15.08 C14.5 16.05 14.02 16.6 13.18 16.6 H12.6 Z M16.25 17.5 H17.3 V15.6 H19 V14.72 H17.3 V13.55 H19.2 V12.65 H16.25 V17.5 Z",
                    "file-icon-red");
            case "doc", "docx" -> new IconSpec(
                    "M6 2 H14 L20 8 V21 C20 21.55 19.55 22 19 22 H6 C5.45 22 5 21.55 5 21 V3 C5 2.45 5.45 2 6 2 Z M8 12 H17 V13.5 H8 V12 Z M8 15 H17 V16.5 H8 V15 Z M8 18 H14 V19.5 H8 V18 Z M13 3.5 V9 H18.5",
                    "file-icon-blue");
            case "xls", "xlsx" -> new IconSpec(
                    "M6 2 H14 L20 8 V21 C20 21.55 19.55 22 19 22 H6 C5.45 22 5 21.55 5 21 V3 C5 2.45 5.45 2 6 2 Z M8 12 L10.2 15 L8 18 H10 L11.25 16.1 L12.5 18 H14.5 L12.3 15 L14.5 12 H12.5 L11.25 13.9 L10 12 H8 Z M13 3.5 V9 H18.5",
                    "file-icon-green");
            case "zip", "rar" -> new IconSpec(
                    "M7 2 H15 L20 7 V21 C20 21.55 19.55 22 19 22 H7 C6.45 22 6 21.55 6 21 V3 C6 2.45 6.45 2 7 2 Z M10 4 H12 V6 H10 V8 H12 V10 H10 V12 H12 V14 H10 V16 H13 V12 H12 V10 H14 V8 H12 V6 H14 V4 H12 V2 H10 V4 Z",
                    "file-icon-yellow");
            default -> new IconSpec(
                    "M6 2 H14 L20 8 V21 C20 21.55 19.55 22 19 22 H6 C5.45 22 5 21.55 5 21 V3 C5 2.45 5.45 2 6 2 Z M13 3.5 V9 H18.5 M8 13 H17 V14.5 H8 V13 Z M8 16 H17 V17.5 H8 V16 Z",
                    "file-icon-gray");
        };

        SVGPath glyph = new SVGPath();
        glyph.setContent(spec.svgPath());
        glyph.getStyleClass().add("file-type-glyph");

        StackPane icon = new StackPane(glyph);
        icon.getStyleClass().addAll("file-type-icon", spec.colorClass());
        return icon;
    }

    private static String getExtension(String filename) {
        if (filename == null || filename.isBlank()) {
            return "";
        }
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == filename.length() - 1) {
            return "";
        }
        return filename.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
    }

    private record IconSpec(String svgPath, String colorClass) {
    }
}
