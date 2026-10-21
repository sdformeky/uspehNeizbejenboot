package org.example.enrollmentservice.client;

import org.example.enrollmentservice.dto.CourseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(name = "course-service", fallbackFactory = CourseClientFallback.class)
public interface CourseClient {

    @GetMapping("/courses/{id}")
    CourseDto getCourseById(@PathVariable("id") Long id);

    @GetMapping("/courses/{id}/lessons/ids")
    List<Long> getLessonIds(@PathVariable("id") Long id);
}
