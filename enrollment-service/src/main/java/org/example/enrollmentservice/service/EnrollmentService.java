package org.example.enrollmentservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.enrollmentservice.client.CertificateClient;
import org.example.enrollmentservice.client.CourseClient;
import org.example.enrollmentservice.client.NotificationClient;
import org.example.enrollmentservice.client.StudentClient;
import org.example.enrollmentservice.dto.CourseDto;
import org.example.enrollmentservice.dto.NotificationRequest;
import org.example.enrollmentservice.dto.StudentDto;
import org.example.enrollmentservice.exception.ConflictException;
import org.example.enrollmentservice.model.Enrollment;
import org.example.enrollmentservice.repository.EnrollmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class EnrollmentService {

    private final StudentClient studentClient;
    private final CourseClient courseClient;
    private final CertificateClient certificateClient;
    private final NotificationClient notificationClient;
    private final EnrollmentRepository enrollmentRepository;

    @Transactional
    public Enrollment createEnrollment(Long studentId, Long courseId) {
        // 1. Валидация в соседних сервисах
        StudentDto student = studentClient.getStudentById(studentId);
        CourseDto course = courseClient.getCourseById(courseId);

        if (enrollmentRepository.existsByStudentIdAndCourseId(studentId, courseId)) {
            throw new ConflictException("Студент " + studentId + " уже записан на курс " + courseId);
        }

        // 2. Создание записи в БД
        Enrollment enrollment = new Enrollment();
        enrollment.setStudentId(studentId);
        enrollment.setCourseId(courseId);
        Enrollment savedEnrollment = enrollmentRepository.save(enrollment);
        log.info("Студент {} записан на курс {}, запись {}", studentId, courseId, savedEnrollment.getId());

        // 3. Вызов доп. сервиса после основного действия
        try {
            certificateClient.issueCertificate(savedEnrollment.getId());
        } catch (Exception e) {
            log.error("Не удалось выпустить сертификат для записи ID {}: {}", savedEnrollment.getId(), e.getMessage());
        }

        // 4. Уведомление студенту — если его профиль привязан к пользователю сайта
        if (student.getUserId() != null) {
            try {
                notificationClient.send(new NotificationRequest(student.getUserId(), "ENROLLMENT",
                        "Вы записаны на курс «" + course.getTitle() + "»"));
            } catch (Exception e) {
                log.error("Не удалось отправить уведомление о записи ID {}: {}", savedEnrollment.getId(), e.getMessage());
            }
        }

        return savedEnrollment;
    }

    public List<Enrollment> findAll(Long studentId) {
        if (studentId != null) {
            return enrollmentRepository.findByStudentId(studentId);
        }
        return enrollmentRepository.findAll();
    }
}
