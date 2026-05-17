package com.wechat.service;

import com.wechat.model.Notification;
import java.util.List;

public class NotificationService {

    public void sendNotification(Long userId, String type, String content) {
        // TODO: Implémentation
    }

    public List<Notification> getUnreadNotifications(Long userId) {
        // TODO: Implémentation
        return List.of();
    }

    public void markAsRead(Long notificationId) {
        // TODO: Implémentation
    }
}