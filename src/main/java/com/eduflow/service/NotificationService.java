package com.eduflow.service;

import com.eduflow.dto.response.MessageResponse;
import com.eduflow.entity.communication.Notification;
import com.eduflow.entity.user.User;

import java.util.List;

public interface NotificationService {

    Notification createNotification(User recipient, String title, String message,
                                    Notification.NotificationType type,
                                    Notification.NotificationChannel channel,
                                    String referenceType, Long referenceId);

    MessageResponse sendOverdueFeesNotifications();

    MessageResponse sendUpcomingFeesNotifications(int daysBeforeDue);

    MessageResponse sendPaymentReminderForStudent(Long studentId);

    List<Notification> getUnreadNotifications(Long userId);

    void markAsRead(Long notificationId);

    void markAllAsRead(Long userId);
}
