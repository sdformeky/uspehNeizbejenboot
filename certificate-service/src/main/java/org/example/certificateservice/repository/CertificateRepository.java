package org.example.certificateservice.repository;

import org.example.certificateservice.model.Certificate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CertificateRepository extends JpaRepository<Certificate, Long> {

    Optional<Certificate> findByNumber(String number);

    Optional<Certificate> findByEnrollmentId(Long enrollmentId);
}
