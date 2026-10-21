package org.example.courseservice.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    public ProblemDetail handleNotFound(NotFoundException e, HttpServletRequest request) {
        log.warn("404 {} {}: {}", request.getMethod(), request.getRequestURI(), e.getMessage());
        return problem(HttpStatus.NOT_FOUND, "Не найдено", e.getMessage());
    }

    @ExceptionHandler(ForbiddenException.class)
    public ProblemDetail handleForbidden(ForbiddenException e, HttpServletRequest request) {
        log.warn("403 {} {}: {}", request.getMethod(), request.getRequestURI(), e.getMessage());
        return problem(HttpStatus.FORBIDDEN, "Нет доступа", e.getMessage());
    }

    @ExceptionHandler(ConflictException.class)
    public ProblemDetail handleConflict(ConflictException e, HttpServletRequest request) {
        log.warn("409 {} {}: {}", request.getMethod(), request.getRequestURI(), e.getMessage());
        return problem(HttpStatus.CONFLICT, "Конфликт", e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException e, HttpServletRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError error : e.getBindingResult().getFieldErrors()) {
            errors.putIfAbsent(error.getField(), error.getDefaultMessage());
        }
        log.warn("400 {} {}: ошибка валидации {}", request.getMethod(), request.getRequestURI(), errors);
        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "Ошибка валидации", "Проверьте поля запроса");
        problem.setProperty("errors", errors);
        return problem;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleUnreadable(HttpMessageNotReadableException e, HttpServletRequest request) {
        log.warn("400 {} {}: тело запроса не читается", request.getMethod(), request.getRequestURI());
        return problem(HttpStatus.BAD_REQUEST, "Некорректный запрос", "Тело запроса не удалось прочитать");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail handleTypeMismatch(MethodArgumentTypeMismatchException e, HttpServletRequest request) {
        log.warn("400 {} {}: параметр '{}' = {}", request.getMethod(), request.getRequestURI(), e.getName(), e.getValue());
        return problem(HttpStatus.BAD_REQUEST, "Некорректный запрос",
                "Параметр '" + e.getName() + "' имеет неверный формат: " + e.getValue());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnexpected(Exception e, HttpServletRequest request) {
        if (e instanceof ErrorResponse errorResponse) {
            log.warn("{} {} {}: {}", errorResponse.getStatusCode().value(), request.getMethod(), request.getRequestURI(), e.getMessage());
            return ResponseEntity.status(errorResponse.getStatusCode()).body(errorResponse.getBody());
        }
        log.error("500 {} {}: непредвиденная ошибка", request.getMethod(), request.getRequestURI(), e);
        return ResponseEntity.internalServerError()
                .body(problem(HttpStatus.INTERNAL_SERVER_ERROR, "Внутренняя ошибка", "Что-то пошло не так, попробуйте позже"));
    }

    private ProblemDetail problem(HttpStatus status, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        return problem;
    }
}
