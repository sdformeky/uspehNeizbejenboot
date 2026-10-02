package org.example.courseservice.repository;

import org.example.courseservice.model.Course;
import org.example.courseservice.model.CourseStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CourseRepository extends JpaRepository<Course, Long> {

    List<Course> findByStatus(CourseStatus status);

    List<Course> findByCategoryId(Long categoryId);

    List<Course> findByInstructorId(Long instructorId);
}
