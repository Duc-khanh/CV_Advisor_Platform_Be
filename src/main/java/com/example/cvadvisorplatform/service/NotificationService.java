package com.example.cvadvisorplatform.service;

import com.example.cvadvisorplatform.dto.NotificationResponse;

import java.util.List;

public interface NotificationService {

    List<NotificationResponse> getNotifications(Long userId, String type, Boolean unreadOnly);

    long getUnreadCount(Long userId);

    NotificationResponse markAsRead(Long userId, Long notificationId);

    void markAllAsRead(Long userId);

    void deleteNotification(Long userId, Long notificationId);

    void clearAll(Long userId);

    void createNotification(Long userId, String type, String title, String message, String link, String actionText, Long referenceId);

    void syncRealSystemEvents(Long userId);
}
