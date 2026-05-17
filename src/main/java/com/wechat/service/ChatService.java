package com.wechat.service;

import com.wechat.model.Message;
import java.util.List;

public class ChatService {

    public Message sendPrivateMessage(Long senderId, Long receiverId, String content) {
        // TODO: Implémentation
        return null;
    }

    public Message sendGroupMessage(Long senderId, Long groupId, String content) {
        // TODO: Implémentation
        return null;
    }

    public List<Message> getConversationHistory(Long userId1, Long userId2) {
        // TODO: Implémentation
        return List.of();
    }
}