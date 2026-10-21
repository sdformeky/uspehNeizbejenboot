package org.example.courseservice.client;

import org.example.courseservice.dto.StudentShortDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

// Вызов напрямую в student-service по имени из Eureka (не через gateway)
@FeignClient(name = "student-service")
public interface StudentClient {

    @GetMapping("/students")
    List<StudentShortDto> getAll();

    @GetMapping("/students")
    List<StudentShortDto> getByUserId(@RequestParam("userId") Long userId);
}
