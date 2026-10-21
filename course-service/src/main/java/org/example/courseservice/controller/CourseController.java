package org.example.courseservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.courseservice.dto.CourseDto;
import org.example.courseservice.dto.CoursePageDto;
import org.example.courseservice.dto.PostRequest;
import org.example.courseservice.dto.ReviewRequest;
import org.example.courseservice.service.CoursePageService;
import org.example.courseservice.service.CourseService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/courses")
@RequiredArgsConstructor
@Tag(name = "Курсы")
public class CourseController {

    private final CourseService courseService;
    private final CoursePageService coursePageService;

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

    @GetMapping("/{id}/page")
    @Operation(summary = "Страница курса",
            description = "Курс, преподаватель, программа (модули и уроки), вебинары, посты и отзывы со средней оценкой")
    public CoursePageDto getPage(@PathVariable Long id) {
        return coursePageService.getPage(id);
    }

    @PostMapping("/{id}/posts")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Опубликовать пост курса", description = "Только INSTRUCTOR или ADMIN (проверяет api-gateway)")
    public CoursePageDto.PostDto addPost(@PathVariable Long id, @Valid @RequestBody PostRequest request) {
        return coursePageService.addPost(id, request);
    }

    @PostMapping("/{id}/reviews")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Оставить отзыв",
            description = "Только записанный на курс студент, один отзыв на курс. 403 — не записан, 409 — отзыв уже есть")
    public CoursePageDto.ReviewDto addReview(@PathVariable Long id, @Valid @RequestBody ReviewRequest request,
                                             @Parameter(hidden = true)
                                             @RequestHeader(value = "X-User-Id", required = false) Long currentUserId) {
        return coursePageService.addReview(id, request, currentUserId);
    }
}
