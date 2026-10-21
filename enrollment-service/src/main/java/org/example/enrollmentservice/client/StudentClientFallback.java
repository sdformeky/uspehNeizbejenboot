package org.example.enrollmentservice.client;

import org.example.enrollmentservice.exception.ResourceNotFoundException;
import org.example.enrollmentservice.exception.ServiceIntegrationException;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

// FallbackFactory, а не просто fallback: видно причину ошибки.
// 404 (студента нет) — это не отказ сервиса, его пробрасываем как есть
@Component
public class StudentClientFallback implements FallbackFactory<StudentClient> {
    @Override
    public StudentClient create(Throwable cause) {
        return id -> {
            if (cause instanceof ResourceNotFoundException notFound) {
                throw notFound;
            }
            throw new ServiceIntegrationException(
                "Сервис студентов (student-service) временно недоступен. Сработал предохранитель (Circuit Breaker)."
            );
        };
    }
}
