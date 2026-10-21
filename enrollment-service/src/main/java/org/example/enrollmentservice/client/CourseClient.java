package org.example.enrollmentservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "course-service", fallbackFactory = CourseClientFallback.class)
public interface CourseClient {

    @GetMapping("/courses/{id}")
    Object getCourseById(@PathVariable("id") Long id);
}
