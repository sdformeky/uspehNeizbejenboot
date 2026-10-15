package org.example.studentservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.studentservice.dto.StudentDto;
import org.example.studentservice.service.StudentService;
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
@Tag(name = "Для других сервисов")
public class InternalStudentController {

    private final StudentService studentService;

    @GetMapping("/{id}")
    @Operation(summary = "Студент по id для enrollment-service")
    public StudentDto getById(@PathVariable Long id) {
        return studentService.findById(id);
    }
}
