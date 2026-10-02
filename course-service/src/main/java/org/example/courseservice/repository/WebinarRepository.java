package org.example.courseservice.repository;

import org.example.courseservice.model.Webinar;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WebinarRepository extends JpaRepository<Webinar, Long> {

    List<Webinar> findByCourseIdOrderByStartsAtAsc(Long courseId);
}
