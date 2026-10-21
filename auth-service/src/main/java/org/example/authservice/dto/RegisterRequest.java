package org.example.authservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @Schema(example = "ivanov@example.com")
        @NotBlank(message = "Email обязателен")
        @Email(message = "Некорректный email")
        String email,

        @Schema(example = "secret123")
        @NotBlank(message = "Пароль обязателен")
        @Size(min = 6, max = 100, message = "Пароль от 6 до 100 символов")
        String password
) {
}
