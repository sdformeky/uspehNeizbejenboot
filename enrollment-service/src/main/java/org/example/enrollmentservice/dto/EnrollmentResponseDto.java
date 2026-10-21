package org.example.enrollmentservice.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class EnrollmentResponseDto {
    private Long id;
    private StudentDto student;
    private CourseDto course;
    private LocalDateTime enrollmentDate;
    private String status;
}