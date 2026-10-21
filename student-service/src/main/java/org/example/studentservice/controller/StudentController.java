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
import org.example.studentservice.exception.ForbiddenException;
import org.example.studentservice.service.StudentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
    @Operation(summary = "Список студентов",
            description = "Все студенты, отсортированные по id (только ADMIN), или профиль одного пользователя сайта (?userId=)")
    @ApiResponse(responseCode = "200", description = "Список студентов")
    public List<StudentDto> getAll(@Parameter(description = "id пользователя из auth-service", example = "3")
                                   @RequestParam(required = false) Long userId) {
        if (userId != null) {
            return studentService.findByUserId(userId);
        }
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
    @Operation(summary = "Создать студента", description = "Если статус не передан, студент создаётся активным (ACTIVE). "
            + "Пользователь может создать профиль только себе, ADMIN — любой")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Студент создан"),
            @ApiResponse(responseCode = "400", description = "Ошибка валидации: в поле errors — сообщение по каждому полю",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Профиль для чужого пользователя", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Группа не найдена", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "userId уже привязан к другому студенту",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public StudentDto create(@Valid @RequestBody StudentRequest request,
                             // заголовки ставит api-gateway по токену; при вызове из другого сервиса их нет
                             @Parameter(hidden = true) @RequestHeader(value = "X-User-Id", required = false) Long currentUserId,
                             @Parameter(hidden = true) @RequestHeader(value = "X-User-Roles", required = false) String roles) {
        boolean admin = roles != null && roles.contains("ADMIN");
        if (currentUserId != null && !admin && !currentUserId.equals(request.userId())) {
            throw new ForbiddenException("Профиль студента можно создать только себе");
        }
        return studentService.create(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Обновить студента", description = "Полная замена: поля, которые не переданы, станут пустыми. Только ADMIN (проверяет api-gateway)")
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
    @Operation(summary = "Удалить студента", description = "Вместе со студентом удаляется его список избранных курсов. Только ADMIN (проверяет api-gateway)")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Студент удалён"),
            @ApiResponse(responseCode = "404", description = "Студент не найден", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public void delete(@Parameter(description = "id студента", example = "1") @PathVariable Long id) {
        studentService.delete(id);
    }
}
