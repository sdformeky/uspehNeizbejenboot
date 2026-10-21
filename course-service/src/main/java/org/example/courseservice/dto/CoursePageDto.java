package org.example.courseservice.dto;

import java.time.LocalDateTime;
import java.util.List;

// Всё для страницы курса одним ответом: курс, преподаватель, программа, вебинары, посты и отзывы
public record CoursePageDto(
        CourseDto course,
        String instructorBio,
        List<ModuleDto> modules,
        int lessonsCount,
        int totalMinutes,
        List<WebinarDto> webinars,
        List<PostDto> posts,
        List<ReviewDto> reviews,
        Double averageRating
) {

    public record ModuleDto(Long id, String title, int position, List<LessonDto> lessons) {
    }

    public record LessonDto(Long id, String title, Integer durationMin, boolean hasVideo) {
    }

    public record WebinarDto(Long id, String title, LocalDateTime startsAt, String link) {
    }

    public record PostDto(Long id, String title, String text, String authorName, LocalDateTime createdAt) {
    }

    public record ReviewDto(Long id, Long studentId, String studentName, int rating, String comment,
                            LocalDateTime createdAt) {
    }
}
