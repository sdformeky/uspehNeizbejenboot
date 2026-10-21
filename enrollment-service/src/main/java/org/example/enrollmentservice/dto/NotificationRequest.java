package org.example.enrollmentservice.dto;

// Тело запроса в notification-service
public record NotificationRequest(Long userId, String type, String text) {
}
