package org.example.enrollmentservice.client;

import org.example.enrollmentservice.exception.ServiceIntegrationException;
import org.springframework.stereotype.Component;

@Component
public class CourseClientFallback implements CourseClient {
    @Override
    public Object getCourseById(Long id) {
        throw new ServiceIntegrationException(
            "Сервис курсов (course-service) временно недоступен. Сработал предохранитель (Circuit Breaker)."
        );
    }
}
