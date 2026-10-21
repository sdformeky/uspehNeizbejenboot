package org.example.certificateservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.certificateservice.dto.CertificateResponse;
import org.example.certificateservice.exception.NotFoundException;
import org.example.certificateservice.model.Certificate;
import org.example.certificateservice.model.CertificateTemplate;
import org.example.certificateservice.repository.CertificateRepository;
import org.example.certificateservice.repository.CertificateTemplateRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CertificateService {

    private static final String DEFAULT_TEMPLATE = "Стандартный";

    private final CertificateRepository certificateRepository;
    private final CertificateTemplateRepository templateRepository;

    // Повторный вызов для той же записи не создаёт второй сертификат, а возвращает уже выданный
    @Transactional
    public CertificateResponse issue(Long enrollmentId) {
        Optional<Certificate> existing = certificateRepository.findByEnrollmentId(enrollmentId);
        if (existing.isPresent()) {
            return CertificateResponse.from(existing.get());
        }
        CertificateTemplate template = templateRepository.findByName(DEFAULT_TEMPLATE)
                .orElseThrow(() -> new NotFoundException("Шаблон «" + DEFAULT_TEMPLATE + "» не найден"));

        Certificate certificate = new Certificate();
        certificate.setEnrollmentId(enrollmentId);
        certificate.setTemplate(template);
        // номер: UN-<год>-<id записи из 6 цифр>, запись на курс уникальна — номер тоже
        certificate.setNumber(String.format("UN-%d-%06d", Year.now().getValue(), enrollmentId));
        certificateRepository.save(certificate);
        log.info("Выдан сертификат {} по записи {}", certificate.getNumber(), enrollmentId);
        return CertificateResponse.from(certificate);
    }

    public List<CertificateResponse> findAll(Long enrollmentId) {
        if (enrollmentId != null) {
            return certificateRepository.findByEnrollmentId(enrollmentId).stream()
                    .map(CertificateResponse::from)
                    .toList();
        }
        return certificateRepository.findAll(Sort.by("id")).stream()
                .map(CertificateResponse::from)
                .toList();
    }

    public CertificateResponse findByNumber(String number) {
        return certificateRepository.findByNumber(number.trim().toUpperCase())
                .map(CertificateResponse::from)
                .orElseThrow(() -> new NotFoundException("Сертификат с номером " + number + " не найден"));
    }
}
