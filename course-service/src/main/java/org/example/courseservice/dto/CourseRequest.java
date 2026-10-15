package org.example.courseservice.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.example.courseservice.model.CourseStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

public record CourseRequest(
        @NotBlank(message = "Название обязательно")
        @Size(max = 200, message = "Название не длиннее 200 символов")
        String title,

        String description,

        @NotNull(message = "Цена обязательна")
        @DecimalMin(value = "0.00", message = "Цена не может быть отрицательной")
        @Digits(integer = 8, fraction = 2, message = "Цена: до 8 цифр до запятой и 2 после")
        BigDecimal price,

        @Positive(message = "Число мест должно быть больше нуля")
        Integer maxStudents,

        CourseStatus status,

        LocalDate startDate,

        LocalDate endDate,

        Long categoryId,

        Long instructorId,

        Set<Long> tagIds
) {

    @JsonIgnore
    @AssertTrue(message = "Дата окончания не может быть раньше даты начала")
    public boolean isDatesValid() {
        return startDate == null || endDate == null || !endDate.isBefore(startDate);
    }
}
