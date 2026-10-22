package org.example.courseservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Урок целиком: текст и навигация по курсу")
public record LessonViewDto(
        @Schema(description = "id урока", example = "1")
        Long id,

        @Schema(description = "id курса", example = "3")
        Long courseId,

        @Schema(description = "Название курса", example = "Java с нуля")
        String courseTitle,

        @Schema(description = "Номер модуля в курсе", example = "1")
        int modulePosition,

        @Schema(description = "Название модуля", example = "Первые шаги")
        String moduleTitle,

        @Schema(description = "Номер урока в курсе, по порядку модулей", example = "1")
        int number,

        @Schema(description = "Всего уроков в курсе", example = "8")
        int total,

        @Schema(description = "Название урока", example = "Установка JDK и IntelliJ IDEA")
        String title,

        @Schema(description = "Текст урока в формате Markdown")
        String content,

        @Schema(description = "Длительность, минут", example = "25")
        Integer durationMin,

        @Schema(description = "Есть ли у урока видео", example = "true")
        boolean hasVideo,

        @Schema(description = "id предыдущего урока курса; null — это первый урок", example = "null")
        Long prevLessonId,

        @Schema(description = "id следующего урока курса; null — это последний урок", example = "2")
        Long nextLessonId
) {
}
