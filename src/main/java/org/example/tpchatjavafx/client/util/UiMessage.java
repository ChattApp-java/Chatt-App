package org.example.tpchatjavafx.client.util;

public class UiMessage {

    public enum Kind {
        TEXT,
        SYSTEM,
        AUDIO,
        IMAGE,
        LOCATION, FILE ,       
    }

    private final Kind kind;
    private final boolean own;
    private final String text;      
    private final String filePath;  
    private final String timestamp;
    private final int messageId;
    private boolean read;
    private boolean selected;

    public UiMessage(Kind kind, boolean own, String text, String filePath, String timestamp) {
        this(kind, own, text, filePath, timestamp, -1);
    }

    public UiMessage(Kind kind, boolean own, String text, String filePath, String timestamp, int messageId) {
        this.kind = kind;
        this.own = own;
        this.text = text;
        this.filePath = filePath;
        this.timestamp = timestamp;
        this.messageId = messageId;
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

    public int getMessageId() {
        return messageId;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }
}
