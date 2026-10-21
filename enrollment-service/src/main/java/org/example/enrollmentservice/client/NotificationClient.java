package org.example.enrollmentservice.client;

import org.example.enrollmentservice.dto.NotificationRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "notification-service", fallbackFactory = NotificationClientFallback.class)
public interface NotificationClient {

    @PostMapping("/notifications")
    Object send(@RequestBody NotificationRequest request);
}
