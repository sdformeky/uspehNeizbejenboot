package org.example.notificationservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.notificationservice.dto.NotificationDto;
import org.example.notificationservice.dto.NotificationRequest;
import org.example.notificationservice.service.NotificationService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
@Tag(name = "Уведомления")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    @Operation(summary = "Уведомления пользователя", description = "Новые сверху")
    public List<NotificationDto> getByUser(@Parameter(example = "1") @RequestParam Long userId) {
        return notificationService.findByUser(userId);
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Сколько непрочитанных")
    public Map<String, Long> unreadCount(@Parameter(example = "1") @RequestParam Long userId) {
        return Map.of("count", notificationService.countUnread(userId));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Отправить уведомление", description = "Вызывают другие сервисы, например enrollment-service после записи на курс")
    public NotificationDto create(@Valid @RequestBody NotificationRequest request) {
        return notificationService.create(request);
    }

    @PatchMapping("/{id}/read")
    @Operation(summary = "Отметить прочитанным")
    public NotificationDto markRead(@PathVariable Long id) {
        return notificationService.markRead(id);
    }
}
