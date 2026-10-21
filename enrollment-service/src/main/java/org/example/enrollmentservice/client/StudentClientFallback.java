package org.example.enrollmentservice.client;

import org.example.enrollmentservice.exception.ServiceIntegrationException;
import org.springframework.stereotype.Component;

@Component
public class StudentClientFallback implements StudentClient {
    @Override
    public Object getStudentById(Long id) {
        throw new ServiceIntegrationException(
            "Сервис студентов (student-service) временно недоступен. Сработал предохранитель (Circuit Breaker)."
        );
    }
}
