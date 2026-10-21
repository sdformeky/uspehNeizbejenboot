package org.example.enrollmentservice.client;

import org.example.enrollmentservice.dto.CourseDto;
import org.example.enrollmentservice.exception.ResourceNotFoundException;
import org.example.enrollmentservice.exception.ServiceIntegrationException;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CourseClientFallback implements FallbackFactory<CourseClient> {
    @Override
    public CourseClient create(Throwable cause) {
        return new CourseClient() {
            @Override
            public CourseDto getCourseById(Long id) {
                throw failure(cause);
            }

            @Override
            public List<Long> getLessonIds(Long id) {
                throw failure(cause);
            }
        };
    }

    private RuntimeException failure(Throwable cause) {
        if (cause instanceof ResourceNotFoundException notFound) {
            return notFound;
        }
        return new ServiceIntegrationException(
            "Сервис курсов (course-service) временно недоступен. Сработал предохранитель (Circuit Breaker)."
        );
    }
}
