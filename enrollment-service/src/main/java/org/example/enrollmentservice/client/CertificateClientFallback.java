package org.example.enrollmentservice.client;

import org.example.enrollmentservice.exception.ServiceIntegrationException;
import org.springframework.stereotype.Component;

@Component
public class CertificateClientFallback implements CertificateClient {
    @Override
    public Object issueCertificate(Long enrollmentId) {
        throw new ServiceIntegrationException("Сервис сертификатов недоступен, но запись на курс успешно создана.");
    }
}
