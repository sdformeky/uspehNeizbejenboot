package org.example.courseservice.dto;

// Нужные поля из ответа enrollment-service
public record EnrollmentShortDto(Long id, Long studentId, Long courseId) {
}
