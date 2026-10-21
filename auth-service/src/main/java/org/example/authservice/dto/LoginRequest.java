package org.example.authservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @Schema(example = "ivanov@example.com")
        @NotBlank(message = "Email обязателен")
        String email,

        @Schema(example = "secret123")
        @NotBlank(message = "Пароль обязателен")
        String password
) {
}
