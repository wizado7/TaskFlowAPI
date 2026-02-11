package com.tasktracker.user.repository;

import com.tasktracker.user.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserProfileRepository extends JpaRepository<UserProfile, UUID> {
    Optional<UserProfile> findByUserEmail(String userEmail);
    List<UserProfile> findByUserEmailContainingIgnoreCase(String userEmail);
    List<UserProfile> findByFullNameContainingIgnoreCase(String fullName);
    List<UserProfile> findByPhoneContainingIgnoreCase(String phone);
}
