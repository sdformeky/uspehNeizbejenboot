package org.example.studentservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.studentservice.dto.StudentDto;
import org.example.studentservice.service.StudentService;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * API для других сервисов (enrollment-service вызывает его через Feign по этому пути).
 * По ответу видно, существует ли студент и активен ли он.
 */
@RestController
@RequestMapping("/api/v1/students")
@RequiredArgsConstructor
@Tag(name = "Для других сервисов", description = "Вызывается enrollment-service через Feign, не через gateway")
public class InternalStudentController {

    private final StudentService studentService;

    @GetMapping("/{id}")
    @Operation(summary = "Студент по id для enrollment-service",
            description = "Проверка перед записью на курс: студент существует и status = ACTIVE")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Студент найден"),
            @ApiResponse(responseCode = "404", description = "Студент не найден", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public StudentDto getById(@Parameter(description = "id студента", example = "1") @PathVariable Long id) {
        return studentService.findById(id);
    }
}
