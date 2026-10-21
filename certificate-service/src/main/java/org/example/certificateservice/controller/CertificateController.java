package org.example.certificateservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.certificateservice.dto.CertificateResponse;
import org.example.certificateservice.service.CertificateService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/certificates")
@RequiredArgsConstructor
@Tag(name = "Certificate Controller", description = "Управление сертификатами")
public class CertificateController {

    private final CertificateService certificateService;

    @PostMapping
    @Operation(summary = "Выпустить автоматический сертификат при записи")
    public ResponseEntity<CertificateResponse> issueCertificate(@RequestParam Long enrollmentId) {
        CertificateResponse response = certificateService.issue(enrollmentId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "Сертификаты", description = "Все или по одной записи на курс (?enrollmentId=)")
    public List<CertificateResponse> getAll(@Parameter(example = "1") @RequestParam(required = false) Long enrollmentId) {
        return certificateService.findAll(enrollmentId);
    }

    @GetMapping("/{number}")
    @Operation(summary = "Проверить сертификат по номеру", description = "404 — такого сертификата нет")
    public CertificateResponse verify(@Parameter(example = "UN-2026-000001") @PathVariable String number) {
        return certificateService.findByNumber(number);
    }
}
