package com.wechat.common;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Protocole de communication Client-Serveur WeChat.
 * Tous les messages échangés sont des instances de NetworkMessage.
 */
public class Protocol {

    public static final int SERVER_PORT = 5000;
    public static final String SERVER_HOST = "localhost";
    public static final int MAX_MESSAGE_LENGTH = 5000;

    /**
     * Types de messages réseau
     */
    public enum MessageType {
        // Authentification
        LOGIN_REQUEST, LOGIN_RESPONSE, REGISTER_REQUEST, REGISTER_RESPONSE,
        LOGOUT,

        // Chat
        TEXT_MESSAGE, FILE_MESSAGE, IMAGE_MESSAGE, AUDIO_MESSAGE, VIDEO_MESSAGE,

        // Appels
        CALL_OFFER, CALL_ANSWER, CALL_REJECT, CALL_END, CALL_ICE_CANDIDATE,

        // Statut / Présence
        USER_STATUS, TYPING, STOP_TYPING, USER_LIST,

        // Groupes
        GROUP_CREATE, GROUP_JOIN, GROUP_LEAVE, GROUP_MESSAGE,

        // Notifications
        NOTIFICATION, ERROR, PING, PONG
    }

    /**
     * Message réseau sérialisable - unité de communication
     */
    public static class NetworkMessage implements Serializable {
        private static final long serialVersionUID = 1L;

        private MessageType type;
        private Long senderId;
        private Long receiverId;
        private Long conversationId;
        private String content;
        private String payload;      // JSON additionnel ou données encodées
        private LocalDateTime timestamp;
        private boolean success;
        private String errorMessage;

        public NetworkMessage() {
            this.timestamp = LocalDateTime.now();
            this.success = true;
        }

        public NetworkMessage(MessageType type) {
            this();
            this.type = type;
        }

        // ===== FACTORY METHODS =====

        public static NetworkMessage loginRequest(String username, String password) {
            NetworkMessage msg = new NetworkMessage(MessageType.LOGIN_REQUEST);
            msg.content = username;
            msg.payload = password;
            return msg;
        }

        public static NetworkMessage loginResponse(boolean success, String message, Long userId) {
            NetworkMessage msg = new NetworkMessage(MessageType.LOGIN_RESPONSE);
            msg.success = success;
            msg.content = message;
            msg.senderId = userId;
            return msg;
        }

        public static NetworkMessage textMessage(Long senderId, Long conversationId, String text) {
            NetworkMessage msg = new NetworkMessage(MessageType.TEXT_MESSAGE);
            msg.senderId = senderId;
            msg.conversationId = conversationId;
            msg.content = text;
            return msg;
        }

        public static NetworkMessage callOffer(Long callerId, Long calleeId, String sdpOffer) {
            NetworkMessage msg = new NetworkMessage(MessageType.CALL_OFFER);
            msg.senderId = callerId;
            msg.receiverId = calleeId;
            msg.payload = sdpOffer;
            return msg;
        }

        public static NetworkMessage callAnswer(Long calleeId, Long callerId, String sdpAnswer) {
            NetworkMessage msg = new NetworkMessage(MessageType.CALL_ANSWER);
            msg.senderId = calleeId;
            msg.receiverId = callerId;
            msg.payload = sdpAnswer;
            return msg;
        }

        public static NetworkMessage userStatus(Long userId, String status) {
            NetworkMessage msg = new NetworkMessage(MessageType.USER_STATUS);
            msg.senderId = userId;
            msg.content = status;
            return msg;
        }

        public static NetworkMessage typing(Long senderId, Long conversationId) {
            NetworkMessage msg = new NetworkMessage(MessageType.TYPING);
            msg.senderId = senderId;
            msg.conversationId = conversationId;
            return msg;
        }

        public static NetworkMessage error(String error) {
            NetworkMessage msg = new NetworkMessage(MessageType.ERROR);
            msg.success = false;
            msg.errorMessage = error;
            return msg;
        }

        public static NetworkMessage pong() {
            return new NetworkMessage(MessageType.PONG);
        }

        // ===== GETTERS / SETTERS =====

        public MessageType getType() { return type; }
        public void setType(MessageType type) { this.type = type; }

        public Long getSenderId() { return senderId; }
        public void setSenderId(Long senderId) { this.senderId = senderId; }

        public Long getReceiverId() { return receiverId; }
        public void setReceiverId(Long receiverId) { this.receiverId = receiverId; }

        public Long getConversationId() { return conversationId; }
        public void setConversationId(Long conversationId) { this.conversationId = conversationId; }

        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }

        public String getPayload() { return payload; }
        public void setPayload(String payload) { this.payload = payload; }

        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

        @Override
        public String toString() {
            return String.format("NetworkMessage{type=%s, sender=%s, conv=%s, content='%s'}",
                    type, senderId, conversationId,
                    content != null ? content.substring(0, Math.min(content.length(), 50)) : "null");
        }
    }

    /**
     * Liste des utilisateurs connectés (pour broadcast)
     */
    public static class UserListUpdate implements Serializable {
        private static final long serialVersionUID = 1L;
        private List<Long> onlineUserIds;

        public UserListUpdate(List<Long> onlineUserIds) {
            this.onlineUserIds = onlineUserIds;
        }

        public List<Long> getOnlineUserIds() { return onlineUserIds; }
    }
}