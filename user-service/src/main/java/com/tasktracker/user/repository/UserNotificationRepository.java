package com.tasktracker.user.repository;

import com.tasktracker.user.entity.UserNotification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface UserNotificationRepository extends JpaRepository<UserNotification, UUID> {
    List<UserNotification> findByUserEmailOrderByCreatedAtDesc(String userEmail);
}
