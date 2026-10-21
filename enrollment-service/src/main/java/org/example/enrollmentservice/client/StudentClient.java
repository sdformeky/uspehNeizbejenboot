package org.example.enrollmentservice.client;

import org.example.enrollmentservice.dto.StudentDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

// Путь — как в самом student-service (без /api: этот префикс есть только у gateway)
@FeignClient(name = "student-service", fallbackFactory = StudentClientFallback.class)
public interface StudentClient {

    @GetMapping("/students/{id}")
    StudentDto getStudentById(@PathVariable("id") Long id);
}
