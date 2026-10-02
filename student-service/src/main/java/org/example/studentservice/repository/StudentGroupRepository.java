package org.example.studentservice.repository;

import org.example.studentservice.model.StudentGroup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StudentGroupRepository extends JpaRepository<StudentGroup, Long> {

    Optional<StudentGroup> findByName(String name);
}
