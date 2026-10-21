package org.example.courseservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PostRequest(
        @Schema(example = "Вебинар переносится на пятницу")
        @NotBlank(message = "Заголовок обязателен")
        @Size(max = 200, message = "Заголовок не длиннее 200 символов")
        String title,

        @Schema(example = "Встречаемся в 19:00, ссылка придёт в уведомлении")
        @NotBlank(message = "Текст обязателен")
        @Size(max = 5000, message = "Текст не длиннее 5000 символов")
        String text
) {
}
