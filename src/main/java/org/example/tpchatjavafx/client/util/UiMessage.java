package org.example.tpchatjavafx.client.util;

public class UiMessage {

    public enum Kind {
        TEXT,
        AUDIO,
        IMAGE,
        FILE        // 👈 new for generic files
    }

    private final Kind kind;
    private final boolean own;
    private final String text;      // message text OR filename / label
    private final String filePath;  // local path for audio / image / file
    private final String timestamp;

    public UiMessage(Kind kind, boolean own, String text, String filePath, String timestamp) {
        this.kind = kind;
        this.own = own;
        this.text = text;
        this.filePath = filePath;
        this.timestamp = timestamp;
    }

    public Kind getKind() {
        return kind;
    }

    public boolean isOwn() {
        return own;
    }

    public String getText() {
        return text;
    }

    public String getFilePath() {
        return filePath;
    }

    public String getTimestamp() {
        return timestamp;
    }
}
