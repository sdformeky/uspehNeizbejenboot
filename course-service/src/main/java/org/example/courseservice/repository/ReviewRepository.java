package org.example.courseservice.repository;

import org.example.courseservice.model.Review;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findByCourseId(Long courseId);

    List<Review> findByCourseIdOrderByCreatedAtDesc(Long courseId);

    boolean existsByCourseIdAndStudentId(Long courseId, Long studentId);

    boolean existsByCourseId(Long courseId);
}
