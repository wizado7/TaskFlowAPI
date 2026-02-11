package com.tasktracker.user.controller;

import com.tasktracker.user.dto.NotificationCreateRequest;
import com.tasktracker.user.dto.NotificationResponse;
import com.tasktracker.user.service.UserNotificationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/internal/notifications")
public class InternalNotificationController {

    private final UserNotificationService service;

    public InternalNotificationController(UserNotificationService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<NotificationResponse> create(@Valid @RequestBody NotificationCreateRequest request) {
        var notification = service.create(request);
        return ResponseEntity.ok(new NotificationResponse(
                notification.getId(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getType(),
                notification.getActionUrl(),
                notification.getActionLabel(),
                notification.getCreatedAt(),
                notification.isRead()
        ));
    }
}
