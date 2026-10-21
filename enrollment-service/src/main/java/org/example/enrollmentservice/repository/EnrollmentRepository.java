package org.example.enrollmentservice.repository;

import org.example.enrollmentservice.model.Enrollment;
import org.example.enrollmentservice.model.EnrollmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    List<Enrollment> findByStudentId(Long studentId);

    List<Enrollment> findByCourseId(Long courseId);

    boolean existsByStudentIdAndCourseId(Long studentId, Long courseId);

    long countByCourseIdAndStatus(Long courseId, EnrollmentStatus status);

    long countByCourseIdAndStatusNot(Long courseId, EnrollmentStatus status);
}
