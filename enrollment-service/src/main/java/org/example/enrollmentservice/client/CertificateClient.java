package org.example.enrollmentservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "certificate-service", fallback = CertificateClientFallback.class)
public interface CertificateClient {

    @PostMapping("/api/certificates")
    Object issueCertificate(@RequestParam("enrollmentId") Long enrollmentId);
}
