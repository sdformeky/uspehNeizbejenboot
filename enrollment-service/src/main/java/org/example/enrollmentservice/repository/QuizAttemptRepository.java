package org.example.enrollmentservice.repository;

import org.example.enrollmentservice.model.QuizAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, Long> {

    List<QuizAttempt> findByEnrollmentIdAndQuizId(Long enrollmentId, Long quizId);
}
