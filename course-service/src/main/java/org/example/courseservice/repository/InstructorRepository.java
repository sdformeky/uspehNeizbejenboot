package org.example.courseservice.repository;

import org.example.courseservice.model.Instructor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InstructorRepository extends JpaRepository<Instructor, Long> {

    Optional<Instructor> findByUserId(Long userId);
}
