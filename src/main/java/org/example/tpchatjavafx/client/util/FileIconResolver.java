package org.example.tpchatjavafx.client.util;

import javafx.scene.control.Label;

import java.util.Locale;

public final class FileIconResolver {

    private FileIconResolver() {
    }

    public static Label getIconForFile(String filename) {
        String extension = getExtension(filename);
        IconSpec spec = switch (extension) {
            case "jpg", "jpeg", "png", "gif" -> new IconSpec("\uf03e", "file-icon-green");
            case "mp4", "avi" -> new IconSpec("\uf03d", "file-icon-red");
            case "mp3", "wav" -> new IconSpec("\uf001", "file-icon-purple");
            case "pdf" -> new IconSpec("\uf1c1", "file-icon-red");
            case "doc", "docx" -> new IconSpec("\uf1c2", "file-icon-blue");
            case "xls", "xlsx" -> new IconSpec("\uf1c3", "file-icon-green");
            case "zip", "rar" -> new IconSpec("\uf1c6", "file-icon-yellow");
            default -> new IconSpec("\uf15b", "file-icon-gray");
        };

        Label icon = new Label(spec.glyph());
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

    private record IconSpec(String glyph, String colorClass) {
    }
}
