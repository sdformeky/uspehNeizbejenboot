package org.example.certificateservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.example.certificateservice.dto.CertificateResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/certificates")
@Tag(name = "Certificate Controller", description = "Управление сертификатами")
public class CertificateController {

    @PostMapping
    @Operation(summary = "Выпустить автоматический сертификат при записи")
    public ResponseEntity<CertificateResponse> issueCertificate(@RequestParam Long enrollmentId) {
        CertificateResponse response = new CertificateResponse(System.currentTimeMillis(), enrollmentId, "GENERATED");
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
