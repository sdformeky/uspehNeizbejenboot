package org.example.enrollmentservice.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        log.warn("404 {} {}: {}", request.getMethod(), request.getRequestURI(), ex.getMessage());
        return problem(HttpStatus.NOT_FOUND, "Не найдено", ex.getMessage());
    }

    @ExceptionHandler(ForbiddenException.class)
    public ProblemDetail handleForbidden(ForbiddenException ex, HttpServletRequest request) {
        log.warn("403 {} {}: {}", request.getMethod(), request.getRequestURI(), ex.getMessage());
        return problem(HttpStatus.FORBIDDEN, "Доступ запрещён", ex.getMessage());
    }

    @ExceptionHandler(ConflictException.class)
    public ProblemDetail handleConflict(ConflictException ex, HttpServletRequest request) {
        log.warn("409 {} {}: {}", request.getMethod(), request.getRequestURI(), ex.getMessage());
        return problem(HttpStatus.CONFLICT, "Конфликт", ex.getMessage());
    }

    @ExceptionHandler(ServiceIntegrationException.class)
    public ProblemDetail handleIntegrationError(ServiceIntegrationException ex, HttpServletRequest request) {
        log.warn("502 {} {}: {}", request.getMethod(), request.getRequestURI(), ex.getMessage());
        return problem(HttpStatus.BAD_GATEWAY, "Сервис недоступен", ex.getMessage());
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ProblemDetail handleMissingParameter(MissingServletRequestParameterException ex, HttpServletRequest request) {
        log.warn("400 {} {}: нет параметра '{}'", request.getMethod(), request.getRequestURI(), ex.getParameterName());
        return problem(HttpStatus.BAD_REQUEST, "Некорректный запрос", "Не передан параметр '" + ex.getParameterName() + "'");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        log.warn("400 {} {}: параметр '{}' = {}", request.getMethod(), request.getRequestURI(), ex.getName(), ex.getValue());
        return problem(HttpStatus.BAD_REQUEST, "Некорректный запрос",
                "Параметр '" + ex.getName() + "' имеет неверный формат: " + ex.getValue());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnexpected(Exception ex, HttpServletRequest request) {
        if (ex instanceof ErrorResponse errorResponse) {
            log.warn("{} {} {}: {}", errorResponse.getStatusCode().value(), request.getMethod(), request.getRequestURI(), ex.getMessage());
            return ResponseEntity.status(errorResponse.getStatusCode()).body(errorResponse.getBody());
        }
        log.error("500 {} {}: непредвиденная ошибка", request.getMethod(), request.getRequestURI(), ex);
        return ResponseEntity.internalServerError()
                .body(problem(HttpStatus.INTERNAL_SERVER_ERROR, "Внутренняя ошибка", "Что-то пошло не так, попробуйте позже"));
    }

    private ProblemDetail problem(HttpStatus status, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        return problem;
    }
}
