package org.example.authservice.dto;

import org.example.authservice.model.Role;
import org.example.authservice.model.User;

import java.time.LocalDateTime;
import java.util.List;

// Пароль (даже хэш) наружу не отдаём
public record UserDto(
        Long id,
        String email,
        List<String> roles,
        boolean enabled,
        LocalDateTime createdAt
) {

    public static UserDto from(User user) {
        List<String> roles = user.getRoles().stream()
                .map(Role::getName)
                .map(Enum::name)
                .sorted()
                .toList();
        return new UserDto(user.getId(), user.getEmail(), roles, user.isEnabled(), user.getCreatedAt());
    }
}
