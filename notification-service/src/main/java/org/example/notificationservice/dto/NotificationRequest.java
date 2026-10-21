package org.example.notificationservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.example.notificationservice.model.NotificationType;

public record NotificationRequest(
        @Schema(description = "Кому — id пользователя из auth-service", example = "1")
        @NotNull(message = "userId обязателен")
        Long userId,

        @Schema(example = "ENROLLMENT")
        @NotNull(message = "Тип обязателен")
        NotificationType type,

        @Schema(example = "Вы записаны на курс «Java с нуля»")
        @NotBlank(message = "Текст обязателен")
        @Size(max = 1000, message = "Текст не длиннее 1000 символов")
        String text
) {
}
