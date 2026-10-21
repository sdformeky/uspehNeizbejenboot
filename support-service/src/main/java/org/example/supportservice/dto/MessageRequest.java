package org.example.supportservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record MessageRequest(
        @Schema(example = "1")
        @NotNull(message = "authorId обязателен")
        Long authorId,

        @Schema(example = "Спасибо, теперь всё работает")
        @NotBlank(message = "Сообщение не может быть пустым")
        @Size(max = 2000, message = "Сообщение не длиннее 2000 символов")
        String text
) {
}
