package org.example.enrollmentservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "student-service", fallback = StudentClientFallback.class) 
public interface StudentClient {

    @GetMapping("/api/students/{id}")
    Object getStudentById(@PathVariable("id") Long id);
}
