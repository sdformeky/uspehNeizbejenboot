package org.example.enrollmentservice.client;

import org.example.enrollmentservice.exception.ResourceNotFoundException;
import org.example.enrollmentservice.exception.ServiceIntegrationException;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Component
public class CourseClientFallback implements FallbackFactory<CourseClient> {
    @Override
    public CourseClient create(Throwable cause) {
        return id -> {
            if (cause instanceof ResourceNotFoundException notFound) {
                throw notFound;
            }
            throw new ServiceIntegrationException(
                "Сервис курсов (course-service) временно недоступен. Сработал предохранитель (Circuit Breaker)."
            );
        };
    }
}
