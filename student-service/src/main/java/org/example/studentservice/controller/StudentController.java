package org.example.studentservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.studentservice.dto.StudentDto;
import org.example.studentservice.dto.StudentRequest;
import org.example.studentservice.exception.ForbiddenException;
import org.example.studentservice.service.StudentService;
import org.springframework.http.HttpStatus;
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
@Tag(name = "Студенты")
public class StudentController {

    private final StudentService studentService;

    @GetMapping
    @Operation(summary = "Список студентов",
            description = "Все студенты (только ADMIN) или профиль одного пользователя сайта (?userId=)")
    public List<StudentDto> getAll(@RequestParam(required = false) Long userId) {
        if (userId != null) {
            return studentService.findByUserId(userId);
        }
        return studentService.findAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Студент по id")
    public StudentDto getById(@PathVariable Long id) {
        return studentService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Создать студента", description = "Пользователь может создать профиль только себе, ADMIN — любой")
    public StudentDto create(@Valid @RequestBody StudentRequest request,
                             // заголовки ставит api-gateway по токену; при вызове из другого сервиса их нет
                             @RequestHeader(value = "X-User-Id", required = false) Long currentUserId,
                             @RequestHeader(value = "X-User-Roles", required = false) String roles) {
        boolean admin = roles != null && roles.contains("ADMIN");
        if (currentUserId != null && !admin && !currentUserId.equals(request.userId())) {
            throw new ForbiddenException("Профиль студента можно создать только себе");
        }
        return studentService.create(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Обновить студента", description = "Только ADMIN (проверяет api-gateway)")
    public StudentDto update(@PathVariable Long id, @Valid @RequestBody StudentRequest request) {
        return studentService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Удалить студента", description = "Только ADMIN (проверяет api-gateway)")
    public void delete(@PathVariable Long id) {
        studentService.delete(id);
    }
}
