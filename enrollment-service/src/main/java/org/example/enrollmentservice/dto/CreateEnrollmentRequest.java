package org.example.enrollmentservice.dto;

import lombok.Data;

@Data
public class CreateEnrollmentRequest {
    private Long studentId;
    private Long courseId;
}