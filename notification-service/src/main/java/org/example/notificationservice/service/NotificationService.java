package org.example.notificationservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.notificationservice.dto.NotificationDto;
import org.example.notificationservice.dto.NotificationRequest;
import org.example.notificationservice.exception.ForbiddenException;
import org.example.notificationservice.exception.NotFoundException;
import org.example.notificationservice.model.Notification;
import org.example.notificationservice.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public List<NotificationDto> findByUser(Long userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(NotificationDto::from)
                .toList();
    }

    public long countUnread(Long userId) {
        return notificationRepository.countByUserIdAndReadFalse(userId);
    }

    @Transactional
    public NotificationDto create(NotificationRequest request) {
        Notification notification = new Notification();
        notification.setUserId(request.userId());
        notification.setType(request.type());
        notification.setText(request.text().trim());
        notificationRepository.save(notification);
        log.info("Уведомление {} для пользователя {}: {}", request.type(), request.userId(), notification.getText());
        return NotificationDto.from(notification);
    }

    @Transactional
    public NotificationDto markRead(Long id, Long currentUserId) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Уведомление с id " + id + " не найдено"));
        // X-User-Id ставит api-gateway по токену: отмечать можно только свои уведомления
        if (currentUserId != null && !notification.getUserId().equals(currentUserId)) {
            throw new ForbiddenException("Это чужое уведомление");
        }
        notification.setRead(true);
        return NotificationDto.from(notification);
    }
}
