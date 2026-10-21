package org.example.courseservice.client;

import org.example.courseservice.dto.EnrollmentShortDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "enrollment-service")
public interface EnrollmentClient {

    @GetMapping("/enrollments")
    List<EnrollmentShortDto> getByStudent(@RequestParam("studentId") Long studentId);
}
