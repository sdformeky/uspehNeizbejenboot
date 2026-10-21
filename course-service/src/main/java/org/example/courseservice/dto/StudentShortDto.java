package org.example.courseservice.dto;

// Нужные поля из ответа student-service
public record StudentShortDto(Long id, Long userId, String firstName, String lastName) {
}
