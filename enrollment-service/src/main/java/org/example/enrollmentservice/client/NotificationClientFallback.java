package org.example.enrollmentservice.client;

import org.example.enrollmentservice.exception.ServiceIntegrationException;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Component
public class NotificationClientFallback implements FallbackFactory<NotificationClient> {
    @Override
    public NotificationClient create(Throwable cause) {
        return request -> {
            throw new ServiceIntegrationException("Сервис уведомлений недоступен, но запись на курс успешно создана.");
        };
    }
}
