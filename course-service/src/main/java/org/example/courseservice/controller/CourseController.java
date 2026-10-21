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

import java.util.List;

@RestController
@RequestMapping("/courses")
@RequiredArgsConstructor
@Tag(name = "Курсы")
public class CourseController {

    private final CourseService courseService;

    @GetMapping
    @Operation(summary = "Список всех курсов")
    public List<CourseDto> getAll() {
        return courseService.findAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Курс по id")
    public CourseDto getById(@PathVariable Long id) {
        return courseService.findById(id);
    }
}
