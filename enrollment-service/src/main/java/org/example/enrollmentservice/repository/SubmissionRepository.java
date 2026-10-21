package org.example.enrollmentservice.repository;

import org.example.enrollmentservice.model.Submission;
import org.example.enrollmentservice.model.SubmissionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SubmissionRepository extends JpaRepository<Submission, Long> {

    List<Submission> findByEnrollmentId(Long enrollmentId);

    List<Submission> findByStatus(SubmissionStatus status);
}
