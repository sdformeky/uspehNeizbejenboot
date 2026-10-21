package org.example.studentservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.example.studentservice.model.StudentStatus;

import java.time.LocalDate;

public record StudentRequest(
        Long userId,

        Long groupId,

        @NotBlank(message = "Имя обязательно")
        @Size(max = 100, message = "Имя не длиннее 100 символов")
        String firstName,

        @NotBlank(message = "Фамилия обязательна")
        @Size(max = 100, message = "Фамилия не длиннее 100 символов")
        String lastName,

        @Pattern(regexp = "\\+?\\d{10,15}", message = "Телефон: 10–15 цифр, можно с + в начале")
        String phone,

        @Past(message = "Дата рождения должна быть в прошлом")
        LocalDate birthDate,

        StudentStatus status
) {
}
