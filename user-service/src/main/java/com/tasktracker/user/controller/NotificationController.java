package com.tasktracker.user.controller;

import com.tasktracker.user.dto.NotificationResponse;
import com.tasktracker.user.service.UserNotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final UserNotificationService service;

    public NotificationController(UserNotificationService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<NotificationResponse>> list(@AuthenticationPrincipal Jwt jwt) {
        var notifications = service.listForUser(jwt.getSubject()).stream().map(this::toResponse).toList();
        return ResponseEntity.ok(notifications);
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<NotificationResponse> markRead(@PathVariable("id") UUID id,
                                                         @AuthenticationPrincipal Jwt jwt) {
        var notification = service.markRead(id, jwt.getSubject());
        return ResponseEntity.ok(toResponse(notification));
    }

    private NotificationResponse toResponse(com.tasktracker.user.entity.UserNotification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getType(),
                notification.getActionUrl(),
                notification.getActionLabel(),
                notification.getCreatedAt(),
                notification.isRead()
        );
    }
}
