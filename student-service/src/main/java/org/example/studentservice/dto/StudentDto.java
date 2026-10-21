package org.example.studentservice.dto;

import org.example.studentservice.model.Student;
import org.example.studentservice.model.StudentGroup;
import org.example.studentservice.model.StudentStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record StudentDto(
        Long id,
        Long userId,
        Long groupId,
        String groupName,
        String firstName,
        String lastName,
        String phone,
        LocalDate birthDate,
        StudentStatus status,
        LocalDateTime createdAt
) {

    public static StudentDto from(Student student) {
        StudentGroup group = student.getGroup();
        return new StudentDto(
                student.getId(),
                student.getUserId(),
                group != null ? group.getId() : null,
                group != null ? group.getName() : null,
                student.getFirstName(),
                student.getLastName(),
                student.getPhone(),
                student.getBirthDate(),
                student.getStatus(),
                student.getCreatedAt()
        );
    }
}
