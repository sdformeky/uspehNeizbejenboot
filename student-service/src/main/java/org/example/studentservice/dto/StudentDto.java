package org.example.studentservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.example.studentservice.model.Student;
import org.example.studentservice.model.StudentGroup;
import org.example.studentservice.model.StudentStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "Студент")
public record StudentDto(
        @Schema(description = "id студента", example = "1")
        Long id,

        @Schema(description = "id пользователя из auth-service", example = "15")
        Long userId,

        @Schema(description = "id учебной группы", example = "1")
        Long groupId,

        @Schema(description = "Название группы", example = "ИСП-21")
        String groupName,

        @Schema(description = "Имя", example = "Алексей")
        String firstName,

        @Schema(description = "Фамилия", example = "Кузнецов")
        String lastName,

        @Schema(description = "Телефон", example = "+79161234567")
        String phone,

        @Schema(description = "Дата рождения", example = "2005-03-14")
        LocalDate birthDate,

        @Schema(description = "Статус: ACTIVE — может учиться, BLOCKED — заблокирован", example = "ACTIVE")
        StudentStatus status,

        @Schema(description = "Когда создан", example = "2026-10-03T11:17:40")
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
