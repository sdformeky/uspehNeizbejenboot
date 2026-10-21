package org.example.authservice.dto;

// Ответ на вход и регистрацию: токен для заголовка Authorization: Bearer <token> и данные пользователя
public record AuthResponse(String token, UserDto user) {
}
