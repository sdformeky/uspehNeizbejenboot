package org.example.notificationservice.dto;

import org.example.notificationservice.model.Notification;
import org.example.notificationservice.model.NotificationType;

import java.time.LocalDateTime;

public record NotificationDto(
        Long id,
        Long userId,
        NotificationType type,
        String text,
        boolean read,
        LocalDateTime createdAt
) {

    public static NotificationDto from(Notification n) {
        return new NotificationDto(n.getId(), n.getUserId(), n.getType(), n.getText(), n.isRead(), n.getCreatedAt());
    }
}
