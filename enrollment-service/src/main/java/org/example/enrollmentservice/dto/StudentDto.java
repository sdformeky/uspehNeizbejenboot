package org.example.enrollmentservice.dto;

import lombok.Data;

// Ответ student-service: берём только нужные поля, остальные Jackson пропускает
@Data
public class StudentDto {
    private Long id;
    private Long userId;
    private String firstName;
    private String lastName;
    private String status;
}
