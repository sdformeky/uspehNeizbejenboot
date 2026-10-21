package org.example.enrollmentservice.client;

import org.example.enrollmentservice.exception.ServiceIntegrationException;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Component
public class CertificateClientFallback implements FallbackFactory<CertificateClient> {
    @Override
    public CertificateClient create(Throwable cause) {
        return enrollmentId -> {
            throw new ServiceIntegrationException("Сервис сертификатов недоступен, но запись на курс успешно создана.");
        };
    }
}
