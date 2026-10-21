package org.example.studentservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.studentservice.dto.StudentDto;
import org.example.studentservice.dto.StudentRequest;
import org.example.studentservice.service.StudentService;
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
@RequestMapping("/students")
@RequiredArgsConstructor
@Tag(name = "Студенты", description = "Профили студентов: просмотр, создание, изменение и удаление")
public class StudentController {

    private final StudentService studentService;

    @GetMapping
    @Operation(summary = "Список всех студентов", description = "Все студенты, отсортированные по id")
    @ApiResponse(responseCode = "200", description = "Список студентов")
    public List<StudentDto> getAll() {
        return studentService.findAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Студент по id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Студент найден"),
            @ApiResponse(responseCode = "400", description = "id не число", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Студент не найден", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public StudentDto getById(@Parameter(description = "id студента", example = "1") @PathVariable Long id) {
        return studentService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Создать студента", description = "Если статус не передан, студент создаётся активным (ACTIVE)")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Студент создан"),
            @ApiResponse(responseCode = "400", description = "Ошибка валидации: в поле errors — сообщение по каждому полю",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Группа не найдена", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "userId уже привязан к другому студенту",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public StudentDto create(@Valid @RequestBody StudentRequest request) {
        return studentService.create(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Обновить студента", description = "Полная замена: поля, которые не переданы, станут пустыми")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Студент обновлён"),
            @ApiResponse(responseCode = "400", description = "Ошибка валидации", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Студент или группа не найдены", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "userId уже привязан к другому студенту",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public StudentDto update(@Parameter(description = "id студента", example = "1") @PathVariable Long id,
                             @Valid @RequestBody StudentRequest request) {
        return studentService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Удалить студента", description = "Вместе со студентом удаляется его список избранных курсов")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Студент удалён"),
            @ApiResponse(responseCode = "404", description = "Студент не найден", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public void delete(@Parameter(description = "id студента", example = "1") @PathVariable Long id) {
        studentService.delete(id);
    }
}
