package org.example.courseservice.dto;

import org.example.courseservice.model.Category;
import org.example.courseservice.model.Course;
import org.example.courseservice.model.CourseStatus;
import org.example.courseservice.model.Instructor;
import org.example.courseservice.model.Tag;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record CourseDto(
        Long id,
        String title,
        String description,
        BigDecimal price,
        Integer maxStudents,
        CourseStatus status,
        LocalDate startDate,
        LocalDate endDate,
        Long categoryId,
        String categoryName,
        Long instructorId,
        String instructorName,
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
