package com.tasktracker.user.repository;

import com.tasktracker.user.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserProfileRepository extends JpaRepository<UserProfile, UUID> {
    Optional<UserProfile> findByUserEmail(String userEmail);
    List<UserProfile> findByUserEmailContainingIgnoreCase(String userEmail);
    List<UserProfile> findByFullNameContainingIgnoreCase(String fullName);
    List<UserProfile> findByPhoneContainingIgnoreCase(String phone);

    @Query("select p from UserProfile p where lower(p.userEmail) in :emails")
    List<UserProfile> findByUserEmailInIgnoreCase(@Param("emails") List<String> emails);
}
