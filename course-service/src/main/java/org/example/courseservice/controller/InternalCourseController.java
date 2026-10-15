package org.example.courseservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.courseservice.dto.CourseDto;
import org.example.courseservice.service.CourseService;
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
@Tag(name = "Для других сервисов")
public class InternalCourseController {

    private final CourseService courseService;

    @GetMapping("/{id}")
    @Operation(summary = "Курс по id для enrollment-service")
    public CourseDto getById(@PathVariable Long id) {
        return courseService.findById(id);
    }
}
