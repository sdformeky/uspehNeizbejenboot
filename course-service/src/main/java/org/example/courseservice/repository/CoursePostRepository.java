package org.example.courseservice.repository;

import org.example.courseservice.model.CoursePost;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CoursePostRepository extends JpaRepository<CoursePost, Long> {

    List<CoursePost> findByCourseIdOrderByCreatedAtDesc(Long courseId);
}
