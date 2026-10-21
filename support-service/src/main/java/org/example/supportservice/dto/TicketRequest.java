package org.example.supportservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.example.supportservice.model.TicketCategory;
import org.example.supportservice.model.TicketPriority;

public record TicketRequest(
        @Schema(description = "Автор — id пользователя из auth-service", example = "1")
        @NotNull(message = "authorId обязателен")
        Long authorId,

        @Schema(example = "Не открывается урок 3")
        @NotBlank(message = "Тема обязательна")
        @Size(max = 200, message = "Тема не длиннее 200 символов")
        String subject,

        @Schema(example = "COURSE_ACCESS")
        TicketCategory category,

        @Schema(example = "MEDIUM")
        TicketPriority priority,

        @Schema(description = "Первое сообщение", example = "После записи на курс не вижу третий урок")
        @NotBlank(message = "Опишите проблему")
        @Size(max = 2000, message = "Сообщение не длиннее 2000 символов")
        String text
) {
}
