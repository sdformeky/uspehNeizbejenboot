package org.example.enrollmentservice.config;

import feign.codec.ErrorDecoder;
import org.example.enrollmentservice.exception.ResourceNotFoundException;
import org.example.enrollmentservice.exception.ServiceIntegrationException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeignConfig {

    @Bean
    public ErrorDecoder errorDecoder() {
        return (methodKey, response) -> {
            String serviceName = methodKey.split("#")[0];
            
            if (response.status() == 404) {
                return new ResourceNotFoundException("Ресурс не найден в зависимом сервисе: " + serviceName);
            }
            if (response.status() >= 400 && response.status() < 500) {
                return new ServiceIntegrationException("Некорректный запрос к сервису " + serviceName + " (Код: " + response.status() + ")");
            }
            return new ServiceIntegrationException("Сервис " + serviceName + " вернул внутреннюю ошибку (Код: " + response.status() + ")");
        };
    }
}
