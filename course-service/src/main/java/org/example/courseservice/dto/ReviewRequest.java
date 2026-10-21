package org.example.courseservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record ReviewRequest(
        @Schema(example = "5")
        @Min(value = 1, message = "Оценка от 1 до 5")
        @Max(value = 5, message = "Оценка от 1 до 5")
        int rating,

        @Schema(example = "Понятно объясняют, много практики")
        @Size(max = 2000, message = "Отзыв не длиннее 2000 символов")
        String comment
) {
}
