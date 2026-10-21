package org.example.enrollmentservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.enrollmentservice.model.Enrollment;
import org.example.enrollmentservice.service.EnrollmentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/enrollments")
@RequiredArgsConstructor
@Tag(name = "Enrollment Controller", description = "Управление регистрациями студентов на курсы")
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    @PostMapping
    @Operation(
        summary = "Создать новую запись на курс", 
        description = "Метод проверяет существование студента и курса через Feign-клиенты. Если один из сервисов недоступен, срабатывает Circuit Breaker."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201", 
            description = "Запись успешно создана"
        ),
        @ApiResponse(
            responseCode = "404", 
            description = "Студент или курс не найден в системе",
            content = @Content(examples = @ExampleObject(value = "{\"status\": 404, \"title\": \"Не найдено\", \"detail\": \"Ресурс не найден в зависимом сервисе: StudentClient\"}"))
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Попытка записать чужой профиль студента"
        ),
        @ApiResponse(
            responseCode = "409",
            description = "Студент уже записан на этот курс"
        ),
        @ApiResponse(
            responseCode = "502", 
            description = "Ошибка интеграции или сработал предохранитель Circuit Breaker",
            content = @Content(examples = @ExampleObject(value = "{\"status\": 502, \"title\": \"Сервис недоступен\", \"detail\": \"Сервис курсов (course-service) временно недоступен. Сработал предохранитель (Circuit Breaker).\"}"))
        )
    })
    public ResponseEntity<Enrollment> create(
            @Parameter(description = "ID студента", example = "42", required = true)
            @RequestParam Long studentId,
            
            @Parameter(description = "ID курса", example = "105", required = true)
            @RequestParam Long courseId,

            // заголовки ставит api-gateway по токену
            @Parameter(hidden = true) @RequestHeader(value = "X-User-Id", required = false) Long currentUserId,
            @Parameter(hidden = true) @RequestHeader(value = "X-User-Roles", required = false) String roles
    ) {
        Enrollment created = enrollmentService.createEnrollment(studentId, courseId, currentUserId, roles);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    @Operation(summary = "Записи на курсы", description = "Все записи или записи одного студента (?studentId=)")
    public List<Enrollment> getAll(
            @Parameter(description = "ID студента", example = "1")
            @RequestParam(required = false) Long studentId,
            @Parameter(hidden = true) @RequestHeader(value = "X-User-Id", required = false) Long currentUserId,
            @Parameter(hidden = true) @RequestHeader(value = "X-User-Roles", required = false) String roles
    ) {
        return enrollmentService.findAll(studentId, currentUserId, roles);
    }

    @GetMapping("/{id}/lessons")
    @Operation(summary = "Пройденные уроки", description = "id уроков, отмеченных пройденными в этой записи")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Список id пройденных уроков"),
        @ApiResponse(responseCode = "403", description = "Чужая запись"),
        @ApiResponse(responseCode = "404", description = "Запись не найдена")
    })
    public List<Long> getCompletedLessons(
            @Parameter(description = "ID записи на курс", example = "1") @PathVariable Long id,
            @Parameter(hidden = true) @RequestHeader(value = "X-User-Id", required = false) Long currentUserId,
            @Parameter(hidden = true) @RequestHeader(value = "X-User-Roles", required = false) String roles
    ) {
        return enrollmentService.findCompletedLessons(id, currentUserId, roles);
    }

    @PostMapping("/{id}/lessons/{lessonId}/complete")
    @Operation(
        summary = "Отметить урок пройденным",
        description = "Пересчитывает прогресс. Когда пройдены все уроки курса, запись становится COMPLETED, "
            + "certificate-service выпускает сертификат, notification-service присылает уведомление."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Урок отмечен, в ответе — запись с новым прогрессом"),
        @ApiResponse(responseCode = "403", description = "Чужая запись"),
        @ApiResponse(responseCode = "404", description = "Запись не найдена или урока нет в этом курсе"),
        @ApiResponse(responseCode = "409", description = "Запись уже завершена или отменена")
    })
    public Enrollment completeLesson(
            @Parameter(description = "ID записи на курс", example = "1") @PathVariable Long id,
            @Parameter(description = "ID урока", example = "1") @PathVariable Long lessonId,
            @Parameter(hidden = true) @RequestHeader(value = "X-User-Id", required = false) Long currentUserId,
            @Parameter(hidden = true) @RequestHeader(value = "X-User-Roles", required = false) String roles
    ) {
        return enrollmentService.completeLesson(id, lessonId, currentUserId, roles);
    }
}
