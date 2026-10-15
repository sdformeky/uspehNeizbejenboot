package org.example.courseservice.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
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

@Schema(description = "Данные для создания или полного обновления курса")
public record CourseRequest(
        @Schema(description = "Название курса", example = "Spring Boot и микросервисы",
                requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 200)
        @NotBlank(message = "Название обязательно")
        @Size(max = 200, message = "Название не длиннее 200 символов")
        String title,

        @Schema(description = "Описание курса", example = "REST, Spring Data JPA, Spring Cloud")
        String description,

        @Schema(description = "Цена в рублях, 0 — бесплатный курс", example = "14990.00",
                requiredMode = Schema.RequiredMode.REQUIRED, minimum = "0")
        @NotNull(message = "Цена обязательна")
        @DecimalMin(value = "0.00", message = "Цена не может быть отрицательной")
        @Digits(integer = 8, fraction = 2, message = "Цена: до 8 цифр до запятой и 2 после")
        BigDecimal price,

        @Schema(description = "Максимум студентов; пусто — без ограничения", example = "20", minimum = "1")
        @Positive(message = "Число мест должно быть больше нуля")
        Integer maxStudents,

        @Schema(description = "Статус курса; если не передан — DRAFT", example = "DRAFT")
        CourseStatus status,

        @Schema(description = "Дата начала", example = "2026-11-02")
        LocalDate startDate,

        @Schema(description = "Дата окончания, не раньше даты начала", example = "2027-01-31")
        LocalDate endDate,

        @Schema(description = "id категории", example = "1")
        Long categoryId,

        @Schema(description = "id преподавателя", example = "1")
        Long instructorId,

        @Schema(description = "id тегов курса", example = "[1, 2]")
        Set<Long> tagIds
) {

    @JsonIgnore
    @Schema(hidden = true)
    @AssertTrue(message = "Дата окончания не может быть раньше даты начала")
    public boolean isDatesValid() {
        return startDate == null || endDate == null || !endDate.isBefore(startDate);
    }
}
