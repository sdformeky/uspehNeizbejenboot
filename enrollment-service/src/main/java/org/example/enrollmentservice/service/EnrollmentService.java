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
import org.example.enrollmentservice.exception.ForbiddenException;
import org.example.enrollmentservice.model.Enrollment;
import org.example.enrollmentservice.model.EnrollmentStatus;
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
    public Enrollment createEnrollment(Long studentId, Long courseId, Long currentUserId, String roles) {
        // 1. Валидация в соседних сервисах
        StudentDto student = studentClient.getStudentById(studentId);
        checkOwner(student, currentUserId, roles);
        if (!"ACTIVE".equals(student.getStatus())) {
            throw new ConflictException("Студент " + studentId + " заблокирован — запись на курсы недоступна");
        }
        CourseDto course = courseClient.getCourseById(courseId);
        if (!"PUBLISHED".equals(course.getStatus())) {
            throw new ConflictException("Курс " + courseId + " не открыт для записи");
        }

        if (enrollmentRepository.existsByStudentIdAndCourseId(studentId, courseId)) {
            throw new ConflictException("Студент " + studentId + " уже записан на курс " + courseId);
        }
        if (course.getMaxStudents() != null
                && enrollmentRepository.countByCourseIdAndStatusNot(courseId, EnrollmentStatus.CANCELLED) >= course.getMaxStudents()) {
            throw new ConflictException("На курсе " + courseId + " нет свободных мест");
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

    public List<Enrollment> findAll(Long studentId, Long currentUserId, String roles) {
        if (studentId != null) {
            checkOwner(studentClient.getStudentById(studentId), currentUserId, roles);
            return enrollmentRepository.findByStudentId(studentId);
        }
        return enrollmentRepository.findAll();
    }

    // Пользователь (X-User-Id от gateway) работает только со своим профилем студента; ADMIN — с любым.
    // Нет заголовка — значит, вызов от другого сервиса, не через gateway
    private void checkOwner(StudentDto student, Long currentUserId, String roles) {
        boolean admin = roles != null && roles.contains("ADMIN");
        if (currentUserId != null && !admin && !currentUserId.equals(student.getUserId())) {
            throw new ForbiddenException("Можно записывать на курсы и смотреть записи только своего профиля");
        }
    }
}
