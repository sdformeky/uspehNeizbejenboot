package org.example.courseservice.repository;

import org.example.courseservice.model.Lesson;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface LessonRepository extends JpaRepository<Lesson, Long> {

    List<Lesson> findByModuleIdOrderByPositionAsc(Long moduleId);

    @Query("select l.id from Lesson l where l.module.course.id = :courseId order by l.module.position, l.position")
    List<Long> findIdsByCourseId(@Param("courseId") Long courseId);
}
