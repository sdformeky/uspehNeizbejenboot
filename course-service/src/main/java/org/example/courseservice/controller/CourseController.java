package org.example.courseservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.courseservice.dto.CourseDto;
import org.example.courseservice.dto.CourseRequest;
import org.example.courseservice.service.CourseService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/courses")
@RequiredArgsConstructor
@Tag(name = "Курсы", description = "Каталог курсов: просмотр, создание, изменение и удаление")
public class CourseController {

    private final CourseService courseService;

    @GetMapping
    @Operation(summary = "Список всех курсов", description = "Все курсы в любом статусе, отсортированные по id")
    @ApiResponse(responseCode = "200", description = "Список курсов")
    public List<CourseDto> getAll() {
        return courseService.findAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Курс по id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Курс найден"),
            @ApiResponse(responseCode = "400", description = "id не число", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Курс не найден", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public CourseDto getById(@Parameter(description = "id курса", example = "1") @PathVariable Long id) {
        return courseService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Создать курс", description = "Если статус не передан, курс создаётся черновиком (DRAFT)")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Курс создан"),
            @ApiResponse(responseCode = "400", description = "Ошибка валидации: в поле errors — сообщение по каждому полю",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Не найдены категория, преподаватель или теги",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public CourseDto create(@Valid @RequestBody CourseRequest request) {
        return courseService.create(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Обновить курс", description = "Полная замена: поля, которые не переданы, станут пустыми")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Курс обновлён"),
            @ApiResponse(responseCode = "400", description = "Ошибка валидации", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Не найдены курс, категория, преподаватель или теги",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public CourseDto update(@Parameter(description = "id курса", example = "1") @PathVariable Long id,
                            @Valid @RequestBody CourseRequest request) {
        return courseService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Удалить курс", description = "Нельзя удалить курс, у которого есть модули, отзывы или вебинары")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Курс удалён"),
            @ApiResponse(responseCode = "404", description = "Курс не найден", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "У курса есть модули, отзывы или вебинары",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public void delete(@Parameter(description = "id курса", example = "1") @PathVariable Long id) {
        courseService.delete(id);
    }
}
