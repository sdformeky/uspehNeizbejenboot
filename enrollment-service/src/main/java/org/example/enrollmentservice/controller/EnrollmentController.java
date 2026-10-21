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
            content = @Content(examples = @ExampleObject(value = "{\"error\": \"Not Found\", \"message\": \"Ресурс не найден в зависимом сервисе...\"}"))
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
            content = @Content(examples = @ExampleObject(value = "{\"error\": \"Dependency Service Error\", \"message\": \"Сервис курсов временно недоступен. Сработал предохранитель...\"}"))
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
}
