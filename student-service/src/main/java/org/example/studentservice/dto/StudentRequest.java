package org.example.studentservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.example.studentservice.model.StudentStatus;

import java.time.LocalDate;

@Schema(description = "Данные для создания или полного обновления студента")
public record StudentRequest(
        @Schema(description = "id пользователя из auth-service; у одного пользователя — один студент", example = "15")
        Long userId,

        @Schema(description = "id учебной группы", example = "1")
        Long groupId,

        @Schema(description = "Имя", example = "Алексей", requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 100)
        @NotBlank(message = "Имя обязательно")
        @Size(max = 100, message = "Имя не длиннее 100 символов")
        String firstName,

        @Schema(description = "Фамилия", example = "Кузнецов", requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 100)
        @NotBlank(message = "Фамилия обязательна")
        @Size(max = 100, message = "Фамилия не длиннее 100 символов")
        String lastName,

        @Schema(description = "Телефон: 10–15 цифр, можно с + в начале", example = "+79161234567")
        @Pattern(regexp = "\\+?\\d{10,15}", message = "Телефон: 10–15 цифр, можно с + в начале")
        String phone,

        @Schema(description = "Дата рождения, в прошлом", example = "2005-03-14")
        @Past(message = "Дата рождения должна быть в прошлом")
        LocalDate birthDate,

        @Schema(description = "Статус; если не передан — ACTIVE", example = "ACTIVE")
        StudentStatus status
) {
}
