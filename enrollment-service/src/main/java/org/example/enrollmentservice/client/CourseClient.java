package org.example.enrollmentservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "course-service", fallback = CourseClientFallback.class)
public interface CourseClient {

    @GetMapping("/api/courses/{id}")
    Object getCourseById(@PathVariable("id") Long id);
}
