package org.example.tpchatjavafx.client.model;

import org.example.tpchatjavafx.common.MessageType;
import java.util.Base64;

public class ChatMessage {

    private MessageType type;
    private int messageId = -1;
    private String from;
    private String to;
    private String conversationId;
    private String content;
    private byte[] binaryData;
    private String timestamp;

    // Appels / réunions
    private String callType;
    private String remoteHost;
    private int remotePort;

    private int groupId;
    private int meetingId;
    private String meetingType;
    private int udpAudioPort;
    private int udpVideoPort;
    private String serverHost;
    private int serverUdpAudioPort;
    private int serverUdpVideoPort;

    public ChatMessage() {
        // Constructeur vide pour désérialisation
    }

    public ChatMessage(MessageType type, String from, String to, String conversationId, String content) {
        this.type = type;
        this.from = from;
        this.to = to;
        this.conversationId = conversationId;
        this.content = content;
    }

    public MessageType getType() { return type; }
    public void setType(MessageType type) { this.type = type; }

    public int getMessageId() { return messageId; }
    public void setMessageId(int messageId) { this.messageId = messageId; }

    public String getFrom() { return from; }
    public void setFrom(String from) { this.from = from; }

    public String getTo() { return to; }
    public void setTo(String to) { this.to = to; }

    public String getConversationId() { return conversationId; }
    public void setConversationId(String conversationId) { this.conversationId = conversationId; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public byte[] getBinaryData() { return binaryData; }
    public void setBinaryData(byte[] binaryData) { this.binaryData = binaryData; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    public String getCallType() { return callType; }
    public void setCallType(String callType) { this.callType = callType; }

    public String getRemoteHost() { return remoteHost; }
    public void setRemoteHost(String remoteHost) { this.remoteHost = remoteHost; }

    public int getRemotePort() { return remotePort; }
    public void setRemotePort(int remotePort) { this.remotePort = remotePort; }

    public int getGroupId() { return groupId; }
    public void setGroupId(int groupId) { this.groupId = groupId; }

    public int getMeetingId() { return meetingId; }
    public void setMeetingId(int meetingId) { this.meetingId = meetingId; }

    public String getMeetingType() { return meetingType; }
    public void setMeetingType(String meetingType) { this.meetingType = meetingType; }

    public int getUdpAudioPort() { return udpAudioPort; }
    public void setUdpAudioPort(int udpAudioPort) { this.udpAudioPort = udpAudioPort; }

    public int getUdpVideoPort() { return udpVideoPort; }
    public void setUdpVideoPort(int udpVideoPort) { this.udpVideoPort = udpVideoPort; }

    public String getServerHost() { return serverHost; }
    public void setServerHost(String serverHost) { this.serverHost = serverHost; }

    public int getServerUdpAudioPort() { return serverUdpAudioPort; }
    public void setServerUdpAudioPort(int serverUdpAudioPort) { this.serverUdpAudioPort = serverUdpAudioPort; }

    public int getServerUdpVideoPort() { return serverUdpVideoPort; }
    public void setServerUdpVideoPort(int serverUdpVideoPort) { this.serverUdpVideoPort = serverUdpVideoPort; }

    public String serialize() {
        String base64 = (binaryData == null) ? "" : Base64.getEncoder().encodeToString(binaryData);
        return String.join("|",
                type != null ? type.name() : "",
                String.valueOf(messageId),
                safe(from),
                safe(to),
                safe(conversationId),
                safe(content),
                base64,
                safe(timestamp),
                safe(callType),
                safe(remoteHost),
                String.valueOf(remotePort),
                String.valueOf(groupId),
                String.valueOf(meetingId),
                safe(meetingType),
                String.valueOf(udpAudioPort),
                String.valueOf(udpVideoPort),
                safe(serverHost),
                String.valueOf(serverUdpAudioPort),
                String.valueOf(serverUdpVideoPort)
        );
    }

    private String safe(String value) {
        return value == null ? "" : value.replace("|", "␟");
    }

    private String unsafe(String value) {
        return value == null ? "" : value.replace("␟", "|");
    }

    public static ChatMessage deserialize(String line) {
        if (line == null || line.isEmpty()) return null;
        String[] parts = line.split("\\|", -1);
        if (parts.length < 19) return null;

        ChatMessage msg = new ChatMessage();
        try {
            msg.type = MessageType.valueOf(parts[0]);
        } catch (IllegalArgumentException e) {
            return null;
        }

        msg.messageId = parseIntSafe(parts[1], -1);
        msg.from = msg.unsafe(parts[2]);
        msg.to = msg.unsafe(parts[3]);
        msg.conversationId = msg.unsafe(parts[4]);
        msg.content = msg.unsafe(parts[5]);
        if (!parts[6].isBlank()) {
            msg.binaryData = Base64.getDecoder().decode(parts[6]);
        }
        msg.timestamp = msg.unsafe(parts[7]);
        msg.callType = msg.unsafe(parts[8]);
        msg.remoteHost = msg.unsafe(parts[9]);
        msg.remotePort = parseIntSafe(parts[10], 0);
        msg.groupId = parseIntSafe(parts[11], 0);
        msg.meetingId = parseIntSafe(parts[12], 0);
        msg.meetingType = msg.unsafe(parts[13]);
        msg.udpAudioPort = parseIntSafe(parts[14], 0);
        msg.udpVideoPort = parseIntSafe(parts[15], 0);
        msg.serverHost = msg.unsafe(parts[16]);
        msg.serverUdpAudioPort = parseIntSafe(parts[17], 0);
        msg.serverUdpVideoPort = parseIntSafe(parts[18], 0);

        return msg;
    }

    private static int parseIntSafe(String value, int defaultValue) {
        try {
            return value == null || value.isBlank() ? defaultValue : Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}

