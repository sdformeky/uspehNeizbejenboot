package org.example.courseservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.courseservice.dto.CourseDto;
import org.example.courseservice.service.CourseService;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * API для других сервисов (enrollment-service вызывает его через Feign по этому пути).
 * По ответу видно, существует ли курс, опубликован ли он и сколько в нём мест.
 */
@RestController
@RequestMapping("/api/v1/courses")
@RequiredArgsConstructor
@Tag(name = "Для других сервисов", description = "Вызывается enrollment-service через Feign, не через gateway")
public class InternalCourseController {

    private final CourseService courseService;

    @GetMapping("/{id}")
    @Operation(summary = "Курс по id для enrollment-service",
            description = "Проверка перед записью: курс существует, status = PUBLISHED, есть ли ограничение мест")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Курс найден"),
            @ApiResponse(responseCode = "404", description = "Курс не найден", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public CourseDto getById(@Parameter(description = "id курса", example = "1") @PathVariable Long id) {
        return courseService.findById(id);
    }
}
