package org.example.enrollmentservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.enrollmentservice.client.CertificateClient;
import org.example.enrollmentservice.client.CourseClient;
import org.example.enrollmentservice.client.StudentClient;
import org.example.enrollmentservice.entity.Enrollment;
import org.example.enrollmentservice.repository.EnrollmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class EnrollmentService {

    private final StudentClient studentClient;
    private final CourseClient courseClient;
    private final CertificateClient certificateClient; 
    private final EnrollmentRepository enrollmentRepository;

    @Transactional
    public Enrollment createEnrollment(Long studentId, Long courseId) {
        // 1. Валидация в соседних сервисах
        studentClient.getStudentById(studentId);
        courseClient.getCourseById(courseId);

        // 2. Создание записи в БД
        Enrollment enrollment = new Enrollment();
        enrollment.setStudentId(studentId);
        enrollment.setCourseId(courseId);
        Enrollment savedEnrollment = enrollmentRepository.save(enrollment);

        // 3. Вызов доп. сервиса после основного действия
        try {
            certificateClient.issueCertificate(savedEnrollment.getId());
        } catch (Exception e) {
            log.error("Не удалось выпустить сертификат для записи ID {}: {}", savedEnrollment.getId(), e.getMessage());
        }

        return savedEnrollment;
    }
}
