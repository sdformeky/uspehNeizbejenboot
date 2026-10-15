package org.example.courseservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.example.courseservice.model.Category;
import org.example.courseservice.model.Course;
import org.example.courseservice.model.CourseStatus;
import org.example.courseservice.model.Instructor;
import org.example.courseservice.model.Tag;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "Курс")
public record CourseDto(
        @Schema(description = "id курса", example = "2")
        Long id,

        @Schema(description = "Название", example = "Spring Boot и микросервисы")
        String title,

        @Schema(description = "Описание", example = "REST, Spring Data JPA, Spring Cloud")
        String description,

        @Schema(description = "Цена в рублях", example = "14990.00")
        BigDecimal price,

        @Schema(description = "Максимум студентов; null — без ограничения", example = "20")
        Integer maxStudents,

        @Schema(description = "Статус: DRAFT — черновик, PUBLISHED — открыт для записи, ARCHIVED — в архиве",
                example = "PUBLISHED")
        CourseStatus status,

        @Schema(description = "Дата начала", example = "2026-11-02")
        LocalDate startDate,

        @Schema(description = "Дата окончания", example = "2027-01-31")
        LocalDate endDate,

        @Schema(description = "id категории", example = "1")
        Long categoryId,

        @Schema(description = "Название категории", example = "Программирование")
        String categoryName,

        @Schema(description = "id преподавателя", example = "1")
        Long instructorId,

        @Schema(description = "ФИО преподавателя", example = "Иванов Пётр Сергеевич")
        String instructorName,

        @Schema(description = "Теги курса", example = "[\"Java\", \"Spring\"]")
        List<String> tags
) {

    public static CourseDto from(Course course) {
        Category category = course.getCategory();
        Instructor instructor = course.getInstructor();
        return new CourseDto(
                course.getId(),
                course.getTitle(),
                course.getDescription(),
                course.getPrice(),
                course.getMaxStudents(),
                course.getStatus(),
                course.getStartDate(),
                course.getEndDate(),
                category != null ? category.getId() : null,
                category != null ? category.getName() : null,
                instructor != null ? instructor.getId() : null,
                instructor != null ? instructor.getFullName() : null,
                course.getTags().stream().map(Tag::getName).sorted().toList()
        );
    }
}
