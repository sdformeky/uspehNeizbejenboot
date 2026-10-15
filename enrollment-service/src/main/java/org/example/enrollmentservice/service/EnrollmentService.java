package org.example.enrollmentservice.service;

import lombok.RequiredArgsConstructor;
import org.example.enrollmentservice.client.CourseClient;
import org.example.enrollmentservice.client.StudentClient;
import org.example.enrollmentservice.entity.Enrollment;
import org.example.enrollmentservice.repository.EnrollmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EnrollmentService {

    private final StudentClient studentClient;
    private final CourseClient courseClient;
    private final EnrollmentRepository enrollmentRepository;

    @Transactional
    public Enrollment createEnrollment(Long studentId, Long courseId) {
        // Проверка существования студента и курса через Feign-клиенты
        studentClient.getStudentById(studentId);
        courseClient.getCourseById(courseId);

        // Создание записи, импорт сущности теперь правильный (из пакета entity)
        Enrollment enrollment = new Enrollment();
        enrollment.setStudentId(studentId);
        enrollment.setCourseId(courseId);

        return enrollmentRepository.save(enrollment);
    }
}
