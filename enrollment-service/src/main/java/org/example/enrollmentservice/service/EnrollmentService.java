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
import org.example.enrollmentservice.exception.ResourceNotFoundException;
import org.example.enrollmentservice.model.Enrollment;
import org.example.enrollmentservice.model.EnrollmentStatus;
import org.example.enrollmentservice.model.LessonProgress;
import org.example.enrollmentservice.repository.EnrollmentRepository;
import org.example.enrollmentservice.repository.LessonProgressRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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
    private final LessonProgressRepository lessonProgressRepository;

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

        // 3. Уведомление студенту — если его профиль привязан к пользователю сайта.
        // Сертификат выдаётся не здесь, а после прохождения всех уроков (completeLesson)
        notify(student, "ENROLLMENT", "Вы записаны на курс «" + course.getTitle() + "»", savedEnrollment.getId());

        return savedEnrollment;
    }

    public List<Enrollment> findAll(Long studentId, Long currentUserId, String roles) {
        if (studentId != null) {
            checkOwner(studentClient.getStudentById(studentId), currentUserId, roles);
            return enrollmentRepository.findByStudentId(studentId);
        }
        return enrollmentRepository.findAll();
    }

    public List<Long> findCompletedLessons(Long enrollmentId, Long currentUserId, String roles) {
        Enrollment enrollment = getEnrollment(enrollmentId);
        checkOwner(studentClient.getStudentById(enrollment.getStudentId()), currentUserId, roles);
        return lessonProgressRepository.findByEnrollmentId(enrollmentId).stream()
                .filter(LessonProgress::isCompleted)
                .map(LessonProgress::getLessonId)
                .toList();
    }

    @Transactional
    public Enrollment completeLesson(Long enrollmentId, Long lessonId, Long currentUserId, String roles) {
        Enrollment enrollment = getEnrollment(enrollmentId);
        StudentDto student = studentClient.getStudentById(enrollment.getStudentId());
        checkOwner(student, currentUserId, roles);
        if (enrollment.getStatus() != EnrollmentStatus.ACTIVE) {
            throw new ConflictException("Запись " + enrollmentId + " уже не активна: курс завершён или запись отменена");
        }
        List<Long> lessonIds = courseClient.getLessonIds(enrollment.getCourseId());
        if (!lessonIds.contains(lessonId)) {
            throw new ResourceNotFoundException("Урока " + lessonId + " нет в курсе " + enrollment.getCourseId());
        }

        LessonProgress progress = lessonProgressRepository.findByEnrollmentIdAndLessonId(enrollmentId, lessonId)
                .orElseGet(LessonProgress::new);
        if (!progress.isCompleted()) {
            progress.setEnrollment(enrollment);
            progress.setLessonId(lessonId);
            progress.setCompleted(true);
            progress.setCompletedAt(LocalDateTime.now());
            lessonProgressRepository.save(progress);
        }

        long done = lessonProgressRepository.findByEnrollmentId(enrollmentId).stream()
                .filter(p -> p.isCompleted() && lessonIds.contains(p.getLessonId()))
                .count();
        enrollment.setProgress((int) (done * 100 / lessonIds.size()));
        log.info("Запись {}: урок {} пройден, прогресс {}%", enrollmentId, lessonId, enrollment.getProgress());

        if (done == lessonIds.size()) {
            enrollment.setStatus(EnrollmentStatus.COMPLETED);
            enrollment.setCompletedAt(LocalDateTime.now());
            log.info("Запись {}: курс {} завершён", enrollmentId, enrollment.getCourseId());
            try {
                certificateClient.issueCertificate(enrollmentId);
            } catch (Exception e) {
                log.error("Не удалось выпустить сертификат для записи ID {}: {}", enrollmentId, e.getMessage());
            }
            notify(student, "CERTIFICATE_ISSUED", "Курс пройден! Сертификат — в разделе «Мои курсы»", enrollmentId);
        }
        return enrollmentRepository.save(enrollment);
    }

    private Enrollment getEnrollment(Long id) {
        return enrollmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Запись на курс " + id + " не найдена"));
    }

    private void notify(StudentDto student, String type, String text, Long enrollmentId) {
        if (student.getUserId() == null) {
            return;
        }
        try {
            notificationClient.send(new NotificationRequest(student.getUserId(), type, text));
        } catch (Exception e) {
            log.error("Не удалось отправить уведомление по записи ID {}: {}", enrollmentId, e.getMessage());
        }
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
