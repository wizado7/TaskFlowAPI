package com.tasktracker.user.service;

import com.tasktracker.user.dto.NotificationCreateRequest;
import com.tasktracker.user.entity.UserNotification;
import com.tasktracker.user.exception.AppException;
import com.tasktracker.user.repository.UserNotificationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class UserNotificationService {

    private final UserNotificationRepository repository;

    public UserNotificationService(UserNotificationRepository repository) {
        this.repository = repository;
    }

    public UserNotification create(NotificationCreateRequest request) {
        UserNotification notification = new UserNotification();
        notification.setUserEmail(request.email());
        notification.setTitle(request.title());
        notification.setMessage(request.message());
        notification.setType(request.type());
        notification.setActionUrl(request.actionUrl());
        notification.setActionLabel(request.actionLabel());
        notification.setRead(false);
        return repository.save(notification);
    }

    public List<UserNotification> listForUser(String email) {
        return repository.findByUserEmailOrderByCreatedAtDesc(email);
    }

    public UserNotification markRead(UUID id, String email) {
        UserNotification notification = repository.findById(id)
                .orElseThrow(() -> new AppException("Notification not found", HttpStatus.NOT_FOUND));
        if (!notification.getUserEmail().equalsIgnoreCase(email)) {
            throw new AppException("Notification access denied", HttpStatus.FORBIDDEN);
        }
        notification.setRead(true);
        return repository.save(notification);
    }
}
